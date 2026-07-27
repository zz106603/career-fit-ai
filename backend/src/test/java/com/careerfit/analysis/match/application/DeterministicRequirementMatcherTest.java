package com.careerfit.analysis.match.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.careerfit.analysis.match.domain.RequirementMatchStatus;
import com.careerfit.analysis.search.domain.CareerCandidateSearch;
import com.careerfit.analysis.search.domain.CareerCandidateSearchId;
import com.careerfit.analysis.search.domain.CareerSearchCandidate;
import com.careerfit.career.domain.CareerExperienceId;
import com.careerfit.career.domain.CareerExperienceSourceType;
import com.careerfit.career.domain.CareerExperienceVersion;
import com.careerfit.career.domain.CareerExperienceVersionId;
import com.careerfit.career.domain.DirectCareerContent;
import com.careerfit.identity.UserId;
import com.careerfit.job.structure.domain.JobPostingAnalysisId;
import com.careerfit.job.structure.domain.JobRequirement;
import com.careerfit.job.structure.domain.JobRequirementCategory;
import com.careerfit.job.structure.domain.JobRequirementId;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("결정론적 요구사항 판정 테스트")
class DeterministicRequirementMatcherTest {

    private final DeterministicRequirementMatcher matcher =
            new DeterministicRequirementMatcher();

    @Test
    @DisplayName("모든 핵심 단어가 경력에 있으면 SATISFIED로 판정한다")
    void 모든_핵심_단어가_경력에_있으면_SATISFIED로_판정한다() {
        CareerExperienceVersion version = version("Java Spring", null, null);

        assertThat(match("Java Spring 경험", version))
                .extracting(result -> result.status())
                .isEqualTo(RequirementMatchStatus.SATISFIED);
    }

    @Test
    @DisplayName("일부 핵심 단어만 경력에 있으면 PARTIALLY_SATISFIED로 판정한다")
    void 일부_핵심_단어만_경력에_있으면_PARTIALLY_SATISFIED로_판정한다() {
        CareerExperienceVersion version = version("Java", null, null);

        assertThat(match("Java Kubernetes 경험", version))
                .extracting(result -> result.status())
                .isEqualTo(RequirementMatchStatus.PARTIALLY_SATISFIED);
    }

    @Test
    @DisplayName("확인할 단어 근거가 없으면 UNKNOWN으로 판정한다")
    void 확인할_단어_근거가_없으면_UNKNOWN으로_판정한다() {
        CareerExperienceVersion version = version("Java", null, null);

        assertThat(match("Go 경험", version))
                .satisfies(result -> {
                    assertThat(result.status()).isEqualTo(RequirementMatchStatus.UNKNOWN);
                    assertThat(result.evidence()).isNull();
                });
    }

    @Test
    @DisplayName("요구 연차와 확정 기간이 충돌하면 NOT_SATISFIED로 판정한다")
    void 요구_연차와_확정_기간이_충돌하면_NOT_SATISFIED로_판정한다() {
        CareerExperienceVersion version = version(
                "Java", LocalDate.of(2024, 1, 1), LocalDate.of(2026, 1, 1));

        assertThat(match("Java 5년 이상", version))
                .satisfies(result -> {
                    assertThat(result.status())
                            .isEqualTo(RequirementMatchStatus.NOT_SATISFIED);
                    assertThat(result.evidence().explicitConflict()).isTrue();
                });
    }

    @Test
    @DisplayName("검색 후보가 없으면 UNKNOWN으로 판정한다")
    void 검색_후보가_없으면_UNKNOWN으로_판정한다() {
        JobRequirement requirement = requirement("Java 경험");
        CareerCandidateSearch search = new CareerCandidateSearch(
                CareerCandidateSearchId.newId(),
                userId(),
                requirement.id(),
                "fake-embedding-v1",
                "pgvector-cosine-v1",
                Instant.parse("2026-07-27T00:00:00Z"),
                List.of());

        assertThat(matcher.match(requirement, search, List.of()).status())
                .isEqualTo(RequirementMatchStatus.UNKNOWN);
    }

    private com.careerfit.analysis.match.domain.RequirementMatchResult match(
            String requirementText, CareerExperienceVersion version) {
        JobRequirement requirement = requirement(requirementText);
        CareerSearchCandidate candidate =
                new CareerSearchCandidate(version.id(), 0.8, 1, "fake-embedding-v1");
        CareerCandidateSearch search = new CareerCandidateSearch(
                CareerCandidateSearchId.newId(),
                version.userId(),
                requirement.id(),
                "fake-embedding-v1",
                "pgvector-cosine-v1",
                Instant.parse("2026-07-27T00:00:00Z"),
                List.of(candidate));
        return matcher.match(requirement, search, List.of(version));
    }

    private JobRequirement requirement(String text) {
        return new JobRequirement(
                JobRequirementId.newId(),
                new JobPostingAnalysisId(UUID.randomUUID()),
                JobRequirementCategory.REQUIRED,
                text,
                text,
                1);
    }

    private CareerExperienceVersion version(
            String technologies, LocalDate startDate, LocalDate endDate) {
        UserId userId = userId();
        return new CareerExperienceVersion(
                CareerExperienceVersionId.newId(),
                CareerExperienceId.newId(),
                userId,
                1,
                CareerExperienceSourceType.USER_DIRECT,
                new DirectCareerContent(
                        null,
                        "백엔드 개발",
                        "커리어핏",
                        startDate,
                        endDate,
                        "개발자",
                        "API 개발",
                        null,
                        null,
                        null,
                        technologies),
                Instant.parse("2026-07-27T00:00:00Z"),
                Instant.parse("2026-07-27T00:00:00Z"),
                null,
                null);
    }

    private UserId userId() {
        return new UserId(UUID.randomUUID());
    }
}
