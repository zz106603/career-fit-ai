package com.careerfit.analysis.search.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
class CareerSearchCandidateSnapshotId implements Serializable {

    @Column(name = "candidate_search_id", nullable = false)
    private UUID searchId;

    @Column(name = "candidate_rank", nullable = false)
    private int rank;

    protected CareerSearchCandidateSnapshotId() {}

    CareerSearchCandidateSnapshotId(UUID searchId, int rank) {
        this.searchId = searchId;
        this.rank = rank;
    }

    UUID searchId() { return searchId; }
    int rank() { return rank; }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof CareerSearchCandidateSnapshotId other)) {
            return false;
        }
        return rank == other.rank && Objects.equals(searchId, other.searchId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(searchId, rank);
    }
}
