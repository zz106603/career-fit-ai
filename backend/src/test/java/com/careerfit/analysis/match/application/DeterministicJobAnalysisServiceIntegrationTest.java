package com.careerfit.analysis.match.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.careerfit.PostgresIntegrationTest;
import com.careerfit.analysis.match.domain.CareerEvidenceSnapshot;
import com.careerfit.analysis.match.domain.JobAnalysisResult;
import com.careerfit.analysis.match.domain.JobAnalysisResultId;
import com.careerfit.analysis.match.domain.RequirementMatchResult;
import com.careerfit.analysis.match.domain.RequirementMatchStatus;
import com.careerfit.analysis.search.application.CareerCandidateSearchService;
import com.careerfit.analysis.search.domain.CareerCandidateSearch;
import com.careerfit.career.application.DirectCareerService;
import com.careerfit.career.domain.CareerExperienceVersion;
import com.careerfit.career.domain.CareerExperienceVersionId;
import com.careerfit.career.domain.DirectCareerContent;
import com.careerfit.career.search.application.CareerIndexService;
import com.careerfit.identity.RequestCurrentUserContext;
import com.careerfit.identity.RequestCurrentUserContext.UserScope;
import com.careerfit.identity.development.DevelopmentUsers;
import com.careerfit.job.application.JobPostingService;
import com.careerfit.job.domain.JobPosting;
import com.careerfit.job.structure.application.JobPostingStructureService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
@DisplayName("결정론적 공고 분석 서비스 통합 테스트")
class DeterministicJobAnalysisServiceIntegrationTest extends PostgresIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-07-27T00:00:00Z");

    @Autowired
    private DirectCareerService directCareerService;

    @Autowired
    private CareerIndexService careerIndexService;

    @Autowired
    private JobPostingService jobPostingService;

    @Autowired
    private JobPostingStructureService structureService;

    @Autowired
    private CareerCandidateSearchService candidateSearchService;

    @Autowired
    private DeterministicJobAnalysisService analysisService;

    @Autowired
    private JobAnalysisResultRepository resultRepository;

    @Autowired
    private RequestCurrentUserContext currentUserContext;

    @Autowired
    private JdbcClient jdbcClient;

    @BeforeEach
    void 데이터베이스를_초기화한다() {
        jdbcClient.sql("TRUNCATE career_experience, job_posting CASCADE").update();
    }

    @Test
    @DisplayName("판정 이유와 근거 Snapshot을 자동 저장하고 동일하게 재조회한다")
    void 판정_이유와_근거_Snapshot을_자동_저장하고_동일하게_재조회한다() {
        try (UserScope ignored = currentUserContext.bind(DevelopmentUsers.USER_A)) {
            createIndexedCareer("Java Spring");
            CareerCandidateSearch search = createSearch("Java Spring 경험");

            JobAnalysisResult created = analysisService.analyze(search.id());
            JobAnalysisResult found = analysisService.find(created.id());

            assertThat(created.match().status()).isEqualTo(RequirementMatchStatus.SATISFIED);
            assertThat(created.match().evidence()).isNotNull();
            assertThat(found).isEqualTo(created);
        }
    }

    @Test
    @DisplayName("검색 후보가 없으면 UNKNOWN 결과를 자동 저장한다")
    void 검색_후보가_없으면_UNKNOWN_결과를_자동_저장한다() {
        try (UserScope ignored = currentUserContext.bind(DevelopmentUsers.USER_A)) {
            CareerCandidateSearch search = createSearch("Go 경험");

            JobAnalysisResult result = analysisService.analyze(search.id());

            assertThat(result.match().status()).isEqualTo(RequirementMatchStatus.UNKNOWN);
            assertThat(result.match().evidence()).isNull();
            assertThat(analysisService.find(result.id())).isEqualTo(result);
        }
    }

    @Test
    @DisplayName("판정 근거 저장 실패 시 분석 결과 전체를 롤백한다")
    void 판정_근거_저장_실패_시_분석_결과_전체를_롤백한다() {
        try (UserScope ignored = currentUserContext.bind(DevelopmentUsers.USER_A)) {
            createIndexedCareer("Java");
            CareerCandidateSearch search = createSearch("Java 경험");
            JobAnalysisResult valid = analysisService.analyze(search.id());
            JobAnalysisResultId failedId = JobAnalysisResultId.newId();
            CareerEvidenceSnapshot evidence = valid.match().evidence();
            CareerEvidenceSnapshot invalidEvidence = new CareerEvidenceSnapshot(
                    new CareerExperienceVersionId(UUID.randomUUID()),
                    evidence.sourceType(),
                    evidence.title(),
                    evidence.role(),
                    evidence.responsibilities(),
                    evidence.technologies(),
                    evidence.searchScore(),
                    evidence.searchRank(),
                    false);
            RequirementMatchResult invalidMatch = new RequirementMatchResult(
                    valid.match().requirementId(),
                    valid.match().jobPostingAnalysisId(),
                    valid.match().category(),
                    valid.match().requirementText(),
                    valid.match().sourceExcerpt(),
                    valid.match().sequence(),
                    RequirementMatchStatus.SATISFIED,
                    "저장 실패 검증",
                    invalidEvidence);
            JobAnalysisResult invalid = new JobAnalysisResult(
                    failedId,
                    valid.userId(),
                    valid.jobPostingId(),
                    valid.candidateSearchId(),
                    valid.workflowVersion(),
                    NOW,
                    invalidMatch);

            assertThatThrownBy(() -> resultRepository.save(invalid))
                    .isInstanceOf(DataAccessException.class);
            assertThat(resultCount(failedId.value())).isZero();
        }
    }

    @Test
    @DisplayName("다른 사용자는 저장된 분석 결과를 조회할 수 없다")
    void 다른_사용자는_저장된_분석_결과를_조회할_수_없다() {
        JobAnalysisResult result;
        try (UserScope ignored = currentUserContext.bind(DevelopmentUsers.USER_A)) {
            CareerCandidateSearch search = createSearch("Java 경험");
            result = analysisService.analyze(search.id());
        }

        try (UserScope ignored = currentUserContext.bind(DevelopmentUsers.USER_B)) {
            assertThatThrownBy(() -> analysisService.find(result.id()))
                    .isInstanceOf(JobAnalysisResultNotFoundException.class);
        }
    }

    private void createIndexedCareer(String technologies) {
        CareerExperienceVersion draft =
                directCareerService.create(new DirectCareerContent(
                        null,
                        "백엔드 개발",
                        "커리어핏",
                        null,
                        null,
                        "개발자",
                        "API 개발",
                        null,
                        null,
                        null,
                        technologies));
        directCareerService.confirm(draft.experienceId(), draft.id());
        careerIndexService.index(draft.id());
    }

    private CareerCandidateSearch createSearch(String requirementText) {
        JobPosting jobPosting =
                jobPostingService.create("커리어핏", "백엔드 개발자", requirementText);
        structureService.structure(jobPosting.id());
        return candidateSearchService.search(jobPosting.id());
    }

    private int resultCount(UUID resultId) {
        return jdbcClient
                .sql("""
                        SELECT COUNT(*)
                        FROM job_analysis_result
                        WHERE job_analysis_result_id = :resultId
                        """)
                .param("resultId", resultId)
                .query(Integer.class)
                .single();
    }

    @TestConfiguration
    static class FixedClockConfiguration {

        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }
}
