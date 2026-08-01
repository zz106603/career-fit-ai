package com.careerfit.career.document.infrastructure;

import com.careerfit.career.document.domain.CareerDocumentAnalysisStatus;
import com.careerfit.career.document.domain.CareerDocumentInputKind;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "career_document_analysis")
class CareerDocumentAnalysisEntity {
    @Id @Column(name = "document_analysis_id") private UUID id;
    @Column(name = "document_id", nullable = false) private UUID documentId;
    @Column(name = "user_id", nullable = false) private UUID userId;
    @Column(name = "job_execution_id", nullable = false) private UUID jobExecutionId;
    @Enumerated(EnumType.STRING) @Column(name = "input_kind", nullable = false) private CareerDocumentInputKind inputKind;
    @Enumerated(EnumType.STRING) @Column(name = "status", nullable = false) private CareerDocumentAnalysisStatus status;
    @Column(name = "input_version", nullable = false, length = 200) private String inputVersion;
    @Column(name = "workflow_version", nullable = false, length = 100) private String workflowVersion;
    @Column(name = "extracted_text_reference", length = 500) private String extractedTextReference;
    @Column(name = "failure_code", length = 200) private String failureCode;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "started_at") private Instant startedAt;
    @Column(name = "completed_at") private Instant completedAt;

    protected CareerDocumentAnalysisEntity() {}

    CareerDocumentAnalysisEntity(UUID id, UUID documentId, UUID userId, UUID jobExecutionId,
            CareerDocumentInputKind inputKind, CareerDocumentAnalysisStatus status,
            String inputVersion, String workflowVersion, String extractedTextReference,
            String failureCode, Instant createdAt, Instant startedAt, Instant completedAt) {
        this.id=id; this.documentId=documentId; this.userId=userId; this.jobExecutionId=jobExecutionId;
        this.inputKind=inputKind; this.status=status; this.inputVersion=inputVersion;
        this.workflowVersion=workflowVersion; this.extractedTextReference=extractedTextReference;
        this.failureCode=failureCode; this.createdAt=createdAt; this.startedAt=startedAt;
        this.completedAt=completedAt;
    }

    UUID id(){return id;} UUID documentId(){return documentId;} UUID userId(){return userId;}
    UUID jobExecutionId(){return jobExecutionId;} CareerDocumentInputKind inputKind(){return inputKind;}
    CareerDocumentAnalysisStatus status(){return status;} String inputVersion(){return inputVersion;}
    String workflowVersion(){return workflowVersion;} String extractedTextReference(){return extractedTextReference;}
    String failureCode(){return failureCode;} Instant createdAt(){return createdAt;}
    Instant startedAt(){return startedAt;} Instant completedAt(){return completedAt;}
}
