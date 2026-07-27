package com.careerfit.analysis.search.domain;

import com.careerfit.career.domain.CareerExperienceVersionId;
import java.util.Objects;

public record CareerSearchCandidate(
        CareerExperienceVersionId experienceVersionId,
        double score,
        int rank,
        String embeddingVersion) {

    public CareerSearchCandidate {
        Objects.requireNonNull(experienceVersionId, "경력 버전 ID는 필수입니다.");
        if (!Double.isFinite(score) || score < -1.0 || score > 1.0) {
            throw new IllegalArgumentException("검색 점수는 -1과 1 사이여야 합니다.");
        }
        if (rank < 1) {
            throw new IllegalArgumentException("검색 순위는 1 이상이어야 합니다.");
        }
        if (embeddingVersion == null || embeddingVersion.isBlank()) {
            throw new IllegalArgumentException("경력 embedding 버전은 필수입니다.");
        }
        embeddingVersion = embeddingVersion.trim();
    }
}
