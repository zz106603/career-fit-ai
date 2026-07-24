package com.careerfit.common.logging;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.spi.LoggingEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("민감 로그 마스킹 테스트")
class SensitiveLogSanitizerTest {

    @Test
    @DisplayName("자격정보와 토큰을 마스킹한다")
    void 자격정보와_토큰을_마스킹한다() {
        String message = "password=hunter2 token=secret-token Authorization:Bearer abc.def.ghi";

        String sanitized = SensitiveLogSanitizer.sanitize(message);

        assertThat(sanitized)
                .doesNotContain("hunter2", "secret-token", "abc.def.ghi")
                .contains("password=***", "token=***", "Authorization:***");
    }

    @Test
    @DisplayName("사용자 원문과 LLM 요청 응답 필드를 마스킹한다")
    void 사용자_원문과_LLM_요청_응답_필드를_마스킹한다() {
        String message = "{\"documentText\":\"private career text\","
                + "\"prompt\":\"private prompt\",\"responseBody\":\"private response\"}";

        String sanitized = SensitiveLogSanitizer.sanitize(message);

        assertThat(sanitized)
                .doesNotContain("private career text", "private prompt", "private response")
                .containsOnlyOnce("\"documentText\":\"***\"")
                .containsOnlyOnce("\"prompt\":\"***\"")
                .containsOnlyOnce("\"responseBody\":\"***\"");
    }

    @Test
    @DisplayName("로그 메시지 변환기가 민감정보를 제거하고 메타데이터는 유지한다")
    void 로그_메시지_변환기가_민감정보를_제거하고_메타데이터는_유지한다() {
        LoggingEvent event = new LoggingEvent();
        event.setMessage("runId=run-123 apiKey=private-key status=SUCCEEDED");

        String converted = new MaskingMessageConverter().convert(event);

        assertThat(converted)
                .doesNotContain("private-key")
                .contains("runId=run-123", "apiKey=***", "status=SUCCEEDED");
    }
}
