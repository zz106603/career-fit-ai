package com.careerfit.job.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "job_posting")
class JobPostingEntity {

    @Id
    @Column(name = "job_posting_id", nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "original_text", nullable = false, columnDefinition = "text")
    private String originalText;

    @Column(name = "title_hint", length = 500)
    private String titleHint;

    @Column(name = "company_hint", length = 500)
    private String companyHint;

    @Column(name = "registered_at", nullable = false)
    private Instant registeredAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected JobPostingEntity() {}

    JobPostingEntity(
            UUID id,
            UUID userId,
            String originalText,
            String titleHint,
            String companyHint,
            Instant registeredAt,
            Instant deletedAt) {
        this.id = id;
        this.userId = userId;
        this.originalText = originalText;
        this.titleHint = titleHint;
        this.companyHint = companyHint;
        this.registeredAt = registeredAt;
        this.deletedAt = deletedAt;
    }

    UUID id() {
        return id;
    }

    UUID userId() {
        return userId;
    }

    String originalText() {
        return originalText;
    }

    String titleHint() {
        return titleHint;
    }

    String companyHint() {
        return companyHint;
    }

    Instant registeredAt() {
        return registeredAt;
    }

    Instant deletedAt() {
        return deletedAt;
    }
}
