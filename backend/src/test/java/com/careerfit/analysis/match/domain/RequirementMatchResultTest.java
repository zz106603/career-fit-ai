package com.careerfit.analysis.match.domain;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.careerfit.job.structure.domain.JobPostingAnalysisId;
import com.careerfit.job.structure.domain.JobRequirementCategory;
import com.careerfit.job.structure.domain.JobRequirementId;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("요구사항 판정 결과 도메인 테스트")
class RequirementMatchResultTest {

    @Test
    @DisplayName("근거 없는 SATISFIED를 생성할 수 없다")
    void 근거_없는_SATISFIED를_생성할_수_없다() {
        assertThatThrownBy(() -> result(RequirementMatchStatus.SATISFIED, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("명시적 충돌 근거 없는 NOT_SATISFIED를 생성할 수 없다")
    void 명시적_충돌_근거_없는_NOT_SATISFIED를_생성할_수_없다() {
        CareerEvidenceSnapshot evidence = new CareerEvidenceSnapshot(
                new com.careerfit.career.domain.CareerExperienceVersionId(UUID.randomUUID()),
                com.careerfit.career.domain.CareerExperienceSourceType.USER_DIRECT,
                "경력",
                "개발자",
                "API 개발",
                "Java",
                0.8,
                1,
                false);

        assertThatThrownBy(() -> result(RequirementMatchStatus.NOT_SATISFIED, evidence))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private RequirementMatchResult result(
            RequirementMatchStatus status, CareerEvidenceSnapshot evidence) {
        return new RequirementMatchResult(
                new JobRequirementId(UUID.randomUUID()),
                new JobPostingAnalysisId(UUID.randomUUID()),
                JobRequirementCategory.REQUIRED,
                "Java 경험",
                "Java 경험",
                1,
                status,
                "판정 이유",
                evidence);
    }
}
