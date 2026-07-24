package com.careerfit.ai.port.model;

import java.util.List;
import java.util.Objects;

public record SearchResponse(List<SearchResult> results) {

    public SearchResponse {
        Objects.requireNonNull(results, "results는 null일 수 없습니다.");
        results = List.copyOf(results);
    }
}
