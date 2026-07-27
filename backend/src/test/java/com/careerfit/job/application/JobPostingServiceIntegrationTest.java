package com.careerfit.job.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.careerfit.PostgresIntegrationTest;
import com.careerfit.identity.RequestCurrentUserContext;
import com.careerfit.identity.RequestCurrentUserContext.UserScope;
import com.careerfit.identity.development.DevelopmentUsers;
import com.careerfit.job.domain.JobPosting;
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
@DisplayName("채용공고 서비스 통합 테스트")
class JobPostingServiceIntegrationTest extends PostgresIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-07-27T00:00:00Z");

    @Autowired
    private JobPostingService service;

    @Autowired
    private RequestCurrentUserContext currentUserContext;

    @Autowired
    private JdbcClient jdbcClient;

    @BeforeEach
    void 데이터베이스를_초기화한다() {
        jdbcClient.sql("TRUNCATE job_posting").update();
    }

    @Test
    @DisplayName("회사 단서와 제목과 본문을 현재 사용자 소유로 등록한다")
    void 회사_단서와_제목과_본문을_현재_사용자_소유로_등록한다() {
        try (UserScope ignored = currentUserContext.bind(DevelopmentUsers.USER_A)) {
            JobPosting created =
                    service.create("커리어핏", "백엔드 개발자", "주요 업무\n- API 개발");

            JobPosting found = service.find(created.id());

            assertThat(found.userId()).isEqualTo(DevelopmentUsers.USER_A.userId());
            assertThat(found.companyHint()).isEqualTo("커리어핏");
            assertThat(found.titleHint()).isEqualTo("백엔드 개발자");
            assertThat(found.originalText()).isEqualTo("주요 업무\n- API 개발");
            assertThat(found.registeredAt()).isEqualTo(NOW);
        }
    }

    @Test
    @DisplayName("빈 본문은 저장하지 않는다")
    void 빈_본문은_저장하지_않는다() {
        try (UserScope ignored = currentUserContext.bind(DevelopmentUsers.USER_A)) {
            assertThatThrownBy(() -> service.create("커리어핏", "개발자", " \n "))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        assertThat(rowCount()).isZero();
    }

    @Test
    @DisplayName("저장된 원문은 데이터베이스에서도 변경할 수 없다")
    void 저장된_원문은_데이터베이스에서도_변경할_수_없다() {
        JobPosting created;
        try (UserScope ignored = currentUserContext.bind(DevelopmentUsers.USER_A)) {
            created = service.create("커리어핏", "개발자", "최초 공고 원문");
        }

        assertThatThrownBy(() -> jdbcClient
                        .sql("""
                                UPDATE job_posting
                                SET original_text = '구조화 수정값'
                                WHERE job_posting_id = :jobPostingId
                                """)
                        .param("jobPostingId", created.id().value())
                        .update())
                .isInstanceOf(DataAccessException.class);

        try (UserScope ignored = currentUserContext.bind(DevelopmentUsers.USER_A)) {
            assertThat(service.find(created.id()).originalText()).isEqualTo("최초 공고 원문");
        }
    }

    @Test
    @DisplayName("다른 사용자는 채용공고를 조회하거나 삭제할 수 없다")
    void 다른_사용자는_채용공고를_조회하거나_삭제할_수_없다() {
        JobPosting created;
        try (UserScope ignored = currentUserContext.bind(DevelopmentUsers.USER_A)) {
            created = service.create("커리어핏", "개발자", "사용자 A 공고");
        }

        try (UserScope ignored = currentUserContext.bind(DevelopmentUsers.USER_B)) {
            assertThatThrownBy(() -> service.find(created.id()))
                    .isInstanceOf(JobPostingNotFoundException.class);
            assertThatThrownBy(() -> service.delete(created.id()))
                    .isInstanceOf(JobPostingNotFoundException.class);
        }

        try (UserScope ignored = currentUserContext.bind(DevelopmentUsers.USER_A)) {
            assertThat(service.find(created.id()).originalText()).isEqualTo("사용자 A 공고");
        }
    }

    @Test
    @DisplayName("삭제한 채용공고는 조회할 수 없다")
    void 삭제한_채용공고는_조회할_수_없다() {
        try (UserScope ignored = currentUserContext.bind(DevelopmentUsers.USER_A)) {
            JobPosting created = service.create("커리어핏", "개발자", "삭제할 공고");

            service.delete(created.id());

            assertThatThrownBy(() -> service.find(created.id()))
                    .isInstanceOf(JobPostingNotFoundException.class);
        }
    }

    private int rowCount() {
        return jdbcClient.sql("SELECT COUNT(*) FROM job_posting").query(Integer.class).single();
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
