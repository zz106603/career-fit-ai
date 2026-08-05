package com.careerfit.career.extraction.infrastructure;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataCareerExperienceEvidenceRepository
        extends JpaRepository<CareerExperienceEvidenceEntity, UUID> {
    List<CareerExperienceEvidenceEntity> findAllByUserIdAndVersionId(UUID userId, UUID versionId);
    boolean existsByUserIdAndVersionId(UUID userId, UUID versionId);
}
