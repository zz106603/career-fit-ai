package com.careerfit.ai.adapter.fake;

import static org.assertj.core.api.Assertions.assertThat;

import com.careerfit.ai.port.EmbeddingProviderPort;
import com.careerfit.ai.port.LlmProviderPort;
import com.careerfit.ai.port.SearchProviderPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

@DisplayName("M0 Fake Provider Bean 테스트")
class FakeProviderBeanTest {

    @Test
    @DisplayName("모든 Provider Port에 Fake Adapter가 연결된다")
    void 모든_Provider_Port에_Fake_Adapter가_연결된다() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
                FakeLlmProviderAdapter.class,
                FakeEmbeddingProviderAdapter.class,
                FakeSearchProviderAdapter.class)) {
            assertThat(context.getBean(LlmProviderPort.class))
                    .isInstanceOf(FakeLlmProviderAdapter.class);
            assertThat(context.getBean(EmbeddingProviderPort.class))
                    .isInstanceOf(FakeEmbeddingProviderAdapter.class);
            assertThat(context.getBean(SearchProviderPort.class))
                    .isInstanceOf(FakeSearchProviderAdapter.class);
        }
    }
}
