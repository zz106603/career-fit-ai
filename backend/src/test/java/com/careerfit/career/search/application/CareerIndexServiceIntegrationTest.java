package com.careerfit.career.search.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.careerfit.PostgresIntegrationTest;
import com.careerfit.ai.port.EmbeddingProviderPort;
import com.careerfit.ai.port.model.EmbeddingRequest;
import com.careerfit.ai.port.model.EmbeddingResponse;
import com.careerfit.career.application.DirectCareerService;
import com.careerfit.career.domain.CareerExperienceVersion;
import com.careerfit.career.domain.DirectCareerContent;
import com.careerfit.career.search.domain.CareerSearchDocument;
import com.careerfit.career.search.domain.CareerSearchIndexStatus;
import com.careerfit.identity.RequestCurrentUserContext;
import com.careerfit.identity.RequestCurrentUserContext.UserScope;
import com.careerfit.identity.development.DevelopmentUsers;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
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
    private EmbeddingProviderPort embeddingProviderPort;

    @Autowired
    private RequestCurrentUserContext currentUserContext;

    @Autowired
    private JdbcClient jdbcClient;

    @BeforeEach
    void 데이터베이스를_초기화한다() {
        jdbcClient.sql("TRUNCATE career_experience CASCADE").update();
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
            assertThat(searchDocumentCount()).isZero();
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
