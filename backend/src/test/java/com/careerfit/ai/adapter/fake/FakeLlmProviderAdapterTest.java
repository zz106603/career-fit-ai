package com.careerfit.ai.adapter.fake;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.careerfit.ai.port.LlmProviderPort;
import com.careerfit.ai.port.error.ProviderErrorType;
import com.careerfit.ai.port.error.ProviderException;
import com.careerfit.ai.port.model.LlmRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Fake LLM Provider Adapter 테스트")
class FakeLlmProviderAdapterTest {

    private static final LlmRequest 요청 = new LlmRequest("채용공고를 구조화한다");

    @Test
    @DisplayName("동일한 요청에 동일한 응답을 반환한다")
    void 동일한_요청에_동일한_응답을_반환한다() {
        LlmProviderPort 포트 = new FakeLlmProviderAdapter(FakeProviderBehavior.SUCCESS);

        assertThat(포트.generate(요청)).isEqualTo(포트.generate(요청));
    }

    @Test
    @DisplayName("타임아웃을 공통 오류 유형으로 반환한다")
    void 타임아웃을_공통_오류_유형으로_반환한다() {
        LlmProviderPort 포트 = new FakeLlmProviderAdapter(FakeProviderBehavior.TIMEOUT);

        assertThatThrownBy(() -> 포트.generate(요청))
                .isInstanceOf(ProviderException.class)
                .extracting("errorType")
                .isEqualTo(ProviderErrorType.TIMEOUT);
    }

    @Test
    @DisplayName("잘못된 응답을 공통 오류 유형으로 반환한다")
    void 잘못된_응답을_공통_오류_유형으로_반환한다() {
        LlmProviderPort 포트 = new FakeLlmProviderAdapter(FakeProviderBehavior.INVALID_RESPONSE);

        assertThatThrownBy(() -> 포트.generate(요청))
                .isInstanceOf(ProviderException.class)
                .extracting("errorType")
                .isEqualTo(ProviderErrorType.INVALID_RESPONSE);
    }
}
