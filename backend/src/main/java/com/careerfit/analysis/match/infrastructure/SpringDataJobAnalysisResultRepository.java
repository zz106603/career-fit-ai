package com.careerfit.analysis.match.infrastructure;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataJobAnalysisResultRepository
        extends JpaRepository<JobAnalysisResultEntity, UUID> {

    Optional<JobAnalysisResultEntity> findByIdAndUserId(UUID id, UUID userId);
}
