package com.careerfit.ai.structured.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AiRetryPolicy {

    private final int maxAttempts;
    private final long initialDelayMillis;
    private final double multiplier;
    private final long maxDelayMillis;

    public AiRetryPolicy(
            @Value("${career-fit.ai.structured-output.retry.max-attempts:3}") int maxAttempts,
            @Value("${career-fit.ai.structured-output.retry.initial-delay-ms:200}") long initialDelayMillis,
            @Value("${career-fit.ai.structured-output.retry.multiplier:2.0}") double multiplier,
            @Value("${career-fit.ai.structured-output.retry.max-delay-ms:2000}") long maxDelayMillis) {
        if (maxAttempts < 1 || initialDelayMillis < 0 || multiplier < 1.0 || maxDelayMillis < 0) {
            throw new IllegalArgumentException("AI 재시도 설정이 올바르지 않습니다.");
        }
        this.maxAttempts = maxAttempts;
        this.initialDelayMillis = initialDelayMillis;
        this.multiplier = multiplier;
        this.maxDelayMillis = maxDelayMillis;
    }

    public int maxAttempts() { return maxAttempts; }

    public long delayMillisAfter(int failedAttemptNumber) {
        double delay = initialDelayMillis * Math.pow(multiplier, failedAttemptNumber - 1);
        return Math.min(maxDelayMillis, (long) delay);
    }
}
