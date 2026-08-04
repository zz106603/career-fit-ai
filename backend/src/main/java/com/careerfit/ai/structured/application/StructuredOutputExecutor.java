package com.careerfit.ai.structured.application;

import com.careerfit.ai.port.LlmProviderPort;
import com.careerfit.ai.port.error.ProviderErrorType;
import com.careerfit.ai.port.error.ProviderException;
import com.careerfit.ai.port.model.LlmRequest;
import com.careerfit.ai.port.model.LlmResponse;
import com.careerfit.ai.port.model.TokenUsage;
import com.careerfit.ai.structured.domain.AiCallAttempt;
import com.careerfit.ai.structured.domain.AiCallExecution;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class StructuredOutputExecutor {

    private static final Logger log = LoggerFactory.getLogger(StructuredOutputExecutor.class);

    private final LlmProviderPort provider;
    private final ObjectMapper objectMapper;
    private final AiCallObservationPersistence persistence;
    private final AiRetryPolicy retryPolicy;
    private final AiRetrySleeper sleeper;
    private final Clock clock;

    public StructuredOutputExecutor(
            LlmProviderPort provider,
            ObjectMapper objectMapper,
            AiCallObservationPersistence persistence,
            AiRetryPolicy retryPolicy,
            AiRetrySleeper sleeper,
            Clock clock) {
        this.provider = provider;
        this.objectMapper = objectMapper;
        this.persistence = persistence;
        this.retryPolicy = retryPolicy;
        this.sleeper = sleeper;
        this.clock = clock;
    }

    public <T> StructuredOutputResult<T> execute(StructuredOutputRequest<T> request) {
        AiCallExecution execution = AiCallExecution.start(
                request.workflowExecutionId(), request.requestId(), request.purpose(),
                request.promptVersion(), request.schemaVersion(), now());
        persistence.saveExecution(execution);
        String promptChecksum = checksum(request.prompt());
        int promptLength = request.prompt().codePointCount(0, request.prompt().length());

        for (int attemptNumber = 1; attemptNumber <= retryPolicy.maxAttempts(); attemptNumber++) {
            AiCallAttempt attempt = AiCallAttempt.start(
                    execution.id(), attemptNumber, now(), promptLength, promptChecksum);
            persistence.saveAttempt(attempt);
            AttemptResult<T> result = performAttempt(request, attempt);
            persistence.saveAttempt(result.attempt());

            if (result.value() != null) {
                persistence.saveExecution(execution.succeed(attemptNumber, now()));
                LlmResponse response = result.response();
                log.info(
                        "AI call succeeded: workflowExecutionId={}, aiCallExecutionId={}, "
                                + "purpose={}, attemptNumber={}, provider={}, model={}",
                        request.workflowExecutionId(), execution.id(), request.purpose(),
                        attemptNumber, response.provider(), response.model());
                return new StructuredOutputResult<>(
                        result.value(), execution.id(), response.provider(), response.model(),
                        request.promptVersion(), request.schemaVersion(), attemptNumber,
                        response.tokenUsage());
            }

            StructuredOutputFailure failure = result.failure();
            log.warn(
                    "AI call attempt failed: workflowExecutionId={}, aiCallExecutionId={}, "
                            + "purpose={}, attemptNumber={}, failureCode={}, retryable={}",
                    request.workflowExecutionId(), execution.id(), request.purpose(),
                    attemptNumber, failure, failure.retryable());
            if (!failure.retryable() || attemptNumber == retryPolicy.maxAttempts()) {
                persistence.saveExecution(execution.fail(failure.name(), attemptNumber, now()));
                throw new StructuredOutputException(execution.id(), failure, attemptNumber);
            }
            sleep(execution, failure, attemptNumber);
        }
        throw new IllegalStateException("AI 재시도 반복문이 비정상 종료되었습니다.");
    }

    private <T> AttemptResult<T> performAttempt(
            StructuredOutputRequest<T> request, AiCallAttempt attempt) {
        try {
            LlmResponse response = provider.generate(
                    new LlmRequest(request.prompt(), request.schemaName(), request.schemaJson()));
            ResponseMetadata metadata = metadata(response);
            JsonNode root;
            try {
                root = objectMapper.readTree(response.content());
            } catch (RuntimeException exception) {
                return failed(attempt, response, metadata, StructuredOutputFailure.RESPONSE_PARSE_FAILED);
            }
            if (root == null) {
                return failed(attempt, response, metadata, StructuredOutputFailure.RESPONSE_PARSE_FAILED);
            }
            try {
                T value = request.decoder().decode(root);
                if (value == null) {
                    return failed(attempt, response, metadata,
                            StructuredOutputFailure.RESPONSE_SCHEMA_INVALID);
                }
                return new AttemptResult<>(value, response, null,
                        attempt.succeed(
                                response.provider(), response.model(), response.providerRequestId(),
                                metadata.usage().inputTokens(), metadata.usage().outputTokens(),
                                metadata.usage().totalTokens(), metadata.length(), metadata.checksum(), now()));
            } catch (StructuredOutputValidationException exception) {
                return failed(attempt, response, metadata, StructuredOutputFailure.RESPONSE_SCHEMA_INVALID);
            } catch (RuntimeException exception) {
                return failed(attempt, response, metadata, StructuredOutputFailure.UNEXPECTED_AI_ERROR);
            }
        } catch (ProviderException exception) {
            StructuredOutputFailure failure = map(exception.errorType());
            return new AttemptResult<>(null, null, failure,
                    attempt.fail(null, null, null, failure.name(),
                            null, null, null, null, null, now()));
        } catch (RuntimeException exception) {
            StructuredOutputFailure failure = StructuredOutputFailure.UNEXPECTED_AI_ERROR;
            return new AttemptResult<>(null, null, failure,
                    attempt.fail(null, null, null, failure.name(),
                            null, null, null, null, null, now()));
        }
    }

    private <T> AttemptResult<T> failed(
            AiCallAttempt attempt,
            LlmResponse response,
            ResponseMetadata metadata,
            StructuredOutputFailure failure) {
        return new AttemptResult<>(null, response, failure,
                attempt.fail(response.provider(), response.model(), response.providerRequestId(),
                        failure.name(), metadata.usage().inputTokens(), metadata.usage().outputTokens(),
                        metadata.usage().totalTokens(), metadata.length(), metadata.checksum(), now()));
    }

    private void sleep(AiCallExecution execution, StructuredOutputFailure failure, int attemptNumber) {
        try {
            sleeper.sleep(retryPolicy.delayMillisAfter(attemptNumber));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            persistence.saveExecution(execution.fail(
                    StructuredOutputFailure.UNEXPECTED_AI_ERROR.name(), attemptNumber, now()));
            throw new StructuredOutputException(
                    execution.id(), StructuredOutputFailure.UNEXPECTED_AI_ERROR, attemptNumber);
        }
    }

    private StructuredOutputFailure map(ProviderErrorType errorType) {
        return switch (errorType) {
            case TIMEOUT -> StructuredOutputFailure.PROVIDER_TIMEOUT;
            case RATE_LIMIT -> StructuredOutputFailure.PROVIDER_RATE_LIMITED;
            case PROVIDER_ERROR -> StructuredOutputFailure.PROVIDER_UNAVAILABLE;
            case CONFIGURATION_ERROR -> StructuredOutputFailure.PROVIDER_CONFIGURATION_ERROR;
            case POLICY_REJECTION -> StructuredOutputFailure.PROVIDER_POLICY_REJECTED;
            case INVALID_RESPONSE -> StructuredOutputFailure.RESPONSE_PARSE_FAILED;
        };
    }

    private ResponseMetadata metadata(LlmResponse response) {
        String content = response.content();
        return new ResponseMetadata(content.codePointCount(0, content.length()),
                checksum(content), response.tokenUsage() == null ? TokenUsage.unknown() : response.tokenUsage());
    }

    private String checksum(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", exception);
        }
    }

    private Instant now() { return clock.instant(); }

    private record AttemptResult<T>(
            T value, LlmResponse response, StructuredOutputFailure failure, AiCallAttempt attempt) {}

    private record ResponseMetadata(int length, String checksum, TokenUsage usage) {}
}
