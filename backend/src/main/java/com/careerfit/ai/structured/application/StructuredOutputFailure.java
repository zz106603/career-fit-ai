package com.careerfit.ai.structured.application;

public enum StructuredOutputFailure {
    RESPONSE_PARSE_FAILED(true),
    RESPONSE_SCHEMA_INVALID(true),
    PROVIDER_TIMEOUT(true),
    PROVIDER_RATE_LIMITED(true),
    PROVIDER_UNAVAILABLE(true),
    PROVIDER_CONFIGURATION_ERROR(false),
    PROVIDER_POLICY_REJECTED(false),
    UNEXPECTED_AI_ERROR(false);

    private final boolean retryable;

    StructuredOutputFailure(boolean retryable) {
        this.retryable = retryable;
    }

    public boolean retryable() {
        return retryable;
    }
}
