package com.careerfit.career.extraction.application;

import java.util.UUID;

public record CareerCandidateEvidenceView(
        UUID candidateId, UUID documentId, String documentName, int pageNumber, String excerpt) {}
