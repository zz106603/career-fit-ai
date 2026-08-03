package com.careerfit.ai.port.model;

import java.util.Objects;

public record LlmResponse(
        String content,
        String provider,
        String model,
        String providerRequestId,
        TokenUsage tokenUsage) {

    public LlmResponse {
        Objects.requireNonNull(content, "content는 null일 수 없습니다.");
        Objects.requireNonNull(provider, "provider는 null일 수 없습니다.");
        Objects.requireNonNull(model, "model은 null일 수 없습니다.");
        tokenUsage = tokenUsage == null ? TokenUsage.unknown() : tokenUsage;
    }

    public LlmResponse(String content, String model) {
        this(content, "unknown", model, null, TokenUsage.unknown());
    }
}
