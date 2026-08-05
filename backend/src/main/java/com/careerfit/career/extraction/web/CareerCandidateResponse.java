package com.careerfit.career.extraction.web;

import com.careerfit.career.extraction.domain.CareerExtractionCandidate;
import java.util.UUID;

public record CareerCandidateResponse(
        UUID candidateId, String candidateType, String organization, String role, String period,
        String description, String status, int revisionNo) {
    static CareerCandidateResponse from(CareerExtractionCandidate candidate) {
        return new CareerCandidateResponse(candidate.id(), candidate.candidateType(),
                candidate.organization(), candidate.role(), candidate.period(), candidate.description(),
                candidate.status().name(), candidate.revisionNo());
    }
}
