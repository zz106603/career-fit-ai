package com.careerfit.common.async.infrastructure;

import com.careerfit.common.async.domain.JobExecutionStatus;
import com.careerfit.common.async.domain.JobType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "job_execution")
class JobExecutionEntity {

    @Id
    @Column(name = "job_execution_id", nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_type", nullable = false, length = 50)
    private JobType type;

    @Column(name = "target_id", nullable = false)
    private UUID targetId;

    @Column(name = "input_version", nullable = false, length = 200)
    private String inputVersion;

    @Column(name = "duplicate_key", nullable = false, length = 500)
    private String duplicateKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private JobExecutionStatus status;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "failure_code", length = 200)
    private String failureCode;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "claimed_at")
    private Instant claimedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected JobExecutionEntity() {}

    UUID id() {
        return id;
    }

    UUID userId() {
        return userId;
    }

    JobType type() {
        return type;
    }

    UUID targetId() {
        return targetId;
    }

    String inputVersion() {
        return inputVersion;
    }

    String duplicateKey() {
        return duplicateKey;
    }

    JobExecutionStatus status() {
        return status;
    }

    int retryCount() {
        return retryCount;
    }

    String failureCode() {
        return failureCode;
    }

    Instant createdAt() {
        return createdAt;
    }

    Instant claimedAt() {
        return claimedAt;
    }

    Instant completedAt() {
        return completedAt;
    }
}
