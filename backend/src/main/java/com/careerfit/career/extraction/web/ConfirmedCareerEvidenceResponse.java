package com.careerfit.career.extraction.web;

import com.careerfit.career.extraction.application.ConfirmedCareerEvidenceView;
import java.util.UUID;

public record ConfirmedCareerEvidenceResponse(
        UUID documentId, String documentName, int pageNumber, String excerpt) {
    static ConfirmedCareerEvidenceResponse from(ConfirmedCareerEvidenceView view) {
        return new ConfirmedCareerEvidenceResponse(
                view.documentId(), view.documentName(), view.pageNumber(), view.excerpt());
    }
}
