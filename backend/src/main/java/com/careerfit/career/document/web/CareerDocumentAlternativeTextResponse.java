package com.careerfit.career.document.web;

import com.careerfit.career.document.application.CareerDocumentAlternativeTextResult;
import com.careerfit.career.document.domain.CareerDocumentAnalysisStatus;
import com.careerfit.career.document.domain.CareerDocumentInputKind;
import java.time.Instant;
import java.util.UUID;

public record CareerDocumentAlternativeTextResponse(
        UUID documentAnalysisId,
        CareerDocumentInputKind inputKind,
        CareerDocumentAnalysisStatus status,
        int textLength,
        Instant createdAt) {

    static CareerDocumentAlternativeTextResponse from(
            CareerDocumentAlternativeTextResult result) {
        return new CareerDocumentAlternativeTextResponse(
                result.analysisId().value(), result.inputKind(), result.status(),
                result.textLength(), result.createdAt());
    }
}
