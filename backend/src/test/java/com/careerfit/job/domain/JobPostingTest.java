package com.careerfit.job.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.careerfit.identity.UserId;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("채용공고 도메인 테스트")
class JobPostingTest {

    @Test
    @DisplayName("본문의 앞뒤 공백과 줄바꿈을 원문 그대로 보존한다")
    void 본문의_앞뒤_공백과_줄바꿈을_원문_그대로_보존한다() {
        String originalText = "  주요 업무\n- API 개발\n  ";

        JobPosting jobPosting = jobPosting(originalText);

        assertThat(jobPosting.originalText()).isEqualTo(originalText);
    }

    @Test
    @DisplayName("본문이 비어 있으면 생성할 수 없다")
    void 본문이_비어_있으면_생성할_수_없다() {
        assertThatThrownBy(() -> jobPosting(" \n\t "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("채용공고 본문은 필수입니다.");
    }

    private JobPosting jobPosting(String originalText) {
        return new JobPosting(
                JobPostingId.newId(),
                new UserId(UUID.randomUUID()),
                originalText,
                "백엔드 개발자",
                "커리어핏",
                Instant.parse("2026-07-27T00:00:00Z"),
                null);
    }
}
