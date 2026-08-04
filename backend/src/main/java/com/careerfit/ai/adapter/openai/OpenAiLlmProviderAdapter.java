package com.careerfit.ai.adapter.openai;

import com.careerfit.ai.port.LlmProviderPort;
import com.careerfit.ai.port.error.ProviderErrorType;
import com.careerfit.ai.port.error.ProviderException;
import com.careerfit.ai.port.model.LlmRequest;
import com.careerfit.ai.port.model.LlmResponse;
import com.careerfit.ai.port.model.TokenUsage;
import java.net.http.HttpTimeoutException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
@ConditionalOnProperty(name = "career-fit.ai.provider", havingValue = "openai")
public class OpenAiLlmProviderAdapter implements LlmProviderPort {

    private final RestClient client;
    private final OpenAiProperties properties;
    private final ObjectMapper objectMapper;

    public OpenAiLlmProviderAdapter(RestClient openAiRestClient, OpenAiProperties properties, ObjectMapper objectMapper) {
        this.client = openAiRestClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public LlmResponse generate(LlmRequest request) {
        try {
            JsonNode response = client.post().uri("/responses")
                    .body(requestBody(request))
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null) throw new ProviderException(ProviderErrorType.INVALID_RESPONSE, "OpenAI 응답이 비어 있습니다.");
            return map(response);
        } catch (RestClientResponseException exception) {
            throw mapHttpError(exception.getStatusCode());
        } catch (ResourceAccessException exception) {
            ProviderErrorType type = exception.getCause() instanceof HttpTimeoutException
                    ? ProviderErrorType.TIMEOUT : ProviderErrorType.PROVIDER_ERROR;
            throw new ProviderException(type, "OpenAI 요청에 실패했습니다.");
        }
    }

    private Map<String, Object> requestBody(LlmRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", properties.model());
        body.put("input", request.prompt());
        body.put("max_output_tokens", properties.maxOutputTokens());
        if (request.structuredOutputRequested()) {
            body.put("text", Map.of("format", Map.of(
                    "type", "json_schema",
                    "name", request.schemaName(),
                    "strict", true,
                    "schema", readSchema(request.schemaJson()))));
        }
        return body;
    }

    private JsonNode readSchema(String schemaJson) {
        try {
            return objectMapper.readTree(schemaJson);
        } catch (RuntimeException exception) {
            throw new ProviderException(ProviderErrorType.CONFIGURATION_ERROR, "Structured Output schema가 올바르지 않습니다.");
        }
    }

    private LlmResponse map(JsonNode response) {
        String content = response.path("output_text").asText(null);
        if (content == null) {
            for (JsonNode output : response.path("output")) {
                for (JsonNode item : output.path("content")) {
                    if ("output_text".equals(item.path("type").asText())) content = item.path("text").asText(null);
                }
            }
        }
        if (content == null) throw new ProviderException(ProviderErrorType.INVALID_RESPONSE, "OpenAI 출력 텍스트가 없습니다.");
        JsonNode usage = response.path("usage");
        Integer input = integerOrNull(usage, "input_tokens");
        Integer output = integerOrNull(usage, "output_tokens");
        Integer total = integerOrNull(usage, "total_tokens");
        return new LlmResponse(content, "openai", response.path("model").asText(properties.model()),
                response.path("id").asText(null), new TokenUsage(input, output, total));
    }

    private Integer integerOrNull(JsonNode node, String field) {
        return node.has(field) && node.path(field).canConvertToInt() ? node.path(field).asInt() : null;
    }

    private ProviderException mapHttpError(HttpStatusCode status) {
        ProviderErrorType type;
        if (status.value() == 401 || status.value() == 403) type = ProviderErrorType.CONFIGURATION_ERROR;
        else if (status.value() == 429) type = ProviderErrorType.RATE_LIMIT;
        else if (status.is5xxServerError()) type = ProviderErrorType.PROVIDER_ERROR;
        else if (status.value() == 400) type = ProviderErrorType.POLICY_REJECTION;
        else type = ProviderErrorType.PROVIDER_ERROR;
        return new ProviderException(type, "OpenAI가 요청을 처리하지 못했습니다.");
    }
}
