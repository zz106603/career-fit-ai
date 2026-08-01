package com.careerfit.career.document.infrastructure;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "career_document_page")
@IdClass(CareerDocumentPageId.class)
class CareerDocumentPageEntity {
    @Id @Column(name = "document_analysis_id") private UUID analysisId;
    @Id @Column(name = "page_number") private int pageNumber;
    @Column(name = "document_id", nullable = false) private UUID documentId;
    @Column(name = "user_id", nullable = false) private UUID userId;
    @Column(name = "page_text", nullable = false, columnDefinition = "text") private String text;
    @Column(name = "text_length", nullable = false) private int textLength;
    @Column(name = "checksum_sha256", nullable = false, length = 64) private String checksumSha256;
    protected CareerDocumentPageEntity() {}
    CareerDocumentPageEntity(UUID analysisId, UUID documentId, UUID userId, int pageNumber,
            String text, int textLength, String checksumSha256) {
        this.analysisId=analysisId; this.documentId=documentId; this.userId=userId;
        this.pageNumber=pageNumber; this.text=text; this.textLength=textLength;
        this.checksumSha256=checksumSha256;
    }
    UUID analysisId(){return analysisId;} UUID documentId(){return documentId;} UUID userId(){return userId;}
    int pageNumber(){return pageNumber;} String text(){return text;} int textLength(){return textLength;}
    String checksumSha256(){return checksumSha256;}
}
