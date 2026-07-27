package com.careerfit.career.search.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.careerfit.career.domain.CareerExperienceId;
import com.careerfit.career.domain.CareerExperienceSourceType;
import com.careerfit.career.domain.CareerExperienceVersion;
import com.careerfit.career.domain.CareerExperienceVersionId;
import com.careerfit.career.domain.DirectCareerContent;
import com.careerfit.identity.development.DevelopmentUsers;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("경력 검색 문장 생성 테스트")
class CareerSearchTextBuilderTest {

    private final CareerSearchTextBuilder textBuilder = new CareerSearchTextBuilder();

    @Test
    @DisplayName("확정 경력의 검색 관련 필드를 정해진 순서로 조합한다")
    void 확정_경력의_검색_관련_필드를_정해진_순서로_조합한다() {
        DirectCareerContent content = new DirectCareerContent(
                "PROJECT",
                "결제 시스템 개선",
                "커리어핏",
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 6, 30),
                "백엔드 개발",
                "결제 API를 개선했다.",
                "응답이 느렸다.",
                "쿼리를 최적화했다.",
                "응답 시간을 줄였다.",
                "Java, PostgreSQL");
        CareerExperienceVersion version = new CareerExperienceVersion(
                CareerExperienceVersionId.newId(),
                CareerExperienceId.newId(),
                DevelopmentUsers.USER_A.userId(),
                1,
                CareerExperienceSourceType.USER_DIRECT,
                content,
                Instant.parse("2026-07-27T00:00:00Z"),
                Instant.parse("2026-07-27T00:00:00Z"),
                null,
                null);

        String searchableText = textBuilder.build(version);

        assertThat(searchableText)
                .containsSubsequence(
                        "경험명: 결제 시스템 개선",
                        "유형: PROJECT",
                        "조직: 커리어핏",
                        "기간: 2025-01-01 ~ 2025-06-30",
                        "역할: 백엔드 개발",
                        "수행: 결제 API를 개선했다.",
                        "문제: 응답이 느렸다.",
                        "행동: 쿼리를 최적화했다.",
                        "성과: 응답 시간을 줄였다.",
                        "기술: Java, PostgreSQL");
    }
}
