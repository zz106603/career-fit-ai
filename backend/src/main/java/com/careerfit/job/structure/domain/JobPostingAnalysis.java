package com.careerfit.job.structure.domain;

import com.careerfit.identity.UserId;
import com.careerfit.job.domain.JobPostingId;
import java.time.Instant;
import java.util.Objects;

/** 공고 원문을 구조화한 한 번의 분석 결과로, 재실행 시 기존 값을 덮어쓰지 않는다. */
public record JobPostingAnalysis(
        JobPostingAnalysisId id,
        JobPostingId jobPostingId,
        UserId userId,
        JobPostingAnalysisStatus status,
        String companyName,
        String jobTitle,
        String workflowVersion,
        Instant createdAt,
        Instant readyAt,
        JobRequirement requirement) {

    public JobPostingAnalysis {
        Objects.requireNonNull(id, "공고 구조화 ID는 필수입니다.");
        Objects.requireNonNull(jobPostingId, "채용공고 ID는 필수입니다.");
        Objects.requireNonNull(userId, "사용자 ID는 필수입니다.");
        Objects.requireNonNull(status, "구조화 상태는 필수입니다.");
        companyName = normalize(companyName);
        jobTitle = normalize(jobTitle);
        workflowVersion = requireText(workflowVersion, "Workflow 버전");
        Objects.requireNonNull(createdAt, "생성 시각은 필수입니다.");
        validateState(status, readyAt, requirement, id);
    }

    public static JobPostingAnalysis processing(
            JobPostingId jobPostingId,
            UserId userId,
            String companyName,
            String jobTitle,
            String workflowVersion,
            Instant createdAt) {
        return new JobPostingAnalysis(
                JobPostingAnalysisId.newId(),
                jobPostingId,
                userId,
                JobPostingAnalysisStatus.PROCESSING,
                companyName,
                jobTitle,
                workflowVersion,
                createdAt,
                null,
                null);
    }

    public JobPostingAnalysis ready(JobRequirement requirement, Instant readyAt) {
        return new JobPostingAnalysis(
                id,
                jobPostingId,
                userId,
                JobPostingAnalysisStatus.READY,
                companyName,
                jobTitle,
                workflowVersion,
                createdAt,
                readyAt,
                requirement);
    }

    private static void validateState(
            JobPostingAnalysisStatus status,
            Instant readyAt,
            JobRequirement requirement,
            JobPostingAnalysisId analysisId) {
        if (status == JobPostingAnalysisStatus.PROCESSING) {
            if (readyAt != null || requirement != null) {
                throw new IllegalArgumentException("PROCESSING 구조화 결과는 요구사항을 가질 수 없습니다.");
            }
            return;
        }
        if (readyAt == null || requirement == null) {
            throw new IllegalArgumentException("READY 구조화 결과에는 요구사항과 완료 시각이 필요합니다.");
        }
        if (!requirement.analysisId().equals(analysisId)) {
            throw new IllegalArgumentException("요구사항은 동일한 공고 구조화 결과에 속해야 합니다.");
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
