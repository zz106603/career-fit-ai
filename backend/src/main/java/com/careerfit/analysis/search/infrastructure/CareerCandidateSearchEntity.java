package com.careerfit.analysis.search.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "career_candidate_search")
class CareerCandidateSearchEntity {

    @Id
    @Column(name = "candidate_search_id", nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "requirement_id", nullable = false)
    private UUID requirementId;

    @Column(name = "query_embedding_version", nullable = false, length = 100)
    private String queryEmbeddingVersion;

    @Column(name = "search_version", nullable = false, length = 100)
    private String searchVersion;

    @Column(name = "searched_at", nullable = false)
    private Instant searchedAt;

    protected CareerCandidateSearchEntity() {}

    CareerCandidateSearchEntity(
            UUID id,
            UUID userId,
            UUID requirementId,
            String queryEmbeddingVersion,
            String searchVersion,
            Instant searchedAt) {
        this.id = id;
        this.userId = userId;
        this.requirementId = requirementId;
        this.queryEmbeddingVersion = queryEmbeddingVersion;
        this.searchVersion = searchVersion;
        this.searchedAt = searchedAt;
    }

    UUID id() { return id; }
    UUID userId() { return userId; }
    UUID requirementId() { return requirementId; }
    String queryEmbeddingVersion() { return queryEmbeddingVersion; }
    String searchVersion() { return searchVersion; }
    Instant searchedAt() { return searchedAt; }
}
