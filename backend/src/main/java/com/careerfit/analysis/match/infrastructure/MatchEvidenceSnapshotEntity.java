package com.careerfit.analysis.match.infrastructure;

import com.careerfit.career.domain.CareerExperienceSourceType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "match_evidence_snapshot")
class MatchEvidenceSnapshotEntity {

    @Id
    @Column(name = "job_analysis_result_id", nullable = false)
    private UUID resultId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "experience_version_id", nullable = false)
    private UUID experienceVersionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 20)
    private CareerExperienceSourceType sourceType;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "role", length = 500)
    private String role;

    @Column(name = "responsibilities", columnDefinition = "text")
    private String responsibilities;

    @Column(name = "technologies", columnDefinition = "text")
    private String technologies;

    @Column(name = "search_score", nullable = false)
    private double searchScore;

    @Column(name = "search_rank", nullable = false)
    private int searchRank;

    @Column(name = "explicit_conflict", nullable = false)
    private boolean explicitConflict;

    protected MatchEvidenceSnapshotEntity() {}

    MatchEvidenceSnapshotEntity(
            UUID resultId,
            UUID userId,
            UUID experienceVersionId,
            CareerExperienceSourceType sourceType,
            String title,
            String role,
            String responsibilities,
            String technologies,
            double searchScore,
            int searchRank,
            boolean explicitConflict) {
        this.resultId = resultId;
        this.userId = userId;
        this.experienceVersionId = experienceVersionId;
        this.sourceType = sourceType;
        this.title = title;
        this.role = role;
        this.responsibilities = responsibilities;
        this.technologies = technologies;
        this.searchScore = searchScore;
        this.searchRank = searchRank;
        this.explicitConflict = explicitConflict;
    }

    UUID experienceVersionId() { return experienceVersionId; }
    CareerExperienceSourceType sourceType() { return sourceType; }
    String title() { return title; }
    String role() { return role; }
    String responsibilities() { return responsibilities; }
    String technologies() { return technologies; }
    double searchScore() { return searchScore; }
    int searchRank() { return searchRank; }
    boolean explicitConflict() { return explicitConflict; }
}
