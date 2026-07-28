package com.careerfit.analysis.match.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "job_analysis_result")
class JobAnalysisResultEntity {

    @Id
    @Column(name = "job_analysis_result_id", nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "job_posting_id", nullable = false)
    private UUID jobPostingId;

    @Column(name = "candidate_search_id", nullable = false)
    private UUID candidateSearchId;

    @Column(name = "workflow_version", nullable = false, length = 100)
    private String workflowVersion;

    @Column(name = "completed_at", nullable = false)
    private Instant completedAt;

    protected JobAnalysisResultEntity() {}

    JobAnalysisResultEntity(
            UUID id,
            UUID userId,
            UUID jobPostingId,
            UUID candidateSearchId,
            String workflowVersion,
            Instant completedAt) {
        this.id = id;
        this.userId = userId;
        this.jobPostingId = jobPostingId;
        this.candidateSearchId = candidateSearchId;
        this.workflowVersion = workflowVersion;
        this.completedAt = completedAt;
    }

    UUID id() { return id; }
    UUID userId() { return userId; }
    UUID jobPostingId() { return jobPostingId; }
    UUID candidateSearchId() { return candidateSearchId; }
    String workflowVersion() { return workflowVersion; }
    Instant completedAt() { return completedAt; }
}
