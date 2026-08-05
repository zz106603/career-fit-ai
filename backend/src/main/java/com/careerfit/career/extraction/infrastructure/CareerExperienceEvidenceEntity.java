package com.careerfit.career.extraction.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "career_experience_evidence")
class CareerExperienceEvidenceEntity {
    @Id @Column(name = "evidence_id") private UUID id;
    @Column(name = "experience_version_id", nullable = false) private UUID versionId;
    @Column(name = "candidate_id", nullable = false) private UUID candidateId;
    @Column(name = "document_analysis_id", nullable = false) private UUID analysisId;
    @Column(name = "document_id", nullable = false) private UUID documentId;
    @Column(name = "user_id", nullable = false) private UUID userId;
    @Column(name = "document_name", nullable = false) private String documentName;
    @Column(name = "page_number", nullable = false) private int pageNumber;
    @Column(nullable = false, columnDefinition = "text") private String excerpt;

    protected CareerExperienceEvidenceEntity() {}

    CareerExperienceEvidenceEntity(
            UUID id, UUID versionId, UUID candidateId, UUID analysisId, UUID documentId,
            UUID userId, String documentName, int pageNumber, String excerpt) {
        this.id = id;
        this.versionId = versionId;
        this.candidateId = candidateId;
        this.analysisId = analysisId;
        this.documentId = documentId;
        this.userId = userId;
        this.documentName = documentName;
        this.pageNumber = pageNumber;
        this.excerpt = excerpt;
    }

    UUID candidateId() { return candidateId; }
    UUID analysisId() { return analysisId; }
    UUID documentId() { return documentId; }
    UUID userId() { return userId; }
    String documentName() { return documentName; }
    int pageNumber() { return pageNumber; }
    String excerpt() { return excerpt; }
}
