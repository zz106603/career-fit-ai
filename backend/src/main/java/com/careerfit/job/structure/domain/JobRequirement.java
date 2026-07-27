package com.careerfit.job.structure.domain;

import java.util.Objects;

public record JobRequirement(
        JobRequirementId id,
        JobPostingAnalysisId analysisId,
        JobRequirementCategory category,
        String text,
        String sourceExcerpt,
        int sequence) {

    public JobRequirement {
        Objects.requireNonNull(id, "요구사항 ID는 필수입니다.");
        Objects.requireNonNull(analysisId, "공고 구조화 ID는 필수입니다.");
        Objects.requireNonNull(category, "요구사항 분류는 필수입니다.");
        text = requireText(text, "요구사항 내용");
        sourceExcerpt = requireText(sourceExcerpt, "원문 발췌");
        if (sequence < 1) {
            throw new IllegalArgumentException("요구사항 순서는 1 이상이어야 합니다.");
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + "은 필수입니다.");
        }
        return value.trim();
    }
}
