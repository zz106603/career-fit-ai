package com.careerfit.ai.structured.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record AiCallExecution(
        UUID id,
        UUID workflowExecutionId,
        String requestId,
        String purpose,
        String promptVersion,
        String schemaVersion,
        AiCallStatus status,
        String failureCode,
        int attemptCount,
        Instant startedAt,
        Instant completedAt) {

    public AiCallExecution {
        Objects.requireNonNull(id, "AI 호출 실행 ID는 필수입니다.");
        Objects.requireNonNull(workflowExecutionId, "Workflow 실행 ID는 필수입니다.");
        purpose = requireText(purpose, "호출 목적");
        promptVersion = requireText(promptVersion, "Prompt 버전");
        schemaVersion = requireText(schemaVersion, "Schema 버전");
        Objects.requireNonNull(status, "AI 호출 상태는 필수입니다.");
        Objects.requireNonNull(startedAt, "AI 호출 시작 시각은 필수입니다.");
        if (attemptCount < 0) throw new IllegalArgumentException("시도 횟수는 0 이상이어야 합니다.");
        if (status == AiCallStatus.PROCESSING && (completedAt != null || failureCode != null)) {
            throw new IllegalArgumentException("진행 중 호출에는 완료 정보가 없어야 합니다.");
        }
        if (status == AiCallStatus.SUCCEEDED && (completedAt == null || failureCode != null)) {
            throw new IllegalArgumentException("성공 호출의 완료 정보가 올바르지 않습니다.");
        }
        if (status == AiCallStatus.FAILED
                && (completedAt == null || failureCode == null || failureCode.isBlank())) {
            throw new IllegalArgumentException("실패 호출에는 완료 시각과 실패 코드가 필요합니다.");
        }
    }

    public static AiCallExecution start(
            UUID workflowExecutionId, String requestId, String purpose,
            String promptVersion, String schemaVersion, Instant startedAt) {
        return new AiCallExecution(UUID.randomUUID(), workflowExecutionId, requestId,
                purpose, promptVersion, schemaVersion, AiCallStatus.PROCESSING,
                null, 0, startedAt, null);
    }

    public AiCallExecution succeed(int attempts, Instant completedAt) {
        return finish(AiCallStatus.SUCCEEDED, null, attempts, completedAt);
    }

    public AiCallExecution fail(String code, int attempts, Instant completedAt) {
        return finish(AiCallStatus.FAILED, requireText(code, "실패 코드"), attempts, completedAt);
    }

    private AiCallExecution finish(
            AiCallStatus status, String code, int attempts, Instant completedAt) {
        if (this.status != AiCallStatus.PROCESSING) {
            throw new IllegalStateException("종료된 AI 호출은 다시 종료할 수 없습니다.");
        }
        return new AiCallExecution(id, workflowExecutionId, requestId, purpose,
                promptVersion, schemaVersion, status, code, attempts, startedAt, completedAt);
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + "은 필수입니다.");
        return value.trim();
    }
}
