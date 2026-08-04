package com.careerfit.ai.adapter.openai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withTooManyRequests;

import com.careerfit.ai.port.error.ProviderErrorType;
import com.careerfit.ai.port.error.ProviderException;
import com.careerfit.ai.port.model.LlmRequest;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

@DisplayName("OpenAI LLM Provider Adapter 테스트")
class OpenAiLlmProviderAdapterTest {

    @Test
    @DisplayName("Structured Output 요청과 사용량 응답을 공통 모델로 변환한다")
    void 구조화_요청과_응답을_공통_모델로_변환한다() {
        Fixture fixture = fixture();
        fixture.server.expect(requestTo("https://api.openai.com/v1/responses"))
                .andExpect(header("Authorization", "Bearer secret"))
                .andExpect(jsonPath("$.model").value("test-model"))
                .andExpect(jsonPath("$.text.format.type").value("json_schema"))
                .andRespond(withSuccess("""
                        {"id":"resp_1","model":"test-model","output_text":"{\\"value\\":\\"ok\\"}",
                         "usage":{"input_tokens":10,"output_tokens":5,"total_tokens":15}}
                        """, MediaType.APPLICATION_JSON));

        var response = fixture.adapter.generate(new LlmRequest(
                "prompt", "result", "{\"type\":\"object\"}"));

        assertThat(response.content()).isEqualTo("{\"value\":\"ok\"}");
        assertThat(response.providerRequestId()).isEqualTo("resp_1");
        assertThat(response.tokenUsage().totalTokens()).isEqualTo(15);
        fixture.server.verify();
    }

    @Test
    @DisplayName("호출 제한 응답을 재시도 가능한 공통 오류로 변환한다")
    void 호출_제한을_공통_오류로_변환한다() {
        Fixture fixture = fixture();
        fixture.server.expect(requestTo("https://api.openai.com/v1/responses"))
                .andRespond(withTooManyRequests());

        assertThatThrownBy(() -> fixture.adapter.generate(new LlmRequest("prompt")))
                .isInstanceOfSatisfying(ProviderException.class,
                        exception -> assertThat(exception.errorType()).isEqualTo(ProviderErrorType.RATE_LIMIT));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("오류_응답")
    @DisplayName("OpenAI HTTP 오류를 공통 오류 유형으로 변환한다")
    void OpenAI_HTTP_오류를_공통_유형으로_변환한다(
            String name,
            org.springframework.test.web.client.ResponseCreator response,
            ProviderErrorType expected) {
        Fixture fixture = fixture();
        fixture.server.expect(requestTo("https://api.openai.com/v1/responses"))
                .andRespond(response);

        assertThatThrownBy(() -> fixture.adapter.generate(new LlmRequest("prompt")))
                .isInstanceOfSatisfying(ProviderException.class,
                        exception -> assertThat(exception.errorType()).isEqualTo(expected));
    }

    static java.util.stream.Stream<org.junit.jupiter.params.provider.Arguments> 오류_응답() {
        return java.util.stream.Stream.of(
                org.junit.jupiter.params.provider.Arguments.of(
                        "인증 오류", withUnauthorizedRequest(), ProviderErrorType.CONFIGURATION_ERROR),
                org.junit.jupiter.params.provider.Arguments.of(
                        "잘못된 요청", withBadRequest(), ProviderErrorType.POLICY_REJECTION),
                org.junit.jupiter.params.provider.Arguments.of(
                        "Provider 장애", withServerError(), ProviderErrorType.PROVIDER_ERROR));
    }

    private Fixture fixture() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.openai.com/v1")
                .defaultHeader("Authorization", "Bearer secret");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OpenAiProperties properties = new OpenAiProperties("secret", "test-model", Duration.ofSeconds(5), 100);
        return new Fixture(new OpenAiLlmProviderAdapter(builder.build(), properties, new ObjectMapper()), server);
    }

    private record Fixture(OpenAiLlmProviderAdapter adapter, MockRestServiceServer server) {}
}
