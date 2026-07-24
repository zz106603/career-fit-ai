package com.careerfit.ai.port.model;

import java.net.URI;
import java.util.Objects;

public record SearchResult(String title, URI url, String snippet) {

    public SearchResult {
        Objects.requireNonNull(title, "title은 null일 수 없습니다.");
        Objects.requireNonNull(url, "url은 null일 수 없습니다.");
        Objects.requireNonNull(snippet, "snippet은 null일 수 없습니다.");
    }
}
