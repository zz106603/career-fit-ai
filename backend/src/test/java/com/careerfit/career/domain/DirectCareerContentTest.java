package com.careerfit.career.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("직접 입력 경력 내용 테스트")
class DirectCareerContentTest {

    @Test
    @DisplayName("경험명과 역할이 있으면 생성한다")
    void 경험명과_역할이_있으면_생성한다() {
        DirectCareerContent content =
                new DirectCareerContent("결제 시스템 개선", "커리어핏", "백엔드 개발", null);

        assertThat(content.title()).isEqualTo("결제 시스템 개선");
        assertThat(content.role()).isEqualTo("백엔드 개발");
    }

    @Test
    @DisplayName("경험명이 비어 있으면 거절한다")
    void 경험명이_비어_있으면_거절한다() {
        assertThatThrownBy(() -> new DirectCareerContent(" ", null, "개발", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("경험명 또는 프로젝트명은 필수입니다.");
    }

    @Test
    @DisplayName("역할과 수행 내용이 모두 비어 있으면 거절한다")
    void 역할과_수행_내용이_모두_비어_있으면_거절한다() {
        assertThatThrownBy(() -> new DirectCareerContent("프로젝트", null, " ", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("역할 또는 수행 내용은 필수입니다.");
    }

    @Test
    @DisplayName("종료일이 시작일보다 빠르면 거절한다")
    void 종료일이_시작일보다_빠르면_거절한다() {
        assertThatThrownBy(() -> new DirectCareerContent(
                        "PROJECT",
                        "프로젝트",
                        null,
                        LocalDate.of(2026, 2, 1),
                        LocalDate.of(2026, 1, 1),
                        "개발",
                        null,
                        null,
                        null,
                        null,
                        null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("시작일은 종료일보다 늦을 수 없습니다.");
    }
}
