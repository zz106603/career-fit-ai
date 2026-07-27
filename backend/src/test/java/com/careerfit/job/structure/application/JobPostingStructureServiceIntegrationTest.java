package com.careerfit.job.structure.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.careerfit.PostgresIntegrationTest;
import com.careerfit.identity.RequestCurrentUserContext;
import com.careerfit.identity.RequestCurrentUserContext.UserScope;
import com.careerfit.identity.development.DevelopmentUsers;
import com.careerfit.job.application.JobPostingNotFoundException;
import com.careerfit.job.application.JobPostingService;
import com.careerfit.job.domain.JobPosting;
import com.careerfit.job.structure.domain.JobPostingAnalysis;
import com.careerfit.job.structure.domain.JobPostingAnalysisStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
@DisplayName("채용공고 Fake 구조화 서비스 통합 테스트")
class JobPostingStructureServiceIntegrationTest extends PostgresIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-07-27T00:00:00Z");

    @Autowired
    private JobPostingService jobPostingService;

    @Autowired
    private JobPostingStructureService structureService;

    @Autowired
    private RequestCurrentUserContext currentUserContext;

    @Autowired
    private JdbcClient jdbcClient;

    @BeforeEach
    void 데이터베이스를_초기화한다() {
        jdbcClient.sql("TRUNCATE job_posting CASCADE").update();
    }

    @Test
    @DisplayName("Fake 요구사항 한 건을 저장하고 READY로 전환한다")
    void Fake_요구사항_한_건을_저장하고_READY로_전환한다() {
        try (UserScope ignored = currentUserContext.bind(DevelopmentUsers.USER_A)) {
            JobPosting jobPosting = jobPostingService.create(
                    "커리어핏", "백엔드 개발자", "Java와 Spring 경험\n우대: PostgreSQL");

            JobPostingAnalysis ready = structureService.structure(jobPosting.id());
            JobPostingAnalysis found = structureService.findLatestReady(jobPosting.id());

            assertThat(ready.status()).isEqualTo(JobPostingAnalysisStatus.READY);
            assertThat(found.requirement().text()).isEqualTo("Java와 Spring 경험");
            assertThat(found.requirement().sourceExcerpt()).isEqualTo("Java와 Spring 경험");
            assertThat(requirementCount(found.id().value())).isEqualTo(1);
        }
    }

    @Test
    @DisplayName("구조화 이후에도 채용공고 원문은 변경되지 않는다")
    void 구조화_이후에도_채용공고_원문은_변경되지_않는다() {
        String originalText = "  Java와 Spring 경험\n우대: PostgreSQL  ";
        try (UserScope ignored = currentUserContext.bind(DevelopmentUsers.USER_A)) {
            JobPosting jobPosting =
                    jobPostingService.create("커리어핏", "백엔드 개발자", originalText);

            structureService.structure(jobPosting.id());

            assertThat(jobPostingService.find(jobPosting.id()).originalText())
                    .isEqualTo(originalText);
        }
    }

    @Test
    @DisplayName("동일한 원문은 반복 실행해도 동일한 요구사항을 만든다")
    void 동일한_원문은_반복_실행해도_동일한_요구사항을_만든다() {
        try (UserScope ignored = currentUserContext.bind(DevelopmentUsers.USER_A)) {
            JobPosting jobPosting =
                    jobPostingService.create("커리어핏", "개발자", "Java 경험\nSpring 경험");

            JobPostingAnalysis first = structureService.structure(jobPosting.id());
            JobPostingAnalysis second = structureService.structure(jobPosting.id());

            assertThat(first.requirement().text()).isEqualTo(second.requirement().text());
            assertThat(analysisCount(jobPosting.id().value())).isEqualTo(2);
        }
    }

    @Test
    @DisplayName("다른 사용자는 채용공고를 구조화하거나 결과를 조회할 수 없다")
    void 다른_사용자는_채용공고를_구조화하거나_결과를_조회할_수_없다() {
        JobPosting jobPosting;
        try (UserScope ignored = currentUserContext.bind(DevelopmentUsers.USER_A)) {
            jobPosting = jobPostingService.create("커리어핏", "개발자", "Java 경험");
            structureService.structure(jobPosting.id());
        }

        try (UserScope ignored = currentUserContext.bind(DevelopmentUsers.USER_B)) {
            assertThatThrownBy(() -> structureService.structure(jobPosting.id()))
                    .isInstanceOf(JobPostingNotFoundException.class);
            assertThatThrownBy(() -> structureService.findLatestReady(jobPosting.id()))
                    .isInstanceOf(JobPostingNotFoundException.class);
        }
    }

    private int requirementCount(java.util.UUID analysisId) {
        return jdbcClient
                .sql("""
                        SELECT COUNT(*)
                        FROM job_requirement
                        WHERE job_posting_analysis_id = :analysisId
                        """)
                .param("analysisId", analysisId)
                .query(Integer.class)
                .single();
    }

    private int analysisCount(java.util.UUID jobPostingId) {
        return jdbcClient
                .sql("""
                        SELECT COUNT(*)
                        FROM job_posting_analysis
                        WHERE job_posting_id = :jobPostingId
                        """)
                .param("jobPostingId", jobPostingId)
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
