package com.careerfit.ai.port.model;

import java.util.Objects;

public record LlmResponse(String content, String model) {

    public LlmResponse {
        Objects.requireNonNull(content, "content는 null일 수 없습니다.");
        Objects.requireNonNull(model, "model은 null일 수 없습니다.");
    }
}
