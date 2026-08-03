package com.careerfit.career.document.application;

import com.careerfit.career.document.domain.CareerDocumentAnalysisId;
import com.careerfit.career.document.domain.CareerDocumentAnalysisStatus;
import com.careerfit.career.document.domain.CareerDocumentInputKind;
import java.time.Instant;

public record CareerDocumentAlternativeTextResult(
        CareerDocumentAnalysisId analysisId,
        CareerDocumentInputKind inputKind,
        CareerDocumentAnalysisStatus status,
        int textLength,
        Instant createdAt,
        boolean created) {}
