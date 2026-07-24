package com.careerfit.ai.adapter.fake;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.careerfit.ai.port.EmbeddingProviderPort;
import com.careerfit.ai.port.error.ProviderErrorType;
import com.careerfit.ai.port.error.ProviderException;
import com.careerfit.ai.port.model.EmbeddingRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Fake Embedding Provider Adapter 테스트")
class FakeEmbeddingProviderAdapterTest {

    private static final EmbeddingRequest REQUEST = new EmbeddingRequest("백엔드 개발 경력");

    @Test
    @DisplayName("동일한 요청에 동일한 8차원 벡터를 반환한다")
    void 동일한_요청에_동일한_8차원_벡터를_반환한다() {
        EmbeddingProviderPort port =
                new FakeEmbeddingProviderAdapter(FakeProviderBehavior.SUCCESS);

        assertThat(port.embed(REQUEST)).isEqualTo(port.embed(REQUEST));
        assertThat(port.embed(REQUEST).vector()).hasSize(8);
    }

    @Test
    @DisplayName("타임아웃을 공통 오류 유형으로 반환한다")
    void 타임아웃을_공통_오류_유형으로_반환한다() {
        EmbeddingProviderPort port =
                new FakeEmbeddingProviderAdapter(FakeProviderBehavior.TIMEOUT);

        assertThatThrownBy(() -> port.embed(REQUEST))
                .isInstanceOf(ProviderException.class)
                .extracting("errorType")
                .isEqualTo(ProviderErrorType.TIMEOUT);
    }

    @Test
    @DisplayName("잘못된 응답을 공통 오류 유형으로 반환한다")
    void 잘못된_응답을_공통_오류_유형으로_반환한다() {
        EmbeddingProviderPort port =
                new FakeEmbeddingProviderAdapter(FakeProviderBehavior.INVALID_RESPONSE);

        assertThatThrownBy(() -> port.embed(REQUEST))
                .isInstanceOf(ProviderException.class)
                .extracting("errorType")
                .isEqualTo(ProviderErrorType.INVALID_RESPONSE);
    }
}
