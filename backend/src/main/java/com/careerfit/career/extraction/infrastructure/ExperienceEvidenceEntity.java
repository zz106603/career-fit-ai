package com.careerfit.career.extraction.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "experience_evidence")
class ExperienceEvidenceEntity {
    @Id @Column(name = "evidence_id") private UUID id;
    @Column(name = "candidate_id", nullable = false) private UUID candidateId;
    @Column(name = "document_analysis_id", nullable = false) private UUID analysisId;
    @Column(name = "document_id", nullable = false) private UUID documentId;
    @Column(name = "user_id", nullable = false) private UUID userId;
    @Column(name = "page_number", nullable = false) private int pageNumber;
    @Column(nullable = false, columnDefinition = "text") private String excerpt;

    protected ExperienceEvidenceEntity() {}
    ExperienceEvidenceEntity(UUID id, UUID candidateId, UUID analysisId, UUID documentId,
            UUID userId, int pageNumber, String excerpt) {
        this.id=id; this.candidateId=candidateId; this.analysisId=analysisId; this.documentId=documentId;
        this.userId=userId; this.pageNumber=pageNumber; this.excerpt=excerpt;
    }

    UUID id() { return id; }
    UUID candidateId() { return candidateId; }
    UUID analysisId() { return analysisId; }
    UUID documentId() { return documentId; }
    UUID userId() { return userId; }
    int pageNumber() { return pageNumber; }
    String excerpt() { return excerpt; }
}
