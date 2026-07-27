package com.careerfit.analysis.match.domain;

import com.careerfit.career.domain.CareerExperienceSourceType;
import com.careerfit.career.domain.CareerExperienceVersionId;
import java.util.Objects;

public record CareerEvidenceSnapshot(
        CareerExperienceVersionId experienceVersionId,
        CareerExperienceSourceType sourceType,
        String title,
        String role,
        String responsibilities,
        String technologies,
        double searchScore,
        int searchRank,
        boolean explicitConflict) {

    public CareerEvidenceSnapshot {
        Objects.requireNonNull(experienceVersionId, "경력 버전 ID는 필수입니다.");
        Objects.requireNonNull(sourceType, "경력 출처 유형은 필수입니다.");
        title = requireText(title, "경력 제목");
        role = normalize(role);
        responsibilities = normalize(responsibilities);
        technologies = normalize(technologies);
        if (searchRank < 1) {
            throw new IllegalArgumentException("검색 순위는 1 이상이어야 합니다.");
        }
    }

    private static String requireText(String value, String fieldName) {
        String normalized = normalize(value);
        if (normalized == null) {
            throw new IllegalArgumentException(fieldName + "은 필수입니다.");
        }
        return normalized;
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
