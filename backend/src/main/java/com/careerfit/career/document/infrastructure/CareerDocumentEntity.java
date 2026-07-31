package com.careerfit.career.document.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "career_document")
class CareerDocumentEntity {

    @Id
    @Column(name = "career_document_id", nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "original_name", nullable = false, length = 500)
    private String originalName;

    @Column(name = "storage_reference", nullable = false, length = 1000, unique = true)
    private String storageReference;

    @Column(name = "byte_size", nullable = false)
    private long byteSize;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "checksum_sha256", nullable = false, length = 64)
    private String checksumSha256;

    @Column(name = "page_count", nullable = false)
    private int pageCount;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected CareerDocumentEntity() {}

    CareerDocumentEntity(
            UUID id,
            UUID userId,
            String originalName,
            String storageReference,
            long byteSize,
            String contentType,
            String checksumSha256,
            int pageCount,
            Instant uploadedAt,
            Instant deletedAt) {
        this.id = id;
        this.userId = userId;
        this.originalName = originalName;
        this.storageReference = storageReference;
        this.byteSize = byteSize;
        this.contentType = contentType;
        this.checksumSha256 = checksumSha256;
        this.pageCount = pageCount;
        this.uploadedAt = uploadedAt;
        this.deletedAt = deletedAt;
    }

    UUID id() { return id; }
    UUID userId() { return userId; }
    String originalName() { return originalName; }
    String storageReference() { return storageReference; }
    long byteSize() { return byteSize; }
    String contentType() { return contentType; }
    String checksumSha256() { return checksumSha256; }
    int pageCount() { return pageCount; }
    Instant uploadedAt() { return uploadedAt; }
    Instant deletedAt() { return deletedAt; }
}
