package com.careerfit.career.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.careerfit.PostgresIntegrationTest;
import com.careerfit.career.domain.CareerExperienceSourceType;
import com.careerfit.career.domain.CareerExperienceVersion;
import com.careerfit.career.domain.DirectCareerContent;
import com.careerfit.identity.RequestCurrentUserContext;
import com.careerfit.identity.RequestCurrentUserContext.UserScope;
import com.careerfit.identity.development.DevelopmentUsers;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
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
@DisplayName("직접 입력 경력 서비스 통합 테스트")
class DirectCareerServiceIntegrationTest extends PostgresIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-07-24T00:00:00Z");

    @Autowired
    private DirectCareerService service;

    @Autowired
    private RequestCurrentUserContext currentUserContext;

    @Autowired
    private JdbcClient jdbcClient;

    @BeforeEach
    void 데이터베이스를_초기화한다() {
        jdbcClient.sql("TRUNCATE career_experience CASCADE").update();
    }

    @Test
    @DisplayName("직접 입력 경력을 USER_DIRECT 미확정 버전으로 저장한다")
    void 직접_입력_경력을_USER_DIRECT_미확정_버전으로_저장한다() {
        try (UserScope ignored = currentUserContext.bind(DevelopmentUsers.USER_A)) {
            CareerExperienceVersion version = service.create(content("백엔드 프로젝트"));

            assertThat(version.sourceType()).isEqualTo(CareerExperienceSourceType.USER_DIRECT);
            assertThat(version.versionNo()).isEqualTo(1);
            assertThat(version.confirmedAt()).isNull();
            assertThat(service.findConfirmed()).isEmpty();
        }
    }

    @Test
    @DisplayName("경력을 확정하면 확정 시각을 기록하고 검색 대상에 포함한다")
    void 경력을_확정하면_확정_시각을_기록하고_검색_대상에_포함한다() {
        try (UserScope ignored = currentUserContext.bind(DevelopmentUsers.USER_A)) {
            CareerExperienceVersion draft = service.create(content("백엔드 프로젝트"));

            service.confirm(draft.experienceId(), draft.id());

            List<CareerExperienceVersion> confirmed = service.findConfirmed();
            assertThat(confirmed).singleElement().satisfies(version -> {
                assertThat(version.id()).isEqualTo(draft.id());
                assertThat(version.confirmedAt()).isEqualTo(NOW);
            });
        }
    }

    @Test
    @DisplayName("확정 경력을 수정하면 새 버전을 만들고 기존 버전을 보존한다")
    void 확정_경력을_수정하면_새_버전을_만들고_기존_버전을_보존한다() {
        try (UserScope ignored = currentUserContext.bind(DevelopmentUsers.USER_A)) {
            CareerExperienceVersion first = service.create(content("첫 번째 제목"));
            service.confirm(first.experienceId(), first.id());
            CareerExperienceVersion second =
                    service.revise(first.experienceId(), content("두 번째 제목"));

            service.confirm(second.experienceId(), second.id());

            assertThat(second.versionNo()).isEqualTo(2);
            assertThat(service.findConfirmed())
                    .singleElement()
                    .extracting(version -> version.content().title())
                    .isEqualTo("두 번째 제목");
            assertThat(versionCount(first.experienceId().value())).isEqualTo(2);
            assertThat(supersededAt(first.id().value())).isEqualTo(NOW);
        }
    }

    @Test
    @DisplayName("논리 삭제한 경력은 확정 경력 조회에서 제외한다")
    void 논리_삭제한_경력은_확정_경력_조회에서_제외한다() {
        try (UserScope ignored = currentUserContext.bind(DevelopmentUsers.USER_A)) {
            CareerExperienceVersion version = service.create(content("삭제할 경력"));
            service.confirm(version.experienceId(), version.id());

            service.delete(version.experienceId());

            assertThat(service.findConfirmed()).isEmpty();
            assertThat(deletedAt(version.experienceId().value())).isEqualTo(NOW);
        }
    }

    @Test
    @DisplayName("다른 사용자는 경력을 조회하거나 변경할 수 없다")
    void 다른_사용자는_경력을_조회하거나_변경할_수_없다() {
        CareerExperienceVersion userAVersion;
        try (UserScope ignored = currentUserContext.bind(DevelopmentUsers.USER_A)) {
            userAVersion = service.create(content("사용자 A 경력"));
            service.confirm(userAVersion.experienceId(), userAVersion.id());
        }

        try (UserScope ignored = currentUserContext.bind(DevelopmentUsers.USER_B)) {
            assertThat(service.findConfirmed()).isEmpty();
            assertThatThrownBy(() -> service.delete(userAVersion.experienceId()))
                    .isInstanceOf(CareerExperienceNotFoundException.class);
            assertThatThrownBy(
                            () -> service.revise(
                                    userAVersion.experienceId(), content("침범한 수정")))
                    .isInstanceOf(CareerExperienceNotFoundException.class);
        }
    }

    private DirectCareerContent content(String title) {
        return new DirectCareerContent(title, "커리어핏", "백엔드 개발", "API를 개선했다.");
    }

    private int versionCount(java.util.UUID experienceId) {
        return jdbcClient
                .sql("""
                        SELECT COUNT(*)
                        FROM career_experience_version
                        WHERE experience_id = :experienceId
                        """)
                .param("experienceId", experienceId)
                .query(Integer.class)
                .single();
    }

    private Instant supersededAt(java.util.UUID versionId) {
        return jdbcClient
                .sql("""
                        SELECT superseded_at
                        FROM career_experience_version
                        WHERE experience_version_id = :versionId
                        """)
                .param("versionId", versionId)
                .query(Instant.class)
                .single();
    }

    private Instant deletedAt(java.util.UUID experienceId) {
        return jdbcClient
                .sql("""
                        SELECT deleted_at
                        FROM career_experience
                        WHERE experience_id = :experienceId
                        """)
                .param("experienceId", experienceId)
                .query(Instant.class)
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
