package com.careerfit.analysis.search.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "career_search_candidate_snapshot")
class CareerSearchCandidateSnapshotEntity {

    @EmbeddedId
    private CareerSearchCandidateSnapshotId id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "experience_version_id", nullable = false)
    private UUID experienceVersionId;

    @Column(name = "score", nullable = false)
    private double score;

    @Column(name = "embedding_version", nullable = false, length = 100)
    private String embeddingVersion;

    protected CareerSearchCandidateSnapshotEntity() {}

    CareerSearchCandidateSnapshotEntity(
            CareerSearchCandidateSnapshotId id,
            UUID userId,
            UUID experienceVersionId,
            double score,
            String embeddingVersion) {
        this.id = id;
        this.userId = userId;
        this.experienceVersionId = experienceVersionId;
        this.score = score;
        this.embeddingVersion = embeddingVersion;
    }

    UUID experienceVersionId() { return experienceVersionId; }
    double score() { return score; }
    int rank() { return id.rank(); }
    String embeddingVersion() { return embeddingVersion; }
}
