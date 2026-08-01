package com.careerfit.career.document.infrastructure;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataCareerDocumentPageRepository extends JpaRepository<CareerDocumentPageEntity, CareerDocumentPageId> {
    List<CareerDocumentPageEntity> findByAnalysisIdAndUserIdOrderByPageNumber(UUID analysisId, UUID userId);
}
