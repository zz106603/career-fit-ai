package com.careerfit.career.extraction.domain;

import com.careerfit.career.document.domain.CareerDocumentAnalysisId;
import com.careerfit.identity.UserId;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** AI 추출 결과를 사용자가 검토하는 동안 유지되는 미확정 경력 후보다. */
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

    public boolean isEditable() {
        return status == CareerExtractionCandidateStatus.PENDING_REVIEW
                || status == CareerExtractionCandidateStatus.EDITED;
    }

    public CareerExtractionCandidate edit(CareerCandidateContent content) {
        if (!isEditable()) throw new IllegalStateException("검토 가능한 후보만 수정할 수 있습니다.");
        return new CareerExtractionCandidate(id, analysisId, userId, content.candidateType(),
                content.organization(), content.role(), content.period(), content.description(),
                CareerExtractionCandidateStatus.EDITED, revisionNo + 1, model, promptVersion,
                schemaVersion, aiCallExecutionId, createdAt);
    }

    public CareerExtractionCandidate reject() {
        if (!isEditable()) throw new IllegalStateException("검토 가능한 후보만 거절할 수 있습니다.");
        return new CareerExtractionCandidate(id, analysisId, userId, candidateType, organization,
                role, period, description, CareerExtractionCandidateStatus.REJECTED, revisionNo,
                model, promptVersion, schemaVersion, aiCallExecutionId, createdAt);
    }

    public CareerExtractionCandidate confirm() {
        if (!isEditable()) throw new IllegalStateException("검토 가능한 후보만 확정할 수 있습니다.");
        return new CareerExtractionCandidate(id, analysisId, userId, candidateType, organization,
                role, period, description, CareerExtractionCandidateStatus.CONFIRMED, revisionNo,
                model, promptVersion, schemaVersion, aiCallExecutionId, createdAt);
    }
}
