package com.careerfit.ai.structured.infrastructure;

import com.careerfit.ai.structured.domain.AiCallStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_call_execution")
class AiCallExecutionEntity {
    @Id @Column(name = "ai_call_execution_id") private UUID id;
    @Column(name = "workflow_execution_id", nullable = false) private UUID workflowExecutionId;
    @Column(name = "request_id", length = 100) private String requestId;
    @Column(name = "purpose", nullable = false, length = 100) private String purpose;
    @Column(name = "prompt_version", nullable = false, length = 100) private String promptVersion;
    @Column(name = "schema_version", nullable = false, length = 100) private String schemaVersion;
    @Enumerated(EnumType.STRING) @Column(name = "status", nullable = false) private AiCallStatus status;
    @Column(name = "failure_code", length = 100) private String failureCode;
    @Column(name = "attempt_count", nullable = false) private int attemptCount;
    @Column(name = "started_at", nullable = false) private Instant startedAt;
    @Column(name = "completed_at") private Instant completedAt;

    protected AiCallExecutionEntity() {}

    AiCallExecutionEntity(
            UUID id, UUID workflowExecutionId, String requestId, String purpose,
            String promptVersion, String schemaVersion, AiCallStatus status,
            String failureCode, int attemptCount, Instant startedAt, Instant completedAt) {
        this.id=id; this.workflowExecutionId=workflowExecutionId; this.requestId=requestId;
        this.purpose=purpose; this.promptVersion=promptVersion; this.schemaVersion=schemaVersion;
        this.status=status; this.failureCode=failureCode; this.attemptCount=attemptCount;
        this.startedAt=startedAt; this.completedAt=completedAt;
    }
}
