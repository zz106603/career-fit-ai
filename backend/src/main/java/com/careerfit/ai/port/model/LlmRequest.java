package com.careerfit.ai.port.model;

import java.util.Objects;

public record LlmRequest(String prompt, String schemaName, String schemaJson) {

    public LlmRequest {
        Objects.requireNonNull(prompt, "prompt은 null일 수 없습니다.");
    }

    public LlmRequest(String prompt) {
        this(prompt, null, null);
    }

    public boolean structuredOutputRequested() {
        return schemaName != null && !schemaName.isBlank() && schemaJson != null && !schemaJson.isBlank();
    }
}
