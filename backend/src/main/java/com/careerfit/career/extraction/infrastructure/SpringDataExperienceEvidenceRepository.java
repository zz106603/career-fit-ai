package com.careerfit.career.extraction.infrastructure;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SpringDataExperienceEvidenceRepository extends JpaRepository<ExperienceEvidenceEntity, UUID> {
    java.util.List<ExperienceEvidenceEntity> findAllByUserIdAndCandidateIdIn(
            UUID userId, java.util.List<UUID> candidateIds);

    @Query("""
            select new com.careerfit.career.extraction.infrastructure.ConfirmedEvidenceSource(
                evidence.candidateId, evidence.analysisId, evidence.documentId, evidence.userId,
                document.originalName, evidence.pageNumber, evidence.excerpt)
              from ExperienceEvidenceEntity evidence, CareerDocumentEntity document
             where evidence.documentId = document.id
               and evidence.userId = :userId
               and evidence.candidateId = :candidateId
               and document.userId = :userId
               and document.deletedAt is null
            """)
    java.util.List<ConfirmedEvidenceSource> findSources(
            @Param("userId") UUID userId, @Param("candidateId") UUID candidateId);
}
