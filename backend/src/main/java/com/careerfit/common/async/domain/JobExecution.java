package com.careerfit.common.async.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** 외부 호출이나 문서 처리처럼 오래 걸리는 업무를 재시도 가능한 실행 단위로 표현한다. */
public record JobExecution(
        JobExecutionId id,
        UUID userId,
        JobType type,
        UUID targetId,
        String inputVersion,
        String duplicateKey,
        JobExecutionStatus status,
        int retryCount,
        String failureCode,
        Instant createdAt,
        Instant claimedAt,
        Instant completedAt) {

    public JobExecution {
        Objects.requireNonNull(id, "작업 실행 ID는 필수입니다.");
        Objects.requireNonNull(userId, "사용자 ID는 필수입니다.");
        Objects.requireNonNull(type, "작업 유형은 필수입니다.");
        Objects.requireNonNull(targetId, "작업 대상 ID는 필수입니다.");
        inputVersion = requireText(inputVersion, "입력 버전");
        duplicateKey = requireText(duplicateKey, "중복 키");
        Objects.requireNonNull(status, "작업 상태는 필수입니다.");
        if (retryCount < 0) {
            throw new IllegalArgumentException("재시도 횟수는 0 이상이어야 합니다.");
        }
        failureCode = normalize(failureCode);
        Objects.requireNonNull(createdAt, "생성 시각은 필수입니다.");
        validateState(status, failureCode, createdAt, claimedAt, completedAt);
    }

    public static JobExecution queued(
            UUID userId,
            JobType type,
            UUID targetId,
            String inputVersion,
            String duplicateKey,
            Instant createdAt) {
        return new JobExecution(
                JobExecutionId.newId(),
                userId,
                type,
                targetId,
                inputVersion,
                duplicateKey,
                JobExecutionStatus.QUEUED,
                0,
                null,
                createdAt,
                null,
                null);
    }

    public JobExecution start(Instant claimedAt) {
        requireStatus(JobExecutionStatus.QUEUED, JobExecutionStatus.PROCESSING);
        return copy(JobExecutionStatus.PROCESSING, null, claimedAt, null);
    }

    public JobExecution succeed(Instant completedAt) {
        requireStatus(JobExecutionStatus.PROCESSING, JobExecutionStatus.SUCCEEDED);
        return copy(JobExecutionStatus.SUCCEEDED, null, claimedAt, completedAt);
    }

    public JobExecution fail(String failureCode, Instant completedAt) {
        requireStatus(JobExecutionStatus.PROCESSING, JobExecutionStatus.FAILED);
        return copy(
                JobExecutionStatus.FAILED,
                requireText(failureCode, "실패 코드"),
                claimedAt,
                completedAt);
    }

    public JobExecution requeueStale() {
        requireStatus(JobExecutionStatus.PROCESSING, JobExecutionStatus.QUEUED);
        return copy(JobExecutionStatus.QUEUED, retryCount + 1, null, null, null);
    }

    public JobExecution failStale(String failureCode, Instant completedAt) {
        requireStatus(JobExecutionStatus.PROCESSING, JobExecutionStatus.FAILED);
        return copy(
                JobExecutionStatus.FAILED,
                retryCount,
                requireText(failureCode, "실패 코드"),
                claimedAt,
                completedAt);
    }

    private JobExecution copy(
            JobExecutionStatus newStatus,
            String newFailureCode,
            Instant newClaimedAt,
            Instant newCompletedAt) {
        return copy(newStatus, retryCount, newFailureCode, newClaimedAt, newCompletedAt);
    }

    private JobExecution copy(
            JobExecutionStatus newStatus,
            int newRetryCount,
            String newFailureCode,
            Instant newClaimedAt,
            Instant newCompletedAt) {
        return new JobExecution(
                id,
                userId,
                type,
                targetId,
                inputVersion,
                duplicateKey,
                newStatus,
                newRetryCount,
                newFailureCode,
                createdAt,
                newClaimedAt,
                newCompletedAt);
    }

    private void requireStatus(JobExecutionStatus expected, JobExecutionStatus target) {
        if (status != expected) {
            throw new InvalidJobExecutionTransitionException(status, target);
        }
    }

    private static void validateState(
            JobExecutionStatus status,
            String failureCode,
            Instant createdAt,
            Instant claimedAt,
            Instant completedAt) {
        if (claimedAt != null && claimedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("선점 시각은 생성 시각보다 빠를 수 없습니다.");
        }
        if (completedAt != null && (claimedAt == null || completedAt.isBefore(claimedAt))) {
            throw new IllegalArgumentException("완료 시각은 선점 시각보다 빠를 수 없습니다.");
        }

        switch (status) {
            case QUEUED -> require(
                    claimedAt == null && completedAt == null && failureCode == null,
                    "QUEUED 작업에는 선점·완료 시각이나 실패 코드가 없어야 합니다.");
            case PROCESSING -> require(
                    claimedAt != null && completedAt == null && failureCode == null,
                    "PROCESSING 작업에는 선점 시각만 있어야 합니다.");
            case SUCCEEDED -> require(
                    claimedAt != null && completedAt != null && failureCode == null,
                    "SUCCEEDED 작업에는 선점·완료 시각이 필요하고 실패 코드는 없어야 합니다.");
            case FAILED -> require(
                    claimedAt != null && completedAt != null && failureCode != null,
                    "FAILED 작업에는 선점·완료 시각과 실패 코드가 필요합니다.");
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }

    private static String requireText(String value, String fieldName) {
        String normalized = normalize(value);
        if (normalized == null) {
            throw new IllegalArgumentException(fieldName + "은 필수입니다.");
        }
        return normalized;
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
