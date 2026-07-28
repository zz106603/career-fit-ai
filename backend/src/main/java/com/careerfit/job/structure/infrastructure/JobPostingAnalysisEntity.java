package com.careerfit.job.structure.infrastructure;

import com.careerfit.job.structure.domain.JobPostingAnalysisStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "job_posting_analysis")
class JobPostingAnalysisEntity {

    @Id
    @Column(name = "job_posting_analysis_id", nullable = false)
    private UUID id;

    @Column(name = "job_posting_id", nullable = false)
    private UUID jobPostingId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private JobPostingAnalysisStatus status;

    @Column(name = "company_name", length = 500)
    private String companyName;

    @Column(name = "job_title", length = 500)
    private String jobTitle;

    @Column(name = "workflow_version", nullable = false, length = 200)
    private String workflowVersion;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "ready_at")
    private Instant readyAt;

    protected JobPostingAnalysisEntity() {}

    JobPostingAnalysisEntity(
            UUID id,
            UUID jobPostingId,
            UUID userId,
            JobPostingAnalysisStatus status,
            String companyName,
            String jobTitle,
            String workflowVersion,
            Instant createdAt,
            Instant readyAt) {
        this.id = id;
        this.jobPostingId = jobPostingId;
        this.userId = userId;
        this.status = status;
        this.companyName = companyName;
        this.jobTitle = jobTitle;
        this.workflowVersion = workflowVersion;
        this.createdAt = createdAt;
        this.readyAt = readyAt;
    }

    UUID id() { return id; }
    UUID jobPostingId() { return jobPostingId; }
    UUID userId() { return userId; }
    JobPostingAnalysisStatus status() { return status; }
    String companyName() { return companyName; }
    String jobTitle() { return jobTitle; }
    String workflowVersion() { return workflowVersion; }
    Instant createdAt() { return createdAt; }
    Instant readyAt() { return readyAt; }
}
