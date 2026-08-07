package com.careerfit.career.extraction.web;

import com.careerfit.career.extraction.application.CareerCandidateReviewView;
import java.util.List;
import java.util.UUID;

public record CareerCandidateReviewResponse(
        UUID candidateId, String candidateType, String organization, String role, String period,
        String description, String status, int revisionNo,
        List<CareerCandidateEvidenceResponse> evidences) {
    static CareerCandidateReviewResponse from(CareerCandidateReviewView view) {
        var candidate = view.candidate();
        return new CareerCandidateReviewResponse(
                candidate.id(), candidate.candidateType(), candidate.organization(), candidate.role(),
                candidate.period(), candidate.description(), candidate.status().name(),
                candidate.revisionNo(),
                view.evidences().stream().map(CareerCandidateEvidenceResponse::from).toList());
    }
}
