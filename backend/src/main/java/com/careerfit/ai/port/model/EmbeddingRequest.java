package com.careerfit.ai.port.model;

import java.util.Objects;

public record EmbeddingRequest(String text) {

    public EmbeddingRequest {
        Objects.requireNonNull(text, "text는 null일 수 없습니다.");
    }
}
