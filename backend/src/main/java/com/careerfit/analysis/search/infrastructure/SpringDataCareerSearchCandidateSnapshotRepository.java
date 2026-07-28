package com.careerfit.analysis.search.infrastructure;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataCareerSearchCandidateSnapshotRepository
        extends JpaRepository<
                CareerSearchCandidateSnapshotEntity, CareerSearchCandidateSnapshotId> {

    List<CareerSearchCandidateSnapshotEntity>
            findByIdSearchIdAndUserIdOrderByIdRank(UUID searchId, UUID userId);
}
