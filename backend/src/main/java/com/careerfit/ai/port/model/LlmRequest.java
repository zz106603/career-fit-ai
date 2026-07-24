package com.careerfit.ai.port.model;

import java.util.Objects;

public record LlmRequest(String prompt) {

    public LlmRequest {
        Objects.requireNonNull(prompt, "prompt은 null일 수 없습니다.");
    }
}
