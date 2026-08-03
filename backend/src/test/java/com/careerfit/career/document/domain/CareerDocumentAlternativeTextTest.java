package com.careerfit.career.document.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.careerfit.identity.UserId;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("경력 문서 대체 텍스트 도메인 테스트")
class CareerDocumentAlternativeTextTest {

    @Test
    @DisplayName("보조 문자를 포함한 텍스트 길이는 Unicode 문자 수로 검증한다")
    void 보조_문자를_Unicode_문자_수로_검증한다() {
        CareerDocumentAlternativeText text = new CareerDocumentAlternativeText(
                CareerDocumentAnalysisId.newId(), new CareerDocumentId(UUID.randomUUID()),
                new UserId(UUID.randomUUID()), "경력🚀", 3, "a".repeat(64), Instant.now());

        assertThat(text.textLength()).isEqualTo(3);
    }

    @Test
    @DisplayName("CR 줄바꿈과 공백 텍스트는 Snapshot으로 생성할 수 없다")
    void 정규화되지_않은_텍스트는_거부한다() {
        assertThatThrownBy(() -> new CareerDocumentAlternativeText(
                CareerDocumentAnalysisId.newId(), new CareerDocumentId(UUID.randomUUID()),
                new UserId(UUID.randomUUID()), "text\r\n", 6, "a".repeat(64), Instant.now()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CareerDocumentAlternativeText(
                CareerDocumentAnalysisId.newId(), new CareerDocumentId(UUID.randomUUID()),
                new UserId(UUID.randomUUID()), "  ", 2, "a".repeat(64), Instant.now()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
