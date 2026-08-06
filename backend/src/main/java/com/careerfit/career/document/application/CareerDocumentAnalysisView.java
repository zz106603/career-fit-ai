package com.careerfit.career.document.application;

import com.careerfit.career.document.domain.CareerDocumentAnalysis;
import com.careerfit.career.document.domain.CareerDocumentAnalysisId;
import com.careerfit.career.document.domain.CareerDocumentAnalysisStatus;
import com.careerfit.career.document.domain.CareerDocumentId;
import com.careerfit.career.document.domain.CareerDocumentInputKind;
import com.careerfit.common.async.domain.JobExecutionId;
import java.time.Instant;

public record CareerDocumentAnalysisView(
        CareerDocumentId documentId,
        CareerDocumentAnalysisId documentAnalysisId,
        JobExecutionId jobExecutionId,
        CareerDocumentInputKind inputKind,
        String inputVersion,
        String workflowVersion,
        CareerDocumentAnalysisStatus status,
        String failureCode,
        CareerDocumentAnalysisNextAction nextAction,
        Instant createdAt,
        Instant startedAt,
        Instant completedAt) {

    static CareerDocumentAnalysisView from(CareerDocumentAnalysis analysis) {
        return new CareerDocumentAnalysisView(
                analysis.documentId(), analysis.id(), analysis.jobExecutionId(),
                analysis.inputKind(), analysis.inputVersion(), analysis.workflowVersion(),
                analysis.status(), analysis.failureCode(), nextAction(analysis),
                analysis.createdAt(), analysis.startedAt(), analysis.completedAt());
    }

    private static CareerDocumentAnalysisNextAction nextAction(CareerDocumentAnalysis analysis) {
        return switch (analysis.status()) {
            case QUEUED, PROCESSING -> CareerDocumentAnalysisNextAction.WAIT;
            case SUCCEEDED -> CareerDocumentAnalysisNextAction.REVIEW_CANDIDATES;
            case FAILED -> switch (analysis.failureCode()) {
                case "PDF_TEXT_EMPTY", "PDF_PARSE_FAILED", "PDF_ENCRYPTED" ->
                        CareerDocumentAnalysisNextAction.ENTER_ALTERNATIVE_TEXT;
                default -> CareerDocumentAnalysisNextAction.RETRY_FULL_ANALYSIS;
            };
        };
    }
}
