package com.careerfit.ai.structured.infrastructure;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

class AiCallAttemptId implements Serializable {
    private UUID executionId;
    private int attemptNumber;

    protected AiCallAttemptId() {}

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof AiCallAttemptId that)) return false;
        return attemptNumber == that.attemptNumber && Objects.equals(executionId, that.executionId);
    }

    @Override
    public int hashCode() { return Objects.hash(executionId, attemptNumber); }
}
