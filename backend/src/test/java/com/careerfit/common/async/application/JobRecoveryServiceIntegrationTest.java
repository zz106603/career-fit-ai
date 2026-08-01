package com.careerfit.common.async.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.careerfit.PostgresIntegrationTest;
import com.careerfit.common.async.domain.JobExecution;
import com.careerfit.common.async.domain.JobExecutionStatus;
import com.careerfit.common.async.domain.JobType;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.sql.Timestamp;
import java.util.UUID;
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
@DisplayName("정체 작업 복구 서비스 통합 테스트")
class JobRecoveryServiceIntegrationTest extends PostgresIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-07-28T01:00:00Z");

    @Autowired
    private JobExecutionService executionService;

    @Autowired
    private JobRecoveryService recoveryService;

    @Autowired
    private JdbcClient jdbcClient;

    @BeforeEach
    void 데이터베이스를_초기화한다() {
        jdbcClient.sql("TRUNCATE job_execution CASCADE").update();
    }

    @Test
    @DisplayName("DB에 남아 있는 QUEUED 작업은 Dispatcher 조회 시 다시 발견된다")
    void 데이터베이스에_남은_대기_작업은_다시_발견된다() {
        UUID userId = UUID.randomUUID();
        JobExecution queued = executionService.create(
                userId,
                JobType.CAREER_DOCUMENT_EXTRACTION,
                UUID.randomUUID(),
                "document-v1",
                "restart:queued");

        assertThat(executionService.findQueued(10))
                .extracting(JobExecution::id)
                .containsExactly(queued.id());
    }

    @Test
    @DisplayName("stale PROCESSING 작업은 재시도 상한 내에서 QUEUED로 복귀한다")
    void 정체된_처리_중_작업은_재시도_상한_내에서_대기로_복귀한다() {
        JobExecution processing = createProcessing("stale:retry");
        makeStale(processing, 0);

        StaleRecoveryResult result =
                recoveryService.recoverStale(Duration.ofMinutes(10), 2, 10);
        JobExecution recovered =
                executionService.find(processing.userId(), processing.id());

        assertThat(result.requeuedCount()).isEqualTo(1);
        assertThat(recovered.status()).isEqualTo(JobExecutionStatus.QUEUED);
        assertThat(recovered.retryCount()).isEqualTo(1);
        assertThat(recovered.claimedAt()).isNull();
    }

    @Test
    @DisplayName("재시도 상한에 도달한 stale 작업은 최종 FAILED 처리한다")
    void 재시도_상한에_도달한_정체_작업은_최종_실패한다() {
        JobExecution processing = createProcessing("stale:exhausted");
        makeStale(processing, 2);

        StaleRecoveryResult result =
                recoveryService.recoverStale(Duration.ofMinutes(10), 2, 10);
        JobExecution recovered =
                executionService.find(processing.userId(), processing.id());

        assertThat(result.failedCount()).isEqualTo(1);
        assertThat(recovered.status()).isEqualTo(JobExecutionStatus.FAILED);
        assertThat(recovered.failureCode())
                .isEqualTo(JobRecoveryService.STALE_RETRY_EXHAUSTED);
        assertThat(recovered.retryCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("stale 기준보다 최근에 선점된 PROCESSING 작업은 복구하지 않는다")
    void 최근에_선점된_처리_중_작업은_복구하지_않는다() {
        JobExecution processing = createProcessing("stale:recent");

        StaleRecoveryResult result =
                recoveryService.recoverStale(Duration.ofMinutes(10), 2, 10);

        assertThat(result.processedCount()).isZero();
        assertThat(executionService.find(processing.userId(), processing.id()).status())
                .isEqualTo(JobExecutionStatus.PROCESSING);
    }

    @Test
    @DisplayName("수동 전체 재실행은 FAILED 실행을 보존하고 새 QUEUED 실행을 만든다")
    void 수동_전체_재실행은_실패_이력을_보존하고_새_대기_실행을_만든다() {
        JobExecution processing = createProcessing("manual:rerun");
        JobExecution failed =
                executionService.fail(processing.userId(), processing.id(), "PROVIDER_TIMEOUT");

        JobExecution rerun =
                recoveryService.rerunFailed(failed.userId(), failed.id());

        assertThat(rerun.id()).isNotEqualTo(failed.id());
        assertThat(rerun.status()).isEqualTo(JobExecutionStatus.QUEUED);
        assertThat(rerun.retryCount()).isZero();
        assertThat(executionService.find(failed.userId(), failed.id()).status())
                .isEqualTo(JobExecutionStatus.FAILED);
    }

    @Test
    @DisplayName("다른 사용자의 작업은 조회할 수 없다")
    void 다른_사용자의_작업은_조회할_수_없다() {
        UUID userA = UUID.randomUUID();
        UUID userB = UUID.randomUUID();
        JobExecution userAExecution = executionService.create(
                userA,
                JobType.CAREER_DOCUMENT_EXTRACTION,
                UUID.randomUUID(),
                "input-v1",
                "ownership:user-a");

        assertThatThrownBy(() -> executionService.find(userB, userAExecution.id()))
                .isInstanceOf(JobExecutionNotFoundException.class);
    }

    @Test
    @DisplayName("다른 사용자의 실패 작업은 재실행할 수 없다")
    void 다른_사용자의_실패_작업은_재실행할_수_없다() {
        UUID userA = UUID.randomUUID();
        UUID userB = UUID.randomUUID();
        JobExecution queued = executionService.create(
                userA,
                JobType.CAREER_DOCUMENT_EXTRACTION,
                UUID.randomUUID(),
                "input-v1",
                "ownership:rerun");
        executionService.start(queued.userId(), queued.id());
        JobExecution failed = executionService.fail(queued.userId(), queued.id(), "FAILED");

        assertThatThrownBy(() -> recoveryService.rerunFailed(userB, failed.id()))
                .isInstanceOf(JobExecutionNotFoundException.class);

        assertThat(executionService.find(userA, failed.id()).status())
                .isEqualTo(failed.status());
    }

    private JobExecution createProcessing(String duplicateKey) {
        UUID userId = UUID.randomUUID();
        JobExecution queued = executionService.create(
                userId,
                JobType.CAREER_DOCUMENT_EXTRACTION,
                UUID.randomUUID(),
                "document-v1",
                duplicateKey);
        return executionService.start(userId, queued.id());
    }

    private void makeStale(JobExecution execution, int retryCount) {
        Instant staleAt = NOW.minus(Duration.ofMinutes(20));
        jdbcClient
                .sql("""
                        UPDATE job_execution
                           SET created_at = :staleAt,
                               claimed_at = :staleAt,
                               retry_count = :retryCount
                         WHERE job_execution_id = :executionId
                        """)
                .param("staleAt", Timestamp.from(staleAt))
                .param("retryCount", retryCount)
                .param("executionId", execution.id().value())
                .update();
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
