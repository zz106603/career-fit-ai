package com.careerfit.career.extraction.infrastructure;

import com.careerfit.career.extraction.domain.CareerExtractionCandidateStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "career_extraction_candidate")
class CareerExtractionCandidateEntity {
    @Id @Column(name = "candidate_id") private UUID id;
    @Column(name = "document_analysis_id", nullable = false) private UUID analysisId;
    @Column(name = "user_id", nullable = false) private UUID userId;
    @Column(name = "candidate_type", nullable = false) private String candidateType;
    private String organization;
    private String role;
    private String period;
    @Column(nullable = false, columnDefinition = "text") private String description;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private CareerExtractionCandidateStatus status;
    @Column(name = "revision_no", nullable = false) private int revisionNo;
    @Column(nullable = false) private String model;
    @Column(name = "prompt_version", nullable = false) private String promptVersion;
    @Column(name = "schema_version", nullable = false) private String schemaVersion;
    @Column(name = "ai_call_execution_id", nullable = false) private UUID aiCallExecutionId;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    protected CareerExtractionCandidateEntity() {}
    CareerExtractionCandidateEntity(UUID id, UUID analysisId, UUID userId, String candidateType,
            String organization, String role, String period, String description,
            CareerExtractionCandidateStatus status, int revisionNo, String model,
            String promptVersion, String schemaVersion, UUID aiCallExecutionId, Instant createdAt) {
        this.id=id; this.analysisId=analysisId; this.userId=userId; this.candidateType=candidateType;
        this.organization=organization; this.role=role; this.period=period; this.description=description;
        this.status=status; this.revisionNo=revisionNo; this.model=model; this.promptVersion=promptVersion;
        this.schemaVersion=schemaVersion; this.aiCallExecutionId=aiCallExecutionId; this.createdAt=createdAt;
    }

    UUID id() { return id; }
    UUID analysisId() { return analysisId; }
    UUID userId() { return userId; }
    String candidateType() { return candidateType; }
    String organization() { return organization; }
    String role() { return role; }
    String period() { return period; }
    String description() { return description; }
    CareerExtractionCandidateStatus status() { return status; }
    int revisionNo() { return revisionNo; }
    String model() { return model; }
    String promptVersion() { return promptVersion; }
    String schemaVersion() { return schemaVersion; }
    UUID aiCallExecutionId() { return aiCallExecutionId; }
    Instant createdAt() { return createdAt; }
}
