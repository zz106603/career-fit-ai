package com.careerfit.career.extraction.application;

public record ExtractedCareerCandidate(
        String candidateType, String organization, String role, String period,
        String description, int pageNumber, String excerpt) {}
