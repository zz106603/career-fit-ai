package com.careerfit.ai.adapter.openai;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("career-fit.ai.openai")
public record OpenAiProperties(String apiKey, String model, Duration timeout, int maxOutputTokens) {

    public OpenAiProperties {
        if (apiKey == null || apiKey.isBlank()) throw new IllegalArgumentException("OPENAI_API_KEY는 필수입니다.");
        if (model == null || model.isBlank()) throw new IllegalArgumentException("OPENAI_MODEL은 필수입니다.");
        if (timeout == null || timeout.isNegative() || timeout.isZero()) {
            throw new IllegalArgumentException("OpenAI timeout은 0보다 커야 합니다.");
        }
        if (maxOutputTokens <= 0) throw new IllegalArgumentException("최대 출력 토큰은 0보다 커야 합니다.");
    }
}
