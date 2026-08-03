package com.careerfit.career.document.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "career_document_alternative_text")
class CareerDocumentAlternativeTextEntity {

    @Id
    @Column(name = "document_analysis_id")
    private UUID analysisId;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "alternative_text", nullable = false, columnDefinition = "text")
    private String text;

    @Column(name = "text_length", nullable = false)
    private int textLength;

    @Column(name = "checksum_sha256", nullable = false, length = 64)
    private String checksumSha256;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected CareerDocumentAlternativeTextEntity() {}

    CareerDocumentAlternativeTextEntity(
            UUID analysisId,
            UUID documentId,
            UUID userId,
            String text,
            int textLength,
            String checksumSha256,
            Instant createdAt) {
        this.analysisId = analysisId;
        this.documentId = documentId;
        this.userId = userId;
        this.text = text;
        this.textLength = textLength;
        this.checksumSha256 = checksumSha256;
        this.createdAt = createdAt;
    }

    UUID analysisId() { return analysisId; }
    UUID documentId() { return documentId; }
    UUID userId() { return userId; }
    String text() { return text; }
    int textLength() { return textLength; }
    String checksumSha256() { return checksumSha256; }
    Instant createdAt() { return createdAt; }
}
