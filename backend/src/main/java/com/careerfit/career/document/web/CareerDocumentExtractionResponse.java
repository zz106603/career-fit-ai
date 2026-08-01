package com.careerfit.career.document.web;

import com.careerfit.career.document.application.CareerDocumentExtractionResult;
import com.careerfit.career.document.domain.CareerDocumentAnalysisStatus;
import java.util.UUID;

public record CareerDocumentExtractionResponse(
        UUID analysisId, UUID jobExecutionId, CareerDocumentAnalysisStatus status) {

    static CareerDocumentExtractionResponse from(CareerDocumentExtractionResult result) {
        return new CareerDocumentExtractionResponse(
                result.analysisId().value(), result.jobExecutionId().value(), result.status());
    }
}
