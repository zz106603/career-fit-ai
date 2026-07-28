package com.careerfit.job.structure.infrastructure;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataJobRequirementRepository extends JpaRepository<JobRequirementEntity, UUID> {

    Optional<JobRequirementEntity> findByAnalysisIdAndUserId(UUID analysisId, UUID userId);

    Optional<JobRequirementEntity> findByIdAndUserId(UUID id, UUID userId);
}
