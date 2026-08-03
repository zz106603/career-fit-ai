package com.careerfit.ai.structured.infrastructure;

import com.careerfit.ai.structured.domain.AiCallStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_call_attempt")
@IdClass(AiCallAttemptId.class)
class AiCallAttemptEntity {
    @Id @Column(name = "ai_call_execution_id") private UUID executionId;
    @Id @Column(name = "attempt_number") private int attemptNumber;
    @Enumerated(EnumType.STRING) @Column(name = "status", nullable = false) private AiCallStatus status;
    @Column(name = "provider", length = 100) private String provider;
    @Column(name = "model", length = 200) private String model;
    @Column(name = "provider_request_id", length = 200) private String providerRequestId;
    @Column(name = "failure_code", length = 100) private String failureCode;
    @Column(name = "started_at", nullable = false) private Instant startedAt;
    @Column(name = "completed_at") private Instant completedAt;
    @Column(name = "input_tokens") private Integer inputTokens;
    @Column(name = "output_tokens") private Integer outputTokens;
    @Column(name = "total_tokens") private Integer totalTokens;
    @Column(name = "prompt_length", nullable = false) private int promptLength;
    @Column(name = "prompt_checksum_sha256", nullable = false, length = 64) private String promptChecksum;
    @Column(name = "response_length") private Integer responseLength;
    @Column(name = "response_checksum_sha256", length = 64) private String responseChecksum;

    protected AiCallAttemptEntity() {}

    AiCallAttemptEntity(
            UUID executionId, int attemptNumber, AiCallStatus status,
            String provider, String model, String providerRequestId, String failureCode,
            Instant startedAt, Instant completedAt, Integer inputTokens, Integer outputTokens,
            Integer totalTokens, int promptLength, String promptChecksum,
            Integer responseLength, String responseChecksum) {
        this.executionId=executionId; this.attemptNumber=attemptNumber; this.status=status;
        this.provider=provider; this.model=model; this.providerRequestId=providerRequestId;
        this.failureCode=failureCode; this.startedAt=startedAt; this.completedAt=completedAt;
        this.inputTokens=inputTokens; this.outputTokens=outputTokens; this.totalTokens=totalTokens;
        this.promptLength=promptLength; this.promptChecksum=promptChecksum;
        this.responseLength=responseLength; this.responseChecksum=responseChecksum;
    }
}
