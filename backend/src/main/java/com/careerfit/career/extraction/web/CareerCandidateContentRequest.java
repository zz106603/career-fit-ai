package com.careerfit.career.extraction.web;

import com.careerfit.career.extraction.domain.CareerCandidateContent;

public record CareerCandidateContentRequest(
        String candidateType, String organization, String role, String period, String description) {
    CareerCandidateContent toContent() {
        return new CareerCandidateContent(candidateType, organization, role, period, description);
    }
}
