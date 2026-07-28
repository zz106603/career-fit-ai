package com.careerfit.analysis.match.infrastructure;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataMatchEvidenceSnapshotRepository
        extends JpaRepository<MatchEvidenceSnapshotEntity, UUID> {

    Optional<MatchEvidenceSnapshotEntity> findByResultIdAndUserId(
            UUID resultId, UUID userId);
}
