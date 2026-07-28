package com.careerfit.common.async.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.careerfit.common.async.domain.JobExecution;
import com.careerfit.common.async.domain.JobType;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("작업 Handler Registry 테스트")
class JobHandlerRegistryTest {

    @Test
    @DisplayName("작업 유형에 맞는 Handler를 반환한다")
    void 작업_유형에_맞는_Handler를_반환한다() {
        JobHandler handler = handler(JobType.CAREER_DOCUMENT_EXTRACTION);
        JobHandlerRegistry registry = new JobHandlerRegistry(List.of(handler));

        assertThat(registry.resolve(JobType.CAREER_DOCUMENT_EXTRACTION))
                .isSameAs(handler);
    }

    @Test
    @DisplayName("같은 작업 유형의 Handler를 중복 등록할 수 없다")
    void 같은_작업_유형의_Handler를_중복_등록할_수_없다() {
        JobHandler first = handler(JobType.CAREER_DOCUMENT_EXTRACTION);
        JobHandler second = handler(JobType.CAREER_DOCUMENT_EXTRACTION);

        assertThatThrownBy(() -> new JobHandlerRegistry(List.of(first, second)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("하나만 등록");
    }

    @Test
    @DisplayName("등록되지 않은 작업 유형은 명시적인 실패 코드로 거절한다")
    void 등록되지_않은_작업_유형은_명시적인_실패_코드로_거절한다() {
        JobHandlerRegistry registry = new JobHandlerRegistry(List.of());

        assertThatThrownBy(() -> registry.resolve(JobType.JOB_ANALYSIS))
                .isInstanceOfSatisfying(JobHandlerException.class, exception ->
                        assertThat(exception.failureCode()).isEqualTo("HANDLER_NOT_FOUND"));
    }

    private JobHandler handler(JobType type) {
        return new JobHandler() {
            @Override
            public JobType type() {
                return type;
            }

            @Override
            public void handle(JobExecution execution) {}
        };
    }
}
