package com.careerfit.career.extraction.domain;

import com.careerfit.career.document.domain.CareerDocumentAnalysisId;
import com.careerfit.identity.UserId;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record CareerExtractionCandidate(
        UUID id, CareerDocumentAnalysisId analysisId, UserId userId, String candidateType,
        String organization, String role, String period, String description,
        CareerExtractionCandidateStatus status, int revisionNo, String model,
        String promptVersion, String schemaVersion, UUID aiCallExecutionId, Instant createdAt) {

    public CareerExtractionCandidate {
        Objects.requireNonNull(id); Objects.requireNonNull(analysisId); Objects.requireNonNull(userId);
        candidateType = required(candidateType); description = required(description);
        Objects.requireNonNull(status); Objects.requireNonNull(aiCallExecutionId); Objects.requireNonNull(createdAt);
        if (revisionNo < 1) throw new IllegalArgumentException("후보 revision은 1 이상이어야 합니다.");
    }

    private static String required(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("후보 필수값이 없습니다.");
        return value.trim();
    }
}
