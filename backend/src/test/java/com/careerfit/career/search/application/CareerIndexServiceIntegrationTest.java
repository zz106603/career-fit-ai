package com.careerfit.career.search.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.careerfit.PostgresIntegrationTest;
import com.careerfit.ai.port.EmbeddingProviderPort;
import com.careerfit.ai.port.model.EmbeddingRequest;
import com.careerfit.ai.port.model.EmbeddingResponse;
import com.careerfit.ai.adapter.fake.FakeEmbeddingProviderAdapter;
import com.careerfit.ai.adapter.fake.FakeProviderBehavior;
import com.careerfit.career.application.DirectCareerService;
import com.careerfit.career.domain.CareerExperienceVersion;
import com.careerfit.career.domain.DirectCareerContent;
import com.careerfit.career.search.domain.CareerSearchDocument;
import com.careerfit.career.search.domain.CareerSearchIndexStatus;
import com.careerfit.common.async.application.JobDispatcher;
import com.careerfit.common.async.application.JobExecutionService;
import com.careerfit.common.async.application.JobHandlerRegistry;
import com.careerfit.common.async.application.JobWorker;
import com.careerfit.common.async.domain.JobExecution;
import com.careerfit.common.async.domain.JobExecutionStatus;
import com.careerfit.identity.RequestCurrentUserContext;
import com.careerfit.identity.RequestCurrentUserContext.UserScope;
import com.careerfit.identity.development.DevelopmentUsers;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ActiveProfiles("test")
@SpringBootTest
@DisplayName("경력 Fake 색인 서비스 통합 테스트")
class CareerIndexServiceIntegrationTest extends PostgresIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-07-27T00:00:00Z");

    @Autowired
    private DirectCareerService directCareerService;

    @Autowired
    private CareerIndexService indexService;

    @Autowired
    private CareerSearchDocumentRepository searchDocumentRepository;

    @Autowired
    private CareerSearchTextBuilder textBuilder;

    @Autowired
    private CareerIndexJobService indexJobs;

    @Autowired
    private CareerIndexingPersistence indexingPersistence;

    @Autowired
    private JobExecutionService executionService;

    @Autowired
    private JobWorker worker;

    @Autowired
    private EmbeddingProviderPort embeddingProviderPort;

    @Autowired
    private RequestCurrentUserContext currentUserContext;

    @Autowired
    private JdbcClient jdbcClient;

    @BeforeEach
    void 데이터베이스를_초기화한다() {
        jdbcClient.sql("TRUNCATE job_execution, career_experience CASCADE").update();
    }

    @Test
    @DisplayName("미확정 경력 버전은 색인하지 않는다")
    void 미확정_경력_버전은_색인하지_않는다() {
        try (UserScope ignored = currentUserContext.bind(DevelopmentUsers.USER_A)) {
            CareerExperienceVersion draft =
                    directCareerService.create(content("미확정 경력"));

            assertThatThrownBy(() -> indexService.index(draft.id()))
                    .isInstanceOf(CareerVersionNotIndexableException.class);
            assertThat(searchDocumentCount()).isZero();
        }
    }

    @Test
    @DisplayName("확정 경력을 Fake embedding으로 동기 색인한다")
    void 확정_경력을_Fake_embedding으로_동기_색인한다() {
        try (UserScope ignored = currentUserContext.bind(DevelopmentUsers.USER_A)) {
            CareerExperienceVersion confirmed = createConfirmed("동기 색인 경력");

            CareerSearchDocument document = indexService.index(confirmed.id());

            assertThat(document.status()).isEqualTo(CareerSearchIndexStatus.INDEXED);
            assertThat(document.embedding()).hasSize(8);
            assertThat(document.embeddingVersion()).isEqualTo("fake-embedding-v1");
            assertThat(document.indexedAt()).isEqualTo(NOW);
            assertThat(document.searchableText())
                    .contains("경험명: 동기 색인 경력", "역할: 백엔드 개발");
            assertThat(storedVector(confirmed.id().value())).startsWith("[");
        }
    }

    @Test
    @DisplayName("검색 문서는 PENDING에서 INDEXED로 전이한다")
    void 검색_문서는_PENDING에서_INDEXED로_전이한다() {
        try (UserScope ignored = currentUserContext.bind(DevelopmentUsers.USER_A)) {
            CareerExperienceVersion confirmed = createConfirmed("상태 전이 경력");
            String searchableText = textBuilder.build(confirmed);
            CareerSearchDocument pending = CareerSearchDocument.pending(
                    DevelopmentUsers.USER_A.userId(),
                    confirmed.id(),
                    searchableText,
                    "a".repeat(64),
                    NOW);
            searchDocumentRepository.savePending(pending);

            CareerSearchDocument savedPending = searchDocumentRepository
                    .findByExperienceVersion(DevelopmentUsers.USER_A.userId(), confirmed.id())
                    .orElseThrow();
            assertThat(savedPending.status()).isEqualTo(CareerSearchIndexStatus.PENDING);

            EmbeddingResponse response =
                    embeddingProviderPort.embed(new EmbeddingRequest(searchableText));
            boolean updated = searchDocumentRepository.markIndexed(
                    DevelopmentUsers.USER_A.userId(),
                    confirmed.id(),
                    response.vector(),
                    response.model(),
                    NOW);
            assertThat(updated).isFalse();

            assertThat(searchDocumentRepository.markIndexing(
                    DevelopmentUsers.USER_A.userId(),
                    confirmed.id(),
                    NOW))
                    .isTrue();
            updated = searchDocumentRepository.markIndexed(
                    DevelopmentUsers.USER_A.userId(), confirmed.id(), response.vector(), response.model(), NOW);

            assertThat(updated).isTrue();
            assertThat(searchDocumentRepository
                            .findByExperienceVersion(
                                    DevelopmentUsers.USER_A.userId(), confirmed.id())
                            .orElseThrow()
                            .status())
                    .isEqualTo(CareerSearchIndexStatus.INDEXED);
        }
    }

    @Test
    @DisplayName("같은 경력 버전을 다시 색인해도 문서와 vector를 중복 생성하지 않는다")
    void 같은_경력_버전을_다시_색인해도_문서와_vector를_중복_생성하지_않는다() {
        try (UserScope ignored = currentUserContext.bind(DevelopmentUsers.USER_A)) {
            CareerExperienceVersion confirmed = createConfirmed("멱등 색인 경력");

            CareerSearchDocument first = indexService.index(confirmed.id());
            CareerSearchDocument second = indexService.index(confirmed.id());

            assertThat(second.id()).isEqualTo(first.id());
            assertThat(second.embedding()).isEqualTo(first.embedding());
            assertThat(searchDocumentCount()).isEqualTo(1);
        }
    }

    @Test
    @DisplayName("다른 사용자의 확정 경력은 색인할 수 없다")
    void 다른_사용자의_확정_경력은_색인할_수_없다() {
        CareerExperienceVersion userAVersion;
        try (UserScope ignored = currentUserContext.bind(DevelopmentUsers.USER_A)) {
            userAVersion = createConfirmed("사용자 A 경력");
        }

        try (UserScope ignored = currentUserContext.bind(DevelopmentUsers.USER_B)) {
            assertThatThrownBy(() -> indexService.index(userAVersion.id()))
                    .isInstanceOf(CareerVersionNotIndexableException.class);
            assertThat(searchDocumentRepository.findByExperienceVersion(
                            DevelopmentUsers.USER_B.userId(), userAVersion.id()))
                    .isEmpty();
            assertThat(searchDocumentCount()).isEqualTo(1);
        }
    }

    @Test
    @DisplayName("확정 시 만든 QUEUED 작업은 새 Dispatcher에서도 재발견되어 색인된다")
    void 확정_시_만든_QUEUED_작업은_새_Dispatcher에서도_재발견되어_색인된다() {
        try (UserScope ignored = currentUserContext.bind(DevelopmentUsers.USER_A)) {
            CareerExperienceVersion confirmed = createConfirmed("재시작 재발견 경력");
            JobExecution queued = executionService.findQueued(10).getFirst();

            JobDispatcher restartedDispatcher = new JobDispatcher(executionService, worker, false, 10);

            assertThat(restartedDispatcher.dispatchBatch()).isEqualTo(1);
            assertThat(executionService.find(queued.userId(), queued.id()).status())
                    .isEqualTo(JobExecutionStatus.SUCCEEDED);
            assertThat(searchDocumentRepository
                            .findByExperienceVersion(DevelopmentUsers.USER_A.userId(), confirmed.id())
                            .orElseThrow()
                            .status())
                    .isEqualTo(CareerSearchIndexStatus.INDEXED);
        }
    }

    @Test
    @DisplayName("대기 중 이전 확정 버전은 새 버전 확정 뒤에도 저장된 Snapshot으로 색인한다")
    void 대기_중_이전_확정_버전은_새_버전_확정_뒤에도_저장된_Snapshot으로_색인한다() {
        try (UserScope ignored = currentUserContext.bind(DevelopmentUsers.USER_A)) {
            CareerExperienceVersion first = createConfirmed("첫 번째 확정 버전");
            CareerExperienceVersion second = directCareerService.revise(
                    first.experienceId(), content("두 번째 확정 버전"));
            directCareerService.confirm(second.experienceId(), second.id());

            JobDispatcher restartedDispatcher = new JobDispatcher(executionService, worker, false, 10);

            assertThat(restartedDispatcher.dispatchBatch()).isEqualTo(2);
            assertThat(searchDocumentRepository
                            .findByExperienceVersion(DevelopmentUsers.USER_A.userId(), first.id())
                            .orElseThrow()
                            .status())
                    .isEqualTo(CareerSearchIndexStatus.INDEXED);
            assertThat(searchDocumentRepository
                            .findByExperienceVersion(DevelopmentUsers.USER_A.userId(), second.id())
                            .orElseThrow()
                            .status())
                    .isEqualTo(CareerSearchIndexStatus.INDEXED);
        }
    }

    @Test
    @DisplayName("Handler는 입력 Snapshot 트랜잭션을 끝낸 뒤 Provider를 호출한다")
    void Handler는_입력_Snapshot_트랜잭션을_끝낸_뒤_Provider를_호출한다() {
        try (UserScope ignored = currentUserContext.bind(DevelopmentUsers.USER_A)) {
            createConfirmed("트랜잭션 경계 경력");
            JobExecution queued = executionService.findQueued(10).getFirst();
            AtomicBoolean calledOutsideTransaction = new AtomicBoolean();
            CareerIndexingProcessor observingProcessor = new CareerIndexingProcessor(
                    indexingPersistence,
                    request -> {
                        calledOutsideTransaction.set(
                                !TransactionSynchronizationManager.isActualTransactionActive());
                        return embeddingProviderPort.embed(request);
                    });
            JobWorker observingWorker = new JobWorker(
                    executionService,
                    new JobHandlerRegistry(List.of(new CareerIndexingJobHandler(observingProcessor))));

            assertThat(observingWorker.execute(queued)).isTrue();

            assertThat(calledOutsideTransaction).isTrue();
            assertThat(executionService.find(queued.userId(), queued.id()).status())
                    .isEqualTo(JobExecutionStatus.SUCCEEDED);
        }
    }

    @Test
    @DisplayName("같은 사용자와 경력 버전의 활성 색인 작업은 하나만 생성한다")
    void 같은_사용자와_경력_버전의_활성_색인_작업은_하나만_생성한다() {
        try (UserScope ignored = currentUserContext.bind(DevelopmentUsers.USER_A)) {
            CareerExperienceVersion confirmed = createConfirmed("중복 차단 경력");
            JobExecution first = executionService.findQueued(10).getFirst();

            JobExecution duplicate = indexJobs.enqueue(
                    DevelopmentUsers.USER_A.userId(), confirmed.id());

            assertThat(duplicate.id()).isEqualTo(first.id());
            assertThat(activeIndexJobCount(confirmed.id().value())).isEqualTo(1);
        }
    }

    @Test
    @DisplayName("미확정 버전은 색인 작업도 생성하지 않는다")
    void 미확정_버전은_색인_작업도_생성하지_않는다() {
        try (UserScope ignored = currentUserContext.bind(DevelopmentUsers.USER_A)) {
            CareerExperienceVersion draft = directCareerService.create(content("미확정 작업"));

            assertThatThrownBy(() -> indexJobs.enqueue(
                            DevelopmentUsers.USER_A.userId(), draft.id()))
                    .isInstanceOf(CareerVersionNotIndexableException.class);
            assertThat(executionService.findQueued(10)).isEmpty();
        }
    }

    @Test
    @DisplayName("Worker 색인 실패는 원본 확정 경력을 유지하고 두 상태를 FAILED로 기록한다")
    void Worker_색인_실패는_원본_확정_경력을_유지하고_두_상태를_FAILED로_기록한다() {
        try (UserScope ignored = currentUserContext.bind(DevelopmentUsers.USER_A)) {
            CareerExperienceVersion confirmed = createConfirmed("실패 보존 경력");
            JobExecution queued = executionService.findQueued(10).getFirst();
            CareerIndexingProcessor failingProcessor = new CareerIndexingProcessor(
                    indexingPersistence,
                    new FakeEmbeddingProviderAdapter(FakeProviderBehavior.TIMEOUT));
            JobWorker failingWorker = new JobWorker(
                    executionService,
                    new JobHandlerRegistry(List.of(new CareerIndexingJobHandler(failingProcessor))));

            assertThat(failingWorker.execute(queued)).isTrue();

            assertThat(executionService.find(queued.userId(), queued.id()).status())
                    .isEqualTo(JobExecutionStatus.FAILED);
            CareerSearchDocument failed = searchDocumentRepository
                    .findByExperienceVersion(DevelopmentUsers.USER_A.userId(), confirmed.id())
                    .orElseThrow();
            assertThat(failed.status()).isEqualTo(CareerSearchIndexStatus.FAILED);
            assertThat(failed.failureCode()).isEqualTo("CAREER_INDEXING_FAILED");
            assertThat(directCareerService.findConfirmed())
                    .singleElement()
                    .extracting(version -> version.content().title())
                    .isEqualTo("실패 보존 경력");
        }
    }

    private CareerExperienceVersion createConfirmed(String title) {
        CareerExperienceVersion draft = directCareerService.create(content(title));
        directCareerService.confirm(draft.experienceId(), draft.id());
        return directCareerService.findConfirmed().getFirst();
    }

    private DirectCareerContent content(String title) {
        return new DirectCareerContent(title, "커리어핏", "백엔드 개발", "API를 개선했다.");
    }

    private int searchDocumentCount() {
        return jdbcClient
                .sql("SELECT COUNT(*) FROM career_search_document")
                .query(Integer.class)
                .single();
    }

    private int activeIndexJobCount(java.util.UUID versionId) {
        return jdbcClient
                .sql("""
                        SELECT COUNT(*)
                        FROM job_execution
                        WHERE job_type = 'CAREER_INDEXING'
                          AND target_id = :versionId
                          AND status IN ('QUEUED', 'PROCESSING')
                        """)
                .param("versionId", versionId)
                .query(Integer.class)
                .single();
    }

    private String storedVector(java.util.UUID versionId) {
        return jdbcClient
                .sql("""
                        SELECT embedding::text
                        FROM career_search_document
                        WHERE experience_version_id = :versionId
                        """)
                .param("versionId", versionId)
                .query(String.class)
                .single();
    }

    @TestConfiguration
    static class FixedClockConfiguration {

        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }
}
