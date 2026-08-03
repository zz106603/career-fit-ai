package com.careerfit.ai.adapter.fake;

import com.careerfit.ai.port.LlmProviderPort;
import com.careerfit.ai.port.model.LlmRequest;
import com.careerfit.ai.port.model.LlmResponse;
import com.careerfit.ai.port.model.TokenUsage;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
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
        return new LlmResponse(
                "{\"value\":\"fake-response-" + FakeProviderSupport.identifier(request.prompt()) + "\"}",
                "fake", MODEL, "fake-request-" + FakeProviderSupport.identifier(request.prompt()),
                new TokenUsage(10, 5, 15));
    }
}
