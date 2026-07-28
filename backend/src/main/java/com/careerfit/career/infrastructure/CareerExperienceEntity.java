package com.careerfit.career.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "career_experience")
class CareerExperienceEntity {

    @Id
    @Column(name = "experience_id", nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected CareerExperienceEntity() {}

    CareerExperienceEntity(UUID id, UUID userId, Instant createdAt, Instant deletedAt) {
        this.id = id;
        this.userId = userId;
        this.createdAt = createdAt;
        this.deletedAt = deletedAt;
    }

    UUID id() {
        return id;
    }

    UUID userId() {
        return userId;
    }

    Instant createdAt() {
        return createdAt;
    }

    Instant deletedAt() {
        return deletedAt;
    }
}
