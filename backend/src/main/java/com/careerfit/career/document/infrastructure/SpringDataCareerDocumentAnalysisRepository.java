package com.careerfit.career.document.infrastructure;

import com.careerfit.career.document.domain.CareerDocumentAnalysisStatus;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataCareerDocumentAnalysisRepository extends JpaRepository<CareerDocumentAnalysisEntity, UUID> {
    Optional<CareerDocumentAnalysisEntity> findByIdAndUserId(UUID id, UUID userId);
    Optional<CareerDocumentAnalysisEntity> findFirstByUserIdAndDocumentIdAndInputVersionAndStatusIn(
            UUID userId, UUID documentId, String inputVersion,
            Collection<CareerDocumentAnalysisStatus> statuses);
}
