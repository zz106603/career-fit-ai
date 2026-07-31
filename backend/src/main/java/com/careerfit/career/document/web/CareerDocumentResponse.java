package com.careerfit.career.document.web;

import com.careerfit.career.document.domain.CareerDocument;
import java.time.Instant;
import java.util.UUID;

public record CareerDocumentResponse(
        UUID documentId,
        String originalName,
        long byteSize,
        int pageCount,
        Instant uploadedAt) {

    static CareerDocumentResponse from(CareerDocument document) {
        return new CareerDocumentResponse(
                document.id().value(),
                document.originalName(),
                document.byteSize(),
                document.pageCount(),
                document.uploadedAt());
    }
}
