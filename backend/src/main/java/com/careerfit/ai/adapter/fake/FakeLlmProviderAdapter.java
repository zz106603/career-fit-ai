package com.careerfit.ai.adapter.fake;

import com.careerfit.ai.port.LlmProviderPort;
import com.careerfit.ai.port.model.LlmRequest;
import com.careerfit.ai.port.model.LlmResponse;
import com.careerfit.ai.port.model.TokenUsage;
import java.util.Objects;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "career-fit.ai.provider", havingValue = "fake", matchIfMissing = true)
public final class FakeLlmProviderAdapter implements LlmProviderPort {

    private static final String MODEL = "fake-llm-v1";

    private final FakeProviderBehavior behavior;

    public FakeLlmProviderAdapter() {
        this(FakeProviderBehavior.SUCCESS);
    }

    public FakeLlmProviderAdapter(FakeProviderBehavior behavior) {
        this.behavior = Objects.requireNonNull(behavior, "behavior는 null일 수 없습니다.");
    }

    @Override
    public LlmResponse generate(LlmRequest request) {
        Objects.requireNonNull(request, "request는 null일 수 없습니다.");
        FakeProviderSupport.verifyBehavior(behavior);
        String content = "career_candidates".equals(request.schemaName())
                ? careerCandidates(request.prompt())
                : "{\"value\":\"fake-response-" + FakeProviderSupport.identifier(request.prompt()) + "\"}";
        return new LlmResponse(
                content,
                "fake", MODEL, "fake-request-" + FakeProviderSupport.identifier(request.prompt()),
                new TokenUsage(10, 5, 15));
    }

    private String careerCandidates(String prompt) {
        String marker = "[PAGE ";
        int markerStart = prompt.indexOf(marker);
        if (markerStart < 0) return "{\"candidates\":[]}";
        int numberEnd = prompt.indexOf(']', markerStart);
        int textStart = prompt.indexOf('\n', numberEnd) + 1;
        int textEnd = prompt.indexOf("\n[PAGE ", textStart);
        String text = prompt.substring(textStart, textEnd < 0 ? prompt.length() : textEnd).trim();
        if (text.isBlank()) return "{\"candidates\":[]}";
        int page = Integer.parseInt(prompt.substring(markerStart + marker.length(), numberEnd));
        String escaped = text.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\r", "\\r").replace("\n", "\\n");
        return "{\"candidates\":[{\"candidateType\":\"EXPERIENCE\","
                + "\"organization\":null,\"role\":null,\"period\":null,"
                + "\"description\":\"" + escaped + "\",\"pageNumber\":" + page
                + ",\"excerpt\":\"" + escaped + "\"}]}";
    }
}
