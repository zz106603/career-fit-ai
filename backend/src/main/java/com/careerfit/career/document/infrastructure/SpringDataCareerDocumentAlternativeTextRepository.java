package com.careerfit.career.document.infrastructure;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataCareerDocumentAlternativeTextRepository
        extends JpaRepository<CareerDocumentAlternativeTextEntity, UUID> {

    Optional<CareerDocumentAlternativeTextEntity> findByAnalysisIdAndUserId(
            UUID analysisId, UUID userId);

    Optional<CareerDocumentAlternativeTextEntity> findFirstByUserIdAndDocumentIdOrderByCreatedAtDesc(
            UUID userId, UUID documentId);
}
