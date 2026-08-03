package com.careerfit.ai.structured.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.careerfit.ai.port.LlmProviderPort;
import com.careerfit.ai.port.error.ProviderErrorType;
import com.careerfit.ai.port.error.ProviderException;
import com.careerfit.ai.port.model.LlmResponse;
import com.careerfit.ai.port.model.TokenUsage;
import com.careerfit.ai.structured.domain.AiCallAttempt;
import com.careerfit.ai.structured.domain.AiCallExecution;
import com.careerfit.ai.structured.domain.AiCallStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.ObjectMapper;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@DisplayName("Structured Output 공통 실행기 테스트")
class StructuredOutputExecutorTest {

    private static final Instant NOW = Instant.parse("2026-08-03T00:00:00Z");

    @Test
    @DisplayName("검증된 결과와 Provider 메타데이터만 반환한다")
    void 검증된_결과와_메타데이터를_반환한다() {
        LlmResponse response = response("{\"name\":\"backend\"}");
        AtomicBoolean transactionActive = new AtomicBoolean(true);
        Fixture fixture = fixture(request -> {
            transactionActive.set(TransactionSynchronizationManager.isActualTransactionActive());
            return response;
        });

        StructuredOutputResult<String> result = fixture.executor().execute(request());

        assertThat(result.value()).isEqualTo("backend");
        assertThat(result.provider()).isEqualTo("fake");
        assertThat(result.attemptCount()).isEqualTo(1);
        assertThat(result.tokenUsage()).isEqualTo(new TokenUsage(10, 5, 15));
        assertThat(fixture.delays()).isEmpty();
        assertThat(transactionActive).isFalse();
    }

    @Test
    @DisplayName("JSON 파싱 실패는 최대 세 번만 시도하고 backoff를 적용한다")
    void 파싱_실패는_상한_내에서만_재시도한다() {
        AtomicInteger calls = new AtomicInteger();
        Fixture fixture = fixture(request -> {
            calls.incrementAndGet();
            return response("not-json-sensitive-content");
        });

        assertThatThrownBy(() -> fixture.executor().execute(request()))
                .isInstanceOf(StructuredOutputException.class)
                .extracting("failure", "attemptCount")
                .containsExactly(StructuredOutputFailure.RESPONSE_PARSE_FAILED, 3);
        assertThat(calls).hasValue(3);
        assertThat(fixture.delays()).containsExactly(200L, 400L);
    }

    @Test
    @DisplayName("일시 Provider 실패 뒤 성공하면 모든 시도를 같은 실행으로 기록한다")
    void 일시_실패_뒤_성공하면_시도를_연결한다() {
        Queue<Object> outcomes = new ArrayDeque<>();
        outcomes.add(new ProviderException(ProviderErrorType.TIMEOUT, "secret-timeout"));
        outcomes.add(response("{\"name\":\"career\"}"));
        Fixture fixture = fixture(request -> {
            Object outcome = outcomes.remove();
            if (outcome instanceof RuntimeException exception) throw exception;
            return (LlmResponse) outcome;
        });

        StructuredOutputResult<String> result = fixture.executor().execute(request());

        assertThat(result.value()).isEqualTo("career");
        assertThat(result.attemptCount()).isEqualTo(2);
        assertThat(fixture.delays()).containsExactly(200L);
        ArgumentCaptor<AiCallAttempt> attempts = forClass(AiCallAttempt.class);
        verify(fixture.repository(), org.mockito.Mockito.times(4)).saveAttempt(attempts.capture());
        assertThat(attempts.getAllValues()).extracting(AiCallAttempt::attemptNumber)
                .containsExactly(1, 1, 2, 2);
        assertThat(attempts.getAllValues()).extracting(AiCallAttempt::executionId)
                .doesNotContainNull().containsOnly(result.aiCallExecutionId());
    }

    @Test
    @DisplayName("정책 거절은 재시도하지 않고 즉시 실패한다")
    void 정책_거절은_즉시_실패한다() {
        AtomicInteger calls = new AtomicInteger();
        Fixture fixture = fixture(request -> {
            calls.incrementAndGet();
            throw new ProviderException(ProviderErrorType.POLICY_REJECTION, "sensitive-policy");
        });

        assertThatThrownBy(() -> fixture.executor().execute(request()))
                .isInstanceOf(StructuredOutputException.class)
                .extracting("failure", "attemptCount")
                .containsExactly(StructuredOutputFailure.PROVIDER_POLICY_REJECTED, 1);
        assertThat(calls).hasValue(1);
        assertThat(fixture.delays()).isEmpty();
    }

    @Test
    @DisplayName("Provider 설정 오류는 재시도하지 않는다")
    void Provider_설정_오류는_재시도하지_않는다() {
        AtomicInteger calls = new AtomicInteger();
        Fixture fixture = fixture(request -> {
            calls.incrementAndGet();
            throw new ProviderException(ProviderErrorType.CONFIGURATION_ERROR, "secret-api-key");
        });

        assertThatThrownBy(() -> fixture.executor().execute(request()))
                .isInstanceOf(StructuredOutputException.class)
                .extracting("failure", "attemptCount")
                .containsExactly(StructuredOutputFailure.PROVIDER_CONFIGURATION_ERROR, 1);
        assertThat(calls).hasValue(1);
        assertThat(fixture.delays()).isEmpty();
    }

    @Test
    @DisplayName("Schema 검증 실패는 업무 결과로 반환하지 않는다")
    void Schema_검증_실패는_결과로_반환하지_않는다() {
        Fixture fixture = fixture(request -> response("{\"other\":1}"));

        assertThatThrownBy(() -> fixture.executor().execute(request()))
                .isInstanceOf(StructuredOutputException.class)
                .extracting("failure")
                .isEqualTo(StructuredOutputFailure.RESPONSE_SCHEMA_INVALID);
    }

    private Fixture fixture(LlmProviderPort provider) {
        AiCallObservationRepository repository = mock(AiCallObservationRepository.class);
        AiCallObservationPersistence persistence = new AiCallObservationPersistence(repository);
        List<Long> delays = new ArrayList<>();
        StructuredOutputExecutor executor = new StructuredOutputExecutor(
                provider, new ObjectMapper(), persistence,
                new AiRetryPolicy(3, 200, 2.0, 2_000), delays::add,
                Clock.fixed(NOW, ZoneOffset.UTC));
        return new Fixture(executor, repository, delays);
    }

    private StructuredOutputRequest<String> request() {
        return new StructuredOutputRequest<>(
                UUID.randomUUID(), "request-1", "CAREER_CANDIDATE_EXTRACTION",
                "prompt-v1", "schema-v1", "private resume text",
                root -> {
                    if (!root.has("name") || !root.get("name").isString()) {
                        throw new StructuredOutputValidationException("name 필드가 필요합니다.");
                    }
                    return root.get("name").asString();
                });
    }

    private LlmResponse response(String content) {
        return new LlmResponse(content, "fake", "fake-model", "provider-request-1",
                new TokenUsage(10, 5, 15));
    }

    private record Fixture(
            StructuredOutputExecutor executor,
            AiCallObservationRepository repository,
            List<Long> delays) {}
}
