package com.careerfit.career.extraction.web;

import com.careerfit.career.extraction.application.CareerCandidateEvidenceView;
import java.util.UUID;

public record CareerCandidateEvidenceResponse(
        UUID documentId, String documentName, int pageNumber, String excerpt) {
    static CareerCandidateEvidenceResponse from(CareerCandidateEvidenceView view) {
        return new CareerCandidateEvidenceResponse(
                view.documentId(), view.documentName(), view.pageNumber(), view.excerpt());
    }
}
