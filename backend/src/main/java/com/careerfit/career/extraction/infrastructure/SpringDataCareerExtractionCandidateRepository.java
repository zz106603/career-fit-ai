package com.careerfit.career.extraction.infrastructure;

import java.util.UUID;
import java.util.List;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataCareerExtractionCandidateRepository extends JpaRepository<CareerExtractionCandidateEntity, UUID> {
    boolean existsByUserIdAndAnalysisId(UUID userId, UUID analysisId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<CareerExtractionCandidateEntity> findAllByUserIdAndIdIn(UUID userId, List<UUID> ids);
}
