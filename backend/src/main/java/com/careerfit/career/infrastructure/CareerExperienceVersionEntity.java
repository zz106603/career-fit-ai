package com.careerfit.career.infrastructure;

import com.careerfit.career.domain.CareerExperienceSourceType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "career_experience_version")
class CareerExperienceVersionEntity {

    @Id
    @Column(name = "experience_version_id", nullable = false)
    private UUID id;

    @Column(name = "experience_id", nullable = false)
    private UUID experienceId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "version_no", nullable = false)
    private int versionNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 20)
    private CareerExperienceSourceType sourceType;

    @Column(name = "experience_type", length = 100)
    private String experienceType;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "organization", length = 200)
    private String organization;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "role", length = 500)
    private String role;

    @Column(name = "responsibilities", columnDefinition = "text")
    private String responsibilities;

    @Column(name = "problem", columnDefinition = "text")
    private String problem;

    @Column(name = "action", columnDefinition = "text")
    private String action;

    @Column(name = "outcome", columnDefinition = "text")
    private String outcome;

    @Column(name = "technologies", columnDefinition = "text")
    private String technologies;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "superseded_at")
    private Instant supersededAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected CareerExperienceVersionEntity() {}

    CareerExperienceVersionEntity(
            UUID id,
            UUID experienceId,
            UUID userId,
            int versionNo,
            CareerExperienceSourceType sourceType,
            String experienceType,
            String title,
            String organization,
            LocalDate startDate,
            LocalDate endDate,
            String role,
            String responsibilities,
            String problem,
            String action,
            String outcome,
            String technologies,
            Instant createdAt,
            Instant confirmedAt,
            Instant supersededAt,
            Instant deletedAt) {
        this.id = id;
        this.experienceId = experienceId;
        this.userId = userId;
        this.versionNo = versionNo;
        this.sourceType = sourceType;
        this.experienceType = experienceType;
        this.title = title;
        this.organization = organization;
        this.startDate = startDate;
        this.endDate = endDate;
        this.role = role;
        this.responsibilities = responsibilities;
        this.problem = problem;
        this.action = action;
        this.outcome = outcome;
        this.technologies = technologies;
        this.createdAt = createdAt;
        this.confirmedAt = confirmedAt;
        this.supersededAt = supersededAt;
        this.deletedAt = deletedAt;
    }

    UUID id() { return id; }
    UUID experienceId() { return experienceId; }
    UUID userId() { return userId; }
    int versionNo() { return versionNo; }
    CareerExperienceSourceType sourceType() { return sourceType; }
    String experienceType() { return experienceType; }
    String title() { return title; }
    String organization() { return organization; }
    LocalDate startDate() { return startDate; }
    LocalDate endDate() { return endDate; }
    String role() { return role; }
    String responsibilities() { return responsibilities; }
    String problem() { return problem; }
    String action() { return action; }
    String outcome() { return outcome; }
    String technologies() { return technologies; }
    Instant createdAt() { return createdAt; }
    Instant confirmedAt() { return confirmedAt; }
    Instant supersededAt() { return supersededAt; }
    Instant deletedAt() { return deletedAt; }
}
