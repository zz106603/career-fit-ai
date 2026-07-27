package com.careerfit.analysis.match.domain;

import com.careerfit.analysis.search.domain.CareerCandidateSearchId;
import com.careerfit.identity.UserId;
import com.careerfit.job.domain.JobPostingId;
import java.time.Instant;
import java.util.Objects;

public record JobAnalysisResult(
        JobAnalysisResultId id,
        UserId userId,
        JobPostingId jobPostingId,
        CareerCandidateSearchId candidateSearchId,
        String workflowVersion,
        Instant completedAt,
        RequirementMatchResult match) {

    public JobAnalysisResult {
        Objects.requireNonNull(id, "분석 결과 ID는 필수입니다.");
        Objects.requireNonNull(userId, "사용자 ID는 필수입니다.");
        Objects.requireNonNull(jobPostingId, "채용공고 ID는 필수입니다.");
        Objects.requireNonNull(candidateSearchId, "경력 검색 ID는 필수입니다.");
        if (workflowVersion == null || workflowVersion.isBlank()) {
            throw new IllegalArgumentException("Workflow 버전은 필수입니다.");
        }
        workflowVersion = workflowVersion.trim();
        Objects.requireNonNull(completedAt, "완료 시각은 필수입니다.");
        Objects.requireNonNull(match, "요구사항 판정은 필수입니다.");
    }
}
