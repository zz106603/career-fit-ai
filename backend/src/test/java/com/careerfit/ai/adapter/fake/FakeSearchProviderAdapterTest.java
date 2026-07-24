package com.careerfit.ai.adapter.fake;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.careerfit.ai.port.SearchProviderPort;
import com.careerfit.ai.port.error.ProviderErrorType;
import com.careerfit.ai.port.error.ProviderException;
import com.careerfit.ai.port.model.SearchRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Fake Search Provider Adapter 테스트")
class FakeSearchProviderAdapterTest {

    private static final SearchRequest REQUEST = new SearchRequest("커리어핏 공식 채용 정보");

    @Test
    @DisplayName("동일한 요청에 동일한 검색 결과를 반환한다")
    void 동일한_요청에_동일한_검색_결과를_반환한다() {
        SearchProviderPort port = new FakeSearchProviderAdapter(FakeProviderBehavior.SUCCESS);

        assertThat(port.search(REQUEST)).isEqualTo(port.search(REQUEST));
    }

    @Test
    @DisplayName("타임아웃을 공통 오류 유형으로 반환한다")
    void 타임아웃을_공통_오류_유형으로_반환한다() {
        SearchProviderPort port = new FakeSearchProviderAdapter(FakeProviderBehavior.TIMEOUT);

        assertThatThrownBy(() -> port.search(REQUEST))
                .isInstanceOf(ProviderException.class)
                .extracting("errorType")
                .isEqualTo(ProviderErrorType.TIMEOUT);
    }

    @Test
    @DisplayName("잘못된 응답을 공통 오류 유형으로 반환한다")
    void 잘못된_응답을_공통_오류_유형으로_반환한다() {
        SearchProviderPort port =
                new FakeSearchProviderAdapter(FakeProviderBehavior.INVALID_RESPONSE);

        assertThatThrownBy(() -> port.search(REQUEST))
                .isInstanceOf(ProviderException.class)
                .extracting("errorType")
                .isEqualTo(ProviderErrorType.INVALID_RESPONSE);
    }
}
