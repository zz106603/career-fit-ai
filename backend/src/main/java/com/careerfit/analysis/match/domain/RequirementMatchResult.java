package com.careerfit.analysis.match.domain;

import com.careerfit.job.structure.domain.JobPostingAnalysisId;
import com.careerfit.job.structure.domain.JobRequirementCategory;
import com.careerfit.job.structure.domain.JobRequirementId;
import java.util.Objects;

public record RequirementMatchResult(
        JobRequirementId requirementId,
        JobPostingAnalysisId jobPostingAnalysisId,
        JobRequirementCategory category,
        String requirementText,
        String sourceExcerpt,
        int sequence,
        RequirementMatchStatus status,
        String reason,
        CareerEvidenceSnapshot evidence) {

    public RequirementMatchResult {
        Objects.requireNonNull(requirementId, "요구사항 ID는 필수입니다.");
        Objects.requireNonNull(jobPostingAnalysisId, "공고 구조화 ID는 필수입니다.");
        Objects.requireNonNull(category, "요구사항 분류는 필수입니다.");
        requirementText = requireText(requirementText, "요구사항");
        sourceExcerpt = requireText(sourceExcerpt, "요구사항 원문 발췌");
        if (sequence < 1) {
            throw new IllegalArgumentException("요구사항 순서는 1 이상이어야 합니다.");
        }
        Objects.requireNonNull(status, "판정 상태는 필수입니다.");
        reason = requireText(reason, "판정 이유");
        validateEvidence(status, evidence);
    }

    private static void validateEvidence(
            RequirementMatchStatus status, CareerEvidenceSnapshot evidence) {
        if (status == RequirementMatchStatus.UNKNOWN) {
            return;
        }
        if (evidence == null) {
            throw new IllegalArgumentException("UNKNOWN 외 판정에는 경력 근거가 필요합니다.");
        }
        if (status == RequirementMatchStatus.NOT_SATISFIED && !evidence.explicitConflict()) {
            throw new IllegalArgumentException("NOT_SATISFIED에는 명시적 충돌 근거가 필요합니다.");
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + "은 필수입니다.");
        }
        return value.trim();
    }
}
