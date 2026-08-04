package com.careerfit.career.extraction.infrastructure;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataExperienceEvidenceRepository extends JpaRepository<ExperienceEvidenceEntity, UUID> {}
