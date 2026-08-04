package com.careerfit.career.extraction.infrastructure;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataCareerExtractionCandidateRepository extends JpaRepository<CareerExtractionCandidateEntity, UUID> {
    boolean existsByUserIdAndAnalysisId(UUID userId, UUID analysisId);
}
