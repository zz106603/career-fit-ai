package com.careerfit.ai.structured.application;

import java.util.Objects;
import java.util.UUID;

public record StructuredOutputRequest<T>(
        UUID workflowExecutionId,
        String requestId,
        String purpose,
        String promptVersion,
        String schemaVersion,
        String prompt,
        StructuredOutputDecoder<T> decoder) {

    public StructuredOutputRequest {
        Objects.requireNonNull(workflowExecutionId, "Workflow 실행 ID는 필수입니다.");
        requireText(purpose, "호출 목적");
        requireText(promptVersion, "Prompt 버전");
        requireText(schemaVersion, "Schema 버전");
        Objects.requireNonNull(prompt, "Prompt는 필수입니다.");
        Objects.requireNonNull(decoder, "Structured Output decoder는 필수입니다.");
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + "은 필수입니다.");
    }
}
