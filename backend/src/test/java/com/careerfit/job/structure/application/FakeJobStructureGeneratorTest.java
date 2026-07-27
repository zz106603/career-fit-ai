package com.careerfit.job.structure.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.careerfit.ai.port.model.LlmResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Fake 채용공고 구조 생성기 테스트")
class FakeJobStructureGeneratorTest {

    @Test
    @DisplayName("동일한 원문은 동일한 요구사항을 만든다")
    void 동일한_원문은_동일한_요구사항을_만든다() {
        FakeJobStructureGenerator generator = new FakeJobStructureGenerator(
                request -> new LlmResponse("valid", "fake-model"));
        String originalText = "\n Java와 Spring 경험\n우대 조건";

        FakeJobStructureResult first = generator.generate(originalText);
        FakeJobStructureResult second = generator.generate(originalText);

        assertThat(first).isEqualTo(second);
        assertThat(first.requirementText()).isEqualTo("Java와 Spring 경험");
        assertThat(originalText).contains(first.sourceExcerpt());
    }

    @Test
    @DisplayName("Fake 응답이 비어 있으면 구조화를 거절한다")
    void Fake_응답이_비어_있으면_구조화를_거절한다() {
        FakeJobStructureGenerator generator = new FakeJobStructureGenerator(
                request -> new LlmResponse(" ", "fake-model"));

        assertThatThrownBy(() -> generator.generate("Java 경험"))
                .isInstanceOf(InvalidJobStructureException.class);
    }
}
