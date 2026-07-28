package com.careerfit.common.async.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.careerfit.PostgresIntegrationTest;
import com.careerfit.common.async.domain.InvalidJobExecutionTransitionException;
import com.careerfit.common.async.domain.JobExecution;
import com.careerfit.common.async.domain.JobExecutionStatus;
import com.careerfit.common.async.domain.JobType;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
@DisplayName("DB 작업 실행 서비스 통합 테스트")
class JobExecutionServiceIntegrationTest extends PostgresIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-07-28T00:00:00Z");

    @Autowired
    private JobExecutionService service;

    @Autowired
    private JdbcClient jdbcClient;

    @BeforeEach
    void 데이터베이스를_초기화한다() {
        jdbcClient.sql("TRUNCATE job_execution").update();
    }

    @Test
    @DisplayName("작업 유형과 입력 정보와 생성 시각을 저장한다")
    void 작업_유형과_입력_정보와_생성_시각을_저장한다() {
        UUID userId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();

        JobExecution created = service.create(
                userId,
                JobType.CAREER_DOCUMENT_EXTRACTION,
                targetId,
                "document-v3",
                "document:" + targetId + ":v3");
        JobExecution found = service.find(userId, created.id());

        assertThat(found.type()).isEqualTo(JobType.CAREER_DOCUMENT_EXTRACTION);
        assertThat(found.targetId()).isEqualTo(targetId);
        assertThat(found.inputVersion()).isEqualTo("document-v3");
        assertThat(found.status()).isEqualTo(JobExecutionStatus.QUEUED);
        assertThat(found.createdAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("동일 사용자의 동일 중복 키 진행 작업은 하나만 생성한다")
    void 동일_사용자의_동일_중복_키_진행_작업은_하나만_생성한다() {
        UUID userId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        String duplicateKey = "document:" + targetId + ":v1";

        JobExecution first = create(userId, targetId, duplicateKey);
        JobExecution second = create(userId, targetId, duplicateKey);

        assertThat(second.id()).isEqualTo(first.id());
        assertThat(countByDuplicateKey(userId, duplicateKey)).isEqualTo(1);
    }

    @Test
    @DisplayName("동시에 요청해도 동일 중복 키의 진행 작업은 하나만 생성한다")
    void 동시에_요청해도_동일_중복_키의_진행_작업은_하나만_생성한다()
            throws Exception {
        UUID userId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        String duplicateKey = "document:" + targetId + ":concurrent-v1";
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<JobExecution> first = executor.submit(
                    () -> createAfterSignal(userId, targetId, duplicateKey, ready, start));
            Future<JobExecution> second = executor.submit(
                    () -> createAfterSignal(userId, targetId, duplicateKey, ready, start));

            ready.await();
            start.countDown();

            assertThat(first.get().id()).isEqualTo(second.get().id());
        }
        assertThat(countByDuplicateKey(userId, duplicateKey)).isEqualTo(1);
    }

    @Test
    @DisplayName("PROCESSING 상태에서도 동일 중복 키의 새 작업을 생성하지 않는다")
    void 처리_중에도_동일_중복_키의_새_작업을_생성하지_않는다() {
        UUID userId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        String duplicateKey = "document:" + targetId + ":v1";
        JobExecution first = create(userId, targetId, duplicateKey);
        service.start(userId, first.id());

        JobExecution duplicate = create(userId, targetId, duplicateKey);

        assertThat(duplicate.id()).isEqualTo(first.id());
        assertThat(duplicate.status()).isEqualTo(JobExecutionStatus.PROCESSING);
    }

    @Test
    @DisplayName("종료된 작업과 같은 중복 키로 새 실행을 만들 수 있다")
    void 종료된_작업과_같은_중복_키로_새_실행을_만들_수_있다() {
        UUID userId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        String duplicateKey = "document:" + targetId + ":v1";
        JobExecution first = create(userId, targetId, duplicateKey);
        service.succeed(userId, service.start(userId, first.id()).id());

        JobExecution next = create(userId, targetId, duplicateKey);

        assertThat(next.id()).isNotEqualTo(first.id());
        assertThat(countByDuplicateKey(userId, duplicateKey)).isEqualTo(2);
    }

    @Test
    @DisplayName("다른 사용자는 같은 중복 키로 별도 작업을 만들 수 있다")
    void 다른_사용자는_같은_중복_키로_별도_작업을_만들_수_있다() {
        UUID targetId = UUID.randomUUID();
        String duplicateKey = "document:" + targetId + ":v1";

        JobExecution userA = create(UUID.randomUUID(), targetId, duplicateKey);
        JobExecution userB = create(UUID.randomUUID(), targetId, duplicateKey);

        assertThat(userA.id()).isNotEqualTo(userB.id());
    }

    @Test
    @DisplayName("실패 상태와 실패 코드와 실행 시각을 저장한다")
    void 실패_상태와_실패_코드와_실행_시각을_저장한다() {
        UUID userId = UUID.randomUUID();
        JobExecution queued =
                create(userId, UUID.randomUUID(), "document:failure:v1");
        service.start(userId, queued.id());

        JobExecution failed = service.fail(userId, queued.id(), "PDF_PARSE_FAILED");
        JobExecution found = service.find(userId, failed.id());

        assertThat(found.status()).isEqualTo(JobExecutionStatus.FAILED);
        assertThat(found.failureCode()).isEqualTo("PDF_PARSE_FAILED");
        assertThat(found.createdAt()).isEqualTo(NOW);
        assertThat(found.claimedAt()).isEqualTo(NOW);
        assertThat(found.completedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("허용되지 않은 전이는 거절하고 기존 상태를 유지한다")
    void 허용되지_않은_전이는_거절하고_기존_상태를_유지한다() {
        UUID userId = UUID.randomUUID();
        JobExecution queued =
                create(userId, UUID.randomUUID(), "document:invalid-transition:v1");

        assertThatThrownBy(() -> service.succeed(userId, queued.id()))
                .isInstanceOf(InvalidJobExecutionTransitionException.class);
        assertThat(service.find(userId, queued.id()).status())
                .isEqualTo(JobExecutionStatus.QUEUED);
    }

    @Test
    @DisplayName("다른 사용자의 작업은 조회하거나 전이할 수 없다")
    void 다른_사용자의_작업은_조회하거나_전이할_수_없다() {
        UUID ownerId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        JobExecution queued =
                create(ownerId, UUID.randomUUID(), "document:ownership:v1");

        assertThatThrownBy(() -> service.find(otherUserId, queued.id()))
                .isInstanceOf(JobExecutionNotFoundException.class);
        assertThatThrownBy(() -> service.start(otherUserId, queued.id()))
                .isInstanceOf(JobExecutionNotFoundException.class);
    }

    private JobExecution create(UUID userId, UUID targetId, String duplicateKey) {
        return service.create(
                userId,
                JobType.CAREER_DOCUMENT_EXTRACTION,
                targetId,
                "document-v1",
                duplicateKey);
    }

    private JobExecution createAfterSignal(
            UUID userId,
            UUID targetId,
            String duplicateKey,
            CountDownLatch ready,
            CountDownLatch start)
            throws InterruptedException {
        ready.countDown();
        start.await();
        return create(userId, targetId, duplicateKey);
    }

    private int countByDuplicateKey(UUID userId, String duplicateKey) {
        return jdbcClient
                .sql("""
                        SELECT COUNT(*)
                        FROM job_execution
                        WHERE user_id = :userId
                          AND duplicate_key = :duplicateKey
                        """)
                .param("userId", userId)
                .param("duplicateKey", duplicateKey)
                .query(Integer.class)
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
