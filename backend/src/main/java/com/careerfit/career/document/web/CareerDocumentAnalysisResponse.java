package com.careerfit.career.document.web;

import com.careerfit.career.document.application.CareerDocumentAnalysisNextAction;
import com.careerfit.career.document.application.CareerDocumentAnalysisView;
import com.careerfit.career.document.domain.CareerDocumentAnalysisStatus;
import com.careerfit.career.document.domain.CareerDocumentInputKind;
import java.time.Instant;
import java.util.UUID;

public record CareerDocumentAnalysisResponse(
        UUID documentId,
        UUID documentAnalysisId,
        UUID jobExecutionId,
        CareerDocumentInputKind inputKind,
        String inputVersion,
        String workflowVersion,
        CareerDocumentAnalysisStatus status,
        String failureCode,
        CareerDocumentAnalysisNextAction nextAction,
        Instant createdAt,
        Instant startedAt,
        Instant completedAt) {

    static CareerDocumentAnalysisResponse from(CareerDocumentAnalysisView view) {
        return new CareerDocumentAnalysisResponse(
                view.documentId().value(), view.documentAnalysisId().value(),
                view.jobExecutionId() == null ? null : view.jobExecutionId().value(),
                view.inputKind(), view.inputVersion(), view.workflowVersion(), view.status(),
                view.failureCode(), view.nextAction(), view.createdAt(), view.startedAt(),
                view.completedAt());
    }
}
