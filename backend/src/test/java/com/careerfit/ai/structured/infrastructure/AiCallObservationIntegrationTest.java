package com.careerfit.ai.structured.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.careerfit.PostgresIntegrationTest;
import com.careerfit.ai.structured.application.AiCallObservationPersistence;
import com.careerfit.ai.structured.domain.AiCallAttempt;
import com.careerfit.ai.structured.domain.AiCallExecution;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest(properties = "career-fit.test.ai-call-observation=true")
@DisplayName("AI 호출 관측 저장 통합 테스트")
class AiCallObservationIntegrationTest extends PostgresIntegrationTest {

    private static final Instant STARTED = Instant.parse("2026-08-03T00:00:00Z");

    @Autowired
    private AiCallObservationPersistence persistence;

    @Autowired
    private JdbcClient jdbcClient;

    @BeforeEach
    void 관측_데이터를_초기화한다() {
        jdbcClient.sql("TRUNCATE ai_call_execution CASCADE").update();
    }

    @Test
    @DisplayName("논리 실행과 시도를 연결하고 원문 대신 길이와 checksum을 저장한다")
    void 실행과_시도_메타데이터를_저장한다() {
        AiCallExecution execution = AiCallExecution.start(
                UUID.randomUUID(), "request-1", "CAREER_CANDIDATE_EXTRACTION",
                "prompt-v1", "schema-v1", STARTED);
        String promptChecksum = "a".repeat(64);
        String responseChecksum = "b".repeat(64);
        AiCallAttempt attempt = AiCallAttempt.start(
                execution.id(), 1, STARTED, 120, promptChecksum);

        persistence.saveExecution(execution);
        persistence.saveAttempt(attempt);
        persistence.saveAttempt(attempt.succeed(
                "fake", "fake-model", "provider-request-1",
                10, 5, 15, 80, responseChecksum, STARTED.plusMillis(250)));
        persistence.saveExecution(execution.succeed(1, STARTED.plusMillis(300)));

        Map<String, Object> storedExecution = jdbcClient.sql("""
                        SELECT workflow_execution_id, status, attempt_count, failure_code
                        FROM ai_call_execution
                        WHERE ai_call_execution_id = :id
                        """)
                .param("id", execution.id()).query().singleRow();
        assertThat(storedExecution.get("status")).isEqualTo("SUCCEEDED");
        assertThat(storedExecution.get("attempt_count")).isEqualTo(1);

        Map<String, Object> storedAttempt = jdbcClient.sql("""
                        SELECT provider, model, input_tokens, output_tokens, total_tokens,
                               prompt_length, prompt_checksum_sha256,
                               response_length, response_checksum_sha256
                        FROM ai_call_attempt
                        WHERE ai_call_execution_id = :id AND attempt_number = 1
                        """)
                .param("id", execution.id()).query().singleRow();
        assertThat(storedAttempt).containsEntry("provider", "fake")
                .containsEntry("model", "fake-model")
                .containsEntry("input_tokens", 10)
                .containsEntry("output_tokens", 5)
                .containsEntry("total_tokens", 15)
                .containsEntry("prompt_length", 120)
                .containsEntry("prompt_checksum_sha256", promptChecksum)
                .containsEntry("response_length", 80)
                .containsEntry("response_checksum_sha256", responseChecksum);
    }

    @Test
    @DisplayName("관측 테이블은 Prompt와 Response 원문 컬럼을 갖지 않는다")
    void 관측_테이블에_원문_컬럼이_없다() {
        assertThat(jdbcClient.sql("""
                        SELECT column_name
                        FROM information_schema.columns
                        WHERE table_schema = current_schema()
                          AND table_name IN ('ai_call_execution', 'ai_call_attempt')
                        """).query(String.class).list())
                .doesNotContain("prompt", "response", "content");
    }
}
