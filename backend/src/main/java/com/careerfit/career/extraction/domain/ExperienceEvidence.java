package com.careerfit.career.extraction.domain;

import com.careerfit.career.document.domain.CareerDocumentAnalysisId;
import com.careerfit.career.document.domain.CareerDocumentId;
import com.careerfit.identity.UserId;
import java.util.Objects;
import java.util.UUID;

public record ExperienceEvidence(
        UUID id, UUID candidateId, CareerDocumentAnalysisId analysisId,
        CareerDocumentId documentId, UserId userId, int pageNumber, String excerpt) {
    public ExperienceEvidence {
        Objects.requireNonNull(id); Objects.requireNonNull(candidateId); Objects.requireNonNull(analysisId);
        Objects.requireNonNull(documentId); Objects.requireNonNull(userId);
        if (pageNumber < 1) throw new IllegalArgumentException("근거 페이지는 1 이상이어야 합니다.");
        if (excerpt == null || excerpt.isBlank()) throw new IllegalArgumentException("근거 발췌는 필수입니다.");
        excerpt = excerpt.trim();
    }
}
