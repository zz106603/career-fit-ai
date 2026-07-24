package com.careerfit.ai.port.model;

import java.util.List;
import java.util.Objects;

public record EmbeddingResponse(List<Double> vector, String model) {

    public EmbeddingResponse {
        Objects.requireNonNull(vector, "vector는 null일 수 없습니다.");
        vector = List.copyOf(vector);
        Objects.requireNonNull(model, "model은 null일 수 없습니다.");
    }
}
