package com.careerfit.ai.structured.application;

import com.careerfit.ai.port.model.TokenUsage;
import java.util.UUID;

public record StructuredOutputResult<T>(
        T value,
        UUID aiCallExecutionId,
        String provider,
        String model,
        String promptVersion,
        String schemaVersion,
        int attemptCount,
        TokenUsage tokenUsage) {}
