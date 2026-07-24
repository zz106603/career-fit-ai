package com.careerfit.ai.port.model;

import java.util.Objects;

public record SearchRequest(String query) {

    public SearchRequest {
        Objects.requireNonNull(query, "query는 null일 수 없습니다.");
    }
}
