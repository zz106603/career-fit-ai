package com.careerfit.ai.structured.infrastructure;

import com.careerfit.ai.structured.application.AiCallObservationRepository;
import com.careerfit.ai.structured.domain.AiCallAttempt;
import com.careerfit.ai.structured.domain.AiCallExecution;
import org.springframework.stereotype.Repository;

@Repository
public class JpaAiCallObservationRepository implements AiCallObservationRepository {

    private final SpringDataAiCallExecutionRepository executions;
    private final SpringDataAiCallAttemptRepository attempts;

    public JpaAiCallObservationRepository(
            SpringDataAiCallExecutionRepository executions,
            SpringDataAiCallAttemptRepository attempts) {
        this.executions = executions;
        this.attempts = attempts;
    }

    @Override
    public void saveExecution(AiCallExecution value) {
        executions.saveAndFlush(new AiCallExecutionEntity(
                value.id(), value.workflowExecutionId(), value.requestId(), value.purpose(),
                value.promptVersion(), value.schemaVersion(), value.status(), value.failureCode(),
                value.attemptCount(), value.startedAt(), value.completedAt()));
    }

    @Override
    public void saveAttempt(AiCallAttempt value) {
        attempts.saveAndFlush(new AiCallAttemptEntity(
                value.executionId(), value.attemptNumber(), value.status(), value.provider(),
                value.model(), value.providerRequestId(), value.failureCode(), value.startedAt(),
                value.completedAt(), value.inputTokens(), value.outputTokens(), value.totalTokens(),
                value.promptLength(), value.promptChecksumSha256(), value.responseLength(),
                value.responseChecksumSha256()));
    }
}
