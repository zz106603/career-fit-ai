package com.careerfit.common.async.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("DB 작업 실행 도메인 테스트")
class JobExecutionTest {

    private static final Instant CREATED_AT = Instant.parse("2026-07-28T00:00:00Z");
    private static final Instant CLAIMED_AT = Instant.parse("2026-07-28T00:01:00Z");
    private static final Instant COMPLETED_AT = Instant.parse("2026-07-28T00:02:00Z");

    @Test
    @DisplayName("QUEUED 작업은 PROCESSING을 거쳐 SUCCEEDED로 전환한다")
    void 대기_작업은_처리_중을_거쳐_성공으로_전환한다() {
        JobExecution queued = queued();

        JobExecution processing = queued.start(CLAIMED_AT);
        JobExecution succeeded = processing.succeed(COMPLETED_AT);

        assertThat(processing.status()).isEqualTo(JobExecutionStatus.PROCESSING);
        assertThat(processing.claimedAt()).isEqualTo(CLAIMED_AT);
        assertThat(succeeded.status()).isEqualTo(JobExecutionStatus.SUCCEEDED);
        assertThat(succeeded.completedAt()).isEqualTo(COMPLETED_AT);
        assertThat(succeeded.failureCode()).isNull();
    }

    @Test
    @DisplayName("PROCESSING 작업이 실패하면 실패 코드와 완료 시각을 보존한다")
    void 처리_중_작업이_실패하면_실패_코드와_완료_시각을_보존한다() {
        JobExecution processing = queued().start(CLAIMED_AT);

        JobExecution failed = processing.fail("PROVIDER_TIMEOUT", COMPLETED_AT);

        assertThat(failed.status()).isEqualTo(JobExecutionStatus.FAILED);
        assertThat(failed.failureCode()).isEqualTo("PROVIDER_TIMEOUT");
        assertThat(failed.createdAt()).isEqualTo(CREATED_AT);
        assertThat(failed.claimedAt()).isEqualTo(CLAIMED_AT);
        assertThat(failed.completedAt()).isEqualTo(COMPLETED_AT);
    }

    @Test
    @DisplayName("QUEUED에서 SUCCEEDED로 직접 전환할 수 없다")
    void 대기에서_성공으로_직접_전환할_수_없다() {
        assertThatThrownBy(() -> queued().succeed(COMPLETED_AT))
                .isInstanceOf(InvalidJobExecutionTransitionException.class)
                .hasMessageContaining("QUEUED -> SUCCEEDED");
    }

    @Test
    @DisplayName("종료된 작업은 다른 상태로 전환할 수 없다")
    void 종료된_작업은_다른_상태로_전환할_수_없다() {
        JobExecution succeeded = queued().start(CLAIMED_AT).succeed(COMPLETED_AT);

        assertThatThrownBy(() -> succeeded.fail("LATE_FAILURE", COMPLETED_AT))
                .isInstanceOf(InvalidJobExecutionTransitionException.class);
        assertThatThrownBy(() -> succeeded.start(COMPLETED_AT))
                .isInstanceOf(InvalidJobExecutionTransitionException.class);
    }

    @Test
    @DisplayName("FAILED 전환에는 실패 코드가 필요하다")
    void 실패_전환에는_실패_코드가_필요하다() {
        JobExecution processing = queued().start(CLAIMED_AT);

        assertThatThrownBy(() -> processing.fail(" ", COMPLETED_AT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("실패 코드");
    }

    @Test
    @DisplayName("선점 시각은 생성 시각보다 빠를 수 없다")
    void 선점_시각은_생성_시각보다_빠를_수_없다() {
        assertThatThrownBy(() -> queued().start(CREATED_AT.minusSeconds(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("선점 시각");
    }

    private JobExecution queued() {
        return JobExecution.queued(
                UUID.randomUUID(),
                JobType.CAREER_DOCUMENT_EXTRACTION,
                UUID.randomUUID(),
                "document-v1",
                "career-document:input-v1",
                CREATED_AT);
    }
}
