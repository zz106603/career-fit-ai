package com.careerfit.analysis.match.infrastructure;

import com.careerfit.analysis.match.domain.RequirementMatchStatus;
import com.careerfit.job.structure.domain.JobRequirementCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "requirement_match_result")
class RequirementMatchResultEntity {

    @Id
    @Column(name = "job_analysis_result_id", nullable = false)
    private UUID resultId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "requirement_id", nullable = false)
    private UUID requirementId;

    @Column(name = "job_posting_analysis_id", nullable = false)
    private UUID jobPostingAnalysisId;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    private JobRequirementCategory category;

    @Column(name = "requirement_text", nullable = false, columnDefinition = "text")
    private String requirementText;

    @Column(name = "source_excerpt", nullable = false, columnDefinition = "text")
    private String sourceExcerpt;

    @Column(name = "sequence_no", nullable = false)
    private int sequence;

    @Enumerated(EnumType.STRING)
    @Column(name = "match_status", nullable = false, length = 30)
    private RequirementMatchStatus status;

    @Column(name = "reason", nullable = false, columnDefinition = "text")
    private String reason;

    protected RequirementMatchResultEntity() {}

    RequirementMatchResultEntity(
            UUID resultId,
            UUID userId,
            UUID requirementId,
            UUID jobPostingAnalysisId,
            JobRequirementCategory category,
            String requirementText,
            String sourceExcerpt,
            int sequence,
            RequirementMatchStatus status,
            String reason) {
        this.resultId = resultId;
        this.userId = userId;
        this.requirementId = requirementId;
        this.jobPostingAnalysisId = jobPostingAnalysisId;
        this.category = category;
        this.requirementText = requirementText;
        this.sourceExcerpt = sourceExcerpt;
        this.sequence = sequence;
        this.status = status;
        this.reason = reason;
    }

    UUID requirementId() { return requirementId; }
    UUID jobPostingAnalysisId() { return jobPostingAnalysisId; }
    JobRequirementCategory category() { return category; }
    String requirementText() { return requirementText; }
    String sourceExcerpt() { return sourceExcerpt; }
    int sequence() { return sequence; }
    RequirementMatchStatus status() { return status; }
    String reason() { return reason; }
}
