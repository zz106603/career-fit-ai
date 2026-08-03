package com.careerfit.ai.structured.application;

import java.util.UUID;

public class StructuredOutputException extends RuntimeException {

    private final UUID aiCallExecutionId;
    private final StructuredOutputFailure failure;
    private final int attemptCount;

    public StructuredOutputException(
            UUID aiCallExecutionId, StructuredOutputFailure failure, int attemptCount) {
        super("Structured Output 실행에 실패했습니다: " + failure);
        this.aiCallExecutionId = aiCallExecutionId;
        this.failure = failure;
        this.attemptCount = attemptCount;
    }

    public UUID aiCallExecutionId() { return aiCallExecutionId; }
    public StructuredOutputFailure failure() { return failure; }
    public int attemptCount() { return attemptCount; }
}
