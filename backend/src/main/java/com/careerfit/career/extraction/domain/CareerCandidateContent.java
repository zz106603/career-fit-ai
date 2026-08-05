package com.careerfit.career.extraction.domain;

public record CareerCandidateContent(
        String candidateType, String organization, String role, String period, String description) {
    public CareerCandidateContent {
        if (candidateType == null || candidateType.isBlank()) {
            throw new IllegalArgumentException("후보 유형은 필수입니다.");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("후보 내용은 필수입니다.");
        }
        candidateType = candidateType.trim();
        organization = trimToNull(organization);
        role = trimToNull(role);
        period = trimToNull(period);
        description = description.trim();
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
