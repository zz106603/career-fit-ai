package com.careerfit.ai.structured.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record AiCallAttempt(
        UUID executionId, int attemptNumber, AiCallStatus status,
        String provider, String model, String providerRequestId, String failureCode,
        Instant startedAt, Instant completedAt,
        Integer inputTokens, Integer outputTokens, Integer totalTokens,
        int promptLength, String promptChecksumSha256,
        Integer responseLength, String responseChecksumSha256) {

    public AiCallAttempt {
        Objects.requireNonNull(executionId, "AI 호출 실행 ID는 필수입니다.");
        if (attemptNumber < 1) throw new IllegalArgumentException("시도 번호는 1 이상이어야 합니다.");
        Objects.requireNonNull(status, "AI 호출 시도 상태는 필수입니다.");
        Objects.requireNonNull(startedAt, "시도 시작 시각은 필수입니다.");
        if (promptLength < 0 || promptChecksumSha256 == null
                || !promptChecksumSha256.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("Prompt 메타데이터가 올바르지 않습니다.");
        }
        if (status == AiCallStatus.PROCESSING && (completedAt != null || failureCode != null)) {
            throw new IllegalArgumentException("진행 중 시도에는 완료 정보가 없어야 합니다.");
        }
        if (status == AiCallStatus.SUCCEEDED && (completedAt == null || failureCode != null)) {
            throw new IllegalArgumentException("성공 시도의 완료 정보가 올바르지 않습니다.");
        }
        if (status == AiCallStatus.FAILED
                && (completedAt == null || failureCode == null || failureCode.isBlank())) {
            throw new IllegalArgumentException("실패 시도에는 완료 시각과 실패 코드가 필요합니다.");
        }
    }

    public static AiCallAttempt start(
            UUID executionId, int number, Instant startedAt, int promptLength, String promptChecksum) {
        return new AiCallAttempt(executionId, number, AiCallStatus.PROCESSING,
                null, null, null, null, startedAt, null,
                null, null, null, promptLength, promptChecksum, null, null);
    }

    public AiCallAttempt succeed(
            String provider, String model, String providerRequestId,
            Integer inputTokens, Integer outputTokens, Integer totalTokens,
            int responseLength, String responseChecksum, Instant completedAt) {
        return finish(AiCallStatus.SUCCEEDED, provider, model, providerRequestId, null,
                inputTokens, outputTokens, totalTokens, responseLength, responseChecksum, completedAt);
    }

    public AiCallAttempt fail(
            String provider, String model, String providerRequestId, String failureCode,
            Integer inputTokens, Integer outputTokens, Integer totalTokens,
            Integer responseLength, String responseChecksum, Instant completedAt) {
        return finish(AiCallStatus.FAILED, provider, model, providerRequestId, failureCode,
                inputTokens, outputTokens, totalTokens, responseLength, responseChecksum, completedAt);
    }

    private AiCallAttempt finish(
            AiCallStatus status, String provider, String model, String providerRequestId,
            String failureCode, Integer inputTokens, Integer outputTokens, Integer totalTokens,
            Integer responseLength, String responseChecksum, Instant completedAt) {
        return new AiCallAttempt(executionId, attemptNumber, status, provider, model,
                providerRequestId, failureCode, startedAt, Objects.requireNonNull(completedAt),
                inputTokens, outputTokens, totalTokens, promptLength, promptChecksumSha256,
                responseLength, responseChecksum);
    }
}
