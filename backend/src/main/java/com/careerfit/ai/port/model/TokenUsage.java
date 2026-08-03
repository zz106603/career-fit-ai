package com.careerfit.ai.port.model;

public record TokenUsage(Integer inputTokens, Integer outputTokens, Integer totalTokens) {

    public TokenUsage {
        requireNonNegative(inputTokens);
        requireNonNegative(outputTokens);
        requireNonNegative(totalTokens);
    }

    public static TokenUsage unknown() {
        return new TokenUsage(null, null, null);
    }

    private static void requireNonNegative(Integer value) {
        if (value != null && value < 0) {
            throw new IllegalArgumentException("토큰 수는 0 이상이어야 합니다.");
        }
    }
}
