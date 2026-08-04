package com.careerfit.ai.adapter.openai;

import static org.assertj.core.api.Assertions.assertThat;

import com.careerfit.ai.port.model.LlmRequest;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@DisplayName("실제 OpenAI LLM Provider smoke test")
class OpenAiLlmProviderSmokeTest {

    private static final String SCHEMA = """
            {
              "type": "object",
              "properties": {
                "value": {"type": "string"}
              },
              "required": ["value"],
              "additionalProperties": false
            }
            """;

    @Test
    @DisplayName("실제 Responses API가 Structured Output과 호출 메타데이터를 반환한다")
    void 실제_Responses_API의_Structured_Output을_확인한다() {
        String apiKey = requiredEnvironment("OPENAI_API_KEY");
        String model = environment("OPENAI_MODEL", "gpt-5.6-luna");
        int maxOutputTokens = Integer.parseInt(environment("OPENAI_MAX_OUTPUT_TOKENS", "500"));
        Duration timeout = Duration.parse(environment("OPENAI_SMOKE_TIMEOUT", "PT30S"));
        ObjectMapper objectMapper = new ObjectMapper();
        RestClient client = RestClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
        OpenAiLlmProviderAdapter adapter = new OpenAiLlmProviderAdapter(
                client, new OpenAiProperties(apiKey, model, timeout, maxOutputTokens), objectMapper);

        var response = adapter.generate(new LlmRequest(
                "Return a JSON object whose value is exactly smoke-ok. Do not add other fields.",
                "openai_smoke_result",
                SCHEMA));

        JsonNode output = objectMapper.readTree(response.content());
        assertThat(output.path("value").asText()).isEqualTo("smoke-ok");
        assertThat(response.provider()).isEqualTo("openai");
        assertThat(response.model()).isNotBlank();
        assertThat(response.providerRequestId()).isNotBlank();
        assertThat(response.tokenUsage()).isNotNull();
        assertThat(response.tokenUsage().totalTokens()).isNotNull().isPositive();

        System.out.printf(
                "OpenAI smoke call verified: provider=%s, model=%s, requestId=%s, "
                        + "inputTokens=%s, outputTokens=%s, totalTokens=%s%n",
                response.provider(), response.model(), response.providerRequestId(),
                response.tokenUsage().inputTokens(), response.tokenUsage().outputTokens(),
                response.tokenUsage().totalTokens());
    }

    private String requiredEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + "가 필요합니다. 실제 키는 테스트 코드에 입력하지 마세요.");
        }
        return value;
    }

    private String environment(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
