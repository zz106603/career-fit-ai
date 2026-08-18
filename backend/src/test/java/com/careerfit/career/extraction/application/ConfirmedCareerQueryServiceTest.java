package com.careerfit.career.extraction.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.careerfit.career.application.CareerExperienceNotFoundException;
import com.careerfit.career.application.CareerExperienceRepository;
import com.careerfit.career.domain.CareerExperienceId;
import com.careerfit.career.domain.CareerExperienceSourceType;
import com.careerfit.career.domain.CareerExperienceVersion;
import com.careerfit.career.domain.CareerExperienceVersionId;
import com.careerfit.career.domain.DirectCareerContent;
import com.careerfit.identity.CurrentUser;
import com.careerfit.identity.UserId;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("확정 경력 조회 서비스 테스트")
class ConfirmedCareerQueryServiceTest {
    private static final UserId USER = new UserId(UUID.randomUUID());
    private final CareerExperienceRepository experiences = mock(CareerExperienceRepository.class);
    private final ConfirmedCareerEvidenceRepository evidences =
            mock(ConfirmedCareerEvidenceRepository.class);
    private ConfirmedCareerQueryService service;

    @BeforeEach
    void 서비스를_준비한다() {
        service = new ConfirmedCareerQueryService(
                experiences, evidences, () -> new CurrentUser(USER));
    }

    @Test
    @DisplayName("현재 확정 경력에 버전별 Evidence를 연결한다")
    void 현재_확정_경력에_버전별_Evidence를_연결한다() {
        CareerExperienceVersion version = version(CareerExperienceId.newId(), 1, null);
        ConfirmedCareerEvidenceView evidence = new ConfirmedCareerEvidenceView(
                version.id(), UUID.randomUUID(), "resume.pdf", 2, "API 개발");
        when(experiences.findCurrentConfirmed(USER)).thenReturn(List.of(version));
        when(evidences.findAll(USER, List.of(version.id()))).thenReturn(List.of(evidence));

        List<ConfirmedCareerVersionView> result = service.findCurrent();

        assertThat(result).singleElement().satisfies(view -> {
            assertThat(view.version()).isEqualTo(version);
            assertThat(view.evidences()).containsExactly(evidence);
        });
    }

    @Test
    @DisplayName("경력 상세에서는 현재 버전과 과거 확정 버전을 함께 반환한다")
    void 경력_상세에서는_현재와_과거_확정_버전을_함께_반환한다() {
        CareerExperienceId experienceId = CareerExperienceId.newId();
        CareerExperienceVersion current = version(experienceId, 2, null);
        CareerExperienceVersion previous =
                version(experienceId, 1, Instant.parse("2026-08-02T00:00:00Z"));
        when(experiences.findConfirmedVersions(USER, current.experienceId()))
                .thenReturn(List.of(current, previous));
        when(evidences.findAll(USER, List.of(current.id(), previous.id())))
                .thenReturn(List.of());

        List<ConfirmedCareerVersionView> result = service.findVersions(current.experienceId());

        assertThat(result).extracting(view -> view.version().versionNo()).containsExactly(2, 1);
    }

    @Test
    @DisplayName("다른 사용자의 경력은 찾을 수 없는 경력으로 처리한다")
    void 다른_사용자의_경력은_찾을_수_없는_경력으로_처리한다() {
        CareerExperienceId experienceId = CareerExperienceId.newId();
        when(experiences.findConfirmedVersions(USER, experienceId)).thenReturn(List.of());

        assertThatThrownBy(() -> service.findVersions(experienceId))
                .isInstanceOf(CareerExperienceNotFoundException.class);
    }

    private static CareerExperienceVersion version(
            CareerExperienceId experienceId, int versionNo, Instant supersededAt) {
        Instant createdAt = Instant.parse("2026-08-01T00:00:00Z");
        return new CareerExperienceVersion(
                CareerExperienceVersionId.newId(), experienceId, USER, versionNo,
                CareerExperienceSourceType.DOCUMENT,
                new DirectCareerContent("백엔드 개발", "커리어핏", "개발자", "API 개발"),
                createdAt, createdAt, supersededAt, null);
    }
}
