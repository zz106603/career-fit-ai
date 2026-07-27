package com.careerfit.analysis.search.domain;

import java.util.Objects;
import java.util.UUID;

public record CareerCandidateSearchId(UUID value) {

    public CareerCandidateSearchId {
        Objects.requireNonNull(value, "경력 후보 검색 ID는 필수입니다.");
    }

    public static CareerCandidateSearchId newId() {
        return new CareerCandidateSearchId(UUID.randomUUID());
    }
}
