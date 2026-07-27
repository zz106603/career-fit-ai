package com.careerfit.analysis.search.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.careerfit.PostgresIntegrationTest;
import com.careerfit.analysis.search.domain.CareerCandidateSearch;
import com.careerfit.career.application.DirectCareerService;
import com.careerfit.career.domain.CareerExperienceVersion;
import com.careerfit.career.domain.DirectCareerContent;
import com.careerfit.career.search.application.CareerIndexService;
import com.careerfit.career.search.application.CareerSearchDocumentRepository;
import com.careerfit.career.search.application.CareerSearchTextBuilder;
import com.careerfit.career.search.domain.CareerSearchDocument;
import com.careerfit.identity.RequestCurrentUserContext;
import com.careerfit.identity.RequestCurrentUserContext.UserScope;
import com.careerfit.identity.development.DevelopmentUsers;
import com.careerfit.job.application.JobPostingService;
import com.careerfit.job.domain.JobPosting;
import com.careerfit.job.structure.application.JobPostingStructureService;
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
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
@DisplayName("요구사항별 경력 검색 후보 서비스 통합 테스트")
class CareerCandidateSearchServiceIntegrationTest extends PostgresIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-07-27T00:00:00Z");

    @Autowired
    private DirectCareerService directCareerService;

    @Autowired
    private CareerIndexService careerIndexService;

    @Autowired
    private CareerSearchDocumentRepository searchDocumentRepository;

    @Autowired
    private CareerSearchTextBuilder searchTextBuilder;

    @Autowired
    private JobPostingService jobPostingService;

    @Autowired
    private JobPostingStructureService structureService;

    @Autowired
    private CareerCandidateSearchService candidateSearchService;

    @Autowired
    private RequestCurrentUserContext currentUserContext;

    @Autowired
    private JdbcClient jdbcClient;

    @BeforeEach
    void 데이터베이스를_초기화한다() {
        jdbcClient.sql("TRUNCATE career_experience, job_posting CASCADE").update();
    }

    @Test
    @DisplayName("현재 사용자의 INDEXED 확정 경력만 검색한다")
    void 현재_사용자의_INDEXED_확정_경력만_검색한다() {
        CareerExperienceVersion userACareer;
        JobPosting userAJob;
        try (UserScope ignored = currentUserContext.bind(DevelopmentUsers.USER_A)) {
            userACareer = createIndexedCareer("사용자 A Java 경력");
            userAJob = createReadyJob("Java 경험");
        }
        try (UserScope ignored = currentUserContext.bind(DevelopmentUsers.USER_B)) {
            createIndexedCareer("사용자 B Java 경력");
        }

        try (UserScope ignored = currentUserContext.bind(DevelopmentUsers.USER_A)) {
            CareerCandidateSearch search = candidateSearchService.search(userAJob.id());

            assertThat(search.candidates())
                    .singleElement()
                    .extracting(candidate -> candidate.experienceVersionId())
                    .isEqualTo(userACareer.id());
        }
    }

    @Test
    @DisplayName("미확정 PENDING 비활성 경력 버전을 검색에서 제외한다")
    void 미확정_PENDING_비활성_경력_버전을_검색에서_제외한다() {
        try (UserScope ignored = currentUserContext.bind(DevelopmentUsers.USER_A)) {
            directCareerService.create(content("미확정 경력"));
            createPendingCareer("PENDING 경력");
            CareerExperienceVersion inactive = createIndexedCareer("비활성 경력");
            CareerExperienceVersion next =
                    directCareerService.revise(inactive.experienceId(), content("새 현재 버전"));
            directCareerService.confirm(next.experienceId(), next.id());
            CareerExperienceVersion active = createIndexedCareer("활성 경력");
            JobPosting jobPosting = createReadyJob("Java 경험");

            CareerCandidateSearch search = candidateSearchService.search(jobPosting.id());

            assertThat(search.candidates())
                    .singleElement()
                    .extracting(candidate -> candidate.experienceVersionId())
                    .isEqualTo(active.id());
        }
    }

    @Test
    @DisplayName("후보 ID 점수 순위와 검색 버전을 불변 Snapshot으로 저장한다")
    void 후보_ID_점수_순위와_검색_버전을_불변_Snapshot으로_저장한다() {
        try (UserScope ignored = currentUserContext.bind(DevelopmentUsers.USER_A)) {
            createIndexedCareer("Java API 개발");
            createIndexedCareer("Spring 서버 개발");
            JobPosting jobPosting = createReadyJob("Java 경험");

            CareerCandidateSearch created = candidateSearchService.search(jobPosting.id());
            CareerCandidateSearch found = candidateSearchService.find(created.id());

            assertThat(found).isEqualTo(created);
            assertThat(found.searchVersion()).isEqualTo("pgvector-cosine-v1");
            assertThat(found.queryEmbeddingVersion()).isEqualTo("fake-embedding-v1");
            assertThat(found.candidates())
                    .extracting("rank")
                    .containsExactly(1, 2);
            assertThat(found.candidates())
                    .extracting("embeddingVersion")
                    .containsOnly("fake-embedding-v1");
            assertThat(found.candidates().get(0).score())
                    .isGreaterThanOrEqualTo(found.candidates().get(1).score());
            assertThatThrownBy(() -> jdbcClient
                            .sql("""
                                    UPDATE career_search_candidate_snapshot
                                    SET score = 0
                                    WHERE candidate_search_id = :searchId
                                    """)
                            .param("searchId", created.id().value())
                            .update())
                    .isInstanceOf(DataAccessException.class);
        }
    }

    @Test
    @DisplayName("검색 후보가 없어도 빈 결과 Snapshot을 저장한다")
    void 검색_후보가_없어도_빈_결과_Snapshot을_저장한다() {
        try (UserScope ignored = currentUserContext.bind(DevelopmentUsers.USER_A)) {
            JobPosting jobPosting = createReadyJob("존재하지 않는 경력");

            CareerCandidateSearch created = candidateSearchService.search(jobPosting.id());
            CareerCandidateSearch found = candidateSearchService.find(created.id());

            assertThat(created.candidates()).isEmpty();
            assertThat(found.candidates()).isEmpty();
            assertThat(searchCount()).isEqualTo(1);
        }
    }

    @Test
    @DisplayName("다른 사용자는 검색 Snapshot을 조회할 수 없다")
    void 다른_사용자는_검색_Snapshot을_조회할_수_없다() {
        CareerCandidateSearch search;
        try (UserScope ignored = currentUserContext.bind(DevelopmentUsers.USER_A)) {
            JobPosting jobPosting = createReadyJob("Java 경험");
            search = candidateSearchService.search(jobPosting.id());
        }

        try (UserScope ignored = currentUserContext.bind(DevelopmentUsers.USER_B)) {
            assertThatThrownBy(() -> candidateSearchService.find(search.id()))
                    .isInstanceOf(CareerCandidateSearchNotFoundException.class);
        }
    }

    private CareerExperienceVersion createIndexedCareer(String title) {
        CareerExperienceVersion version = createConfirmedCareer(title);
        careerIndexService.index(version.id());
        return version;
    }

    private void createPendingCareer(String title) {
        CareerExperienceVersion version = createConfirmedCareer(title);
        String searchableText = searchTextBuilder.build(version);
        searchDocumentRepository.savePending(CareerSearchDocument.pending(
                DevelopmentUsers.USER_A.userId(),
                version.id(),
                searchableText,
                "a".repeat(64),
                NOW));
    }

    private CareerExperienceVersion createConfirmedCareer(String title) {
        CareerExperienceVersion draft = directCareerService.create(content(title));
        directCareerService.confirm(draft.experienceId(), draft.id());
        return directCareerService.findConfirmed().stream()
                .filter(version -> version.id().equals(draft.id()))
                .findFirst()
                .orElseThrow();
    }

    private JobPosting createReadyJob(String requirementText) {
        JobPosting jobPosting =
                jobPostingService.create("커리어핏", "백엔드 개발자", requirementText);
        structureService.structure(jobPosting.id());
        return jobPosting;
    }

    private DirectCareerContent content(String title) {
        return new DirectCareerContent(title, "커리어핏", "백엔드 개발", "Java API를 개발했다.");
    }

    private int searchCount() {
        return jdbcClient
                .sql("SELECT COUNT(*) FROM career_candidate_search")
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
