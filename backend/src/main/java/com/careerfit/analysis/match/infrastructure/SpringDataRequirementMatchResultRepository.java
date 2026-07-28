package com.careerfit.analysis.match.infrastructure;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataRequirementMatchResultRepository
        extends JpaRepository<RequirementMatchResultEntity, UUID> {

    Optional<RequirementMatchResultEntity> findByResultIdAndUserId(UUID resultId, UUID userId);
}
