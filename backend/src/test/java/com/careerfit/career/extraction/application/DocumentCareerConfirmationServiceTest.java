package com.careerfit.career.extraction.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.careerfit.career.application.CareerExperienceRepository;
import com.careerfit.career.document.domain.CareerDocumentAnalysisId;
import com.careerfit.career.document.domain.CareerDocumentId;
import com.careerfit.career.domain.CareerExperienceId;
import com.careerfit.career.domain.CareerExperienceSourceType;
import com.careerfit.career.domain.CareerExperienceVersion;
import com.careerfit.career.domain.CareerExperienceVersionId;
import com.careerfit.career.domain.DirectCareerContent;
import com.careerfit.career.extraction.domain.CareerExtractionCandidate;
import com.careerfit.career.extraction.domain.CareerExtractionCandidateStatus;
import com.careerfit.career.extraction.domain.ExperienceEvidence;
import com.careerfit.identity.CurrentUser;
import com.careerfit.identity.CurrentUserProvider;
import com.careerfit.identity.UserId;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@DisplayName("문서 경력 후보 확정 서비스 테스트")
class DocumentCareerConfirmationServiceTest {
    private static final Instant NOW = Instant.parse("2026-08-05T00:00:00Z");
    private static final UserId USER = new UserId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    private final CareerExtractionCandidateRepository candidates = mock(CareerExtractionCandidateRepository.class);
    private final CareerExperienceRepository experiences = mock(CareerExperienceRepository.class);
    private final ConfirmedCareerEvidenceRepository evidences = mock(ConfirmedCareerEvidenceRepository.class);
    private final CurrentUserProvider currentUserProvider = () -> new CurrentUser(USER);
    private DocumentCareerConfirmationService service;

    @BeforeEach
    void 서비스를_준비한다() {
        service = new DocumentCareerConfirmationService(candidates, experiences, evidences,
                currentUserProvider, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    @DisplayName("검토 후보를 확정하면 DOCUMENT 경력과 Evidence를 저장하고 후보를 확정한다")
    void 검토_후보를_확정하면_DOCUMENT_경력과_Evidence를_저장하고_후보를_확정한다() {
        CareerExtractionCandidate candidate = candidate(USER);
        when(candidates.findAllForUpdate(USER, List.of(candidate.id()))).thenReturn(List.of(candidate));
        when(candidates.findEvidences(USER, List.of(candidate.id())))
                .thenReturn(List.of(evidence(candidate)));
        when(evidences.copyFromCandidate(any(), any(), any())).thenReturn(1);

        CareerExperienceVersion result = service.confirmCandidate(candidate.id(), content("확정 경력"));

        assertThat(result.sourceType()).isEqualTo(CareerExperienceSourceType.DOCUMENT);
        assertThat(result.versionNo()).isEqualTo(1);
        assertThat(result.confirmedAt()).isEqualTo(NOW);
        ArgumentCaptor<List<CareerExtractionCandidate>> saved = ArgumentCaptor.forClass(List.class);
        verify(candidates).saveAll(saved.capture(), any());
        assertThat(saved.getValue()).singleElement()
                .extracting(CareerExtractionCandidate::status)
                .isEqualTo(CareerExtractionCandidateStatus.CONFIRMED);
    }

    @Test
    @DisplayName("원문 Evidence가 없는 DOCUMENT 후보는 확정하지 않는다")
    void 원문_Evidence가_없는_DOCUMENT_후보는_확정하지_않는다() {
        CareerExtractionCandidate candidate = candidate(USER);
        when(candidates.findAllForUpdate(USER, List.of(candidate.id()))).thenReturn(List.of(candidate));
        when(candidates.findEvidences(USER, List.of(candidate.id()))).thenReturn(List.of());

        assertThatThrownBy(() -> service.confirmCandidate(candidate.id(), content("확정 경력")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Evidence");
        verify(experiences, never()).saveExperience(any());
    }

    @Test
    @DisplayName("확정 DOCUMENT 경력을 수정하면 기존 버전을 유지한 채 다음 미확정 버전을 만든다")
    void 확정_DOCUMENT_경력을_수정하면_기존_버전을_유지한_채_다음_미확정_버전을_만든다() {
        CareerExperienceId experienceId = CareerExperienceId.newId();
        CareerExperienceVersion current = version(experienceId, 1, NOW);
        when(experiences.findCurrentConfirmedByExperience(USER, experienceId))
                .thenReturn(Optional.of(current));
        when(experiences.nextVersionNumber(USER, experienceId)).thenReturn(2);
        when(evidences.copyFromVersion(any(), any(), any())).thenReturn(1);

        CareerExperienceVersion revised = service.revise(experienceId, content("수정 경력"));

        assertThat(revised.versionNo()).isEqualTo(2);
        assertThat(revised.confirmedAt()).isNull();
        verify(experiences, never()).supersedeCurrentVersion(any(), any(), any(), any());
    }

    @Test
    @DisplayName("새 DOCUMENT 버전을 확정할 때만 기존 현재 버전을 대체한다")
    void 새_DOCUMENT_버전을_확정할_때만_기존_현재_버전을_대체한다() {
        CareerExperienceId experienceId = CareerExperienceId.newId();
        CareerExperienceVersion revision = version(experienceId, 2, null);
        when(experiences.findActiveVersion(USER, experienceId, revision.id()))
                .thenReturn(Optional.of(revision));
        when(evidences.exists(USER, revision.id())).thenReturn(true);
        when(experiences.confirmVersion(USER, experienceId, revision.id(), NOW)).thenReturn(true);

        service.confirmRevision(experienceId, revision.id());

        verify(experiences).supersedeCurrentVersion(USER, experienceId, revision.id(), NOW);
        verify(experiences).confirmVersion(USER, experienceId, revision.id(), NOW);
    }

    @Test
    @DisplayName("다른 사용자의 후보는 존재하지 않는 후보로 처리한다")
    void 다른_사용자의_후보는_존재하지_않는_후보로_처리한다() {
        UUID candidateId = UUID.randomUUID();
        when(candidates.findAllForUpdate(USER, List.of(candidateId))).thenReturn(List.of());

        assertThatThrownBy(() -> service.confirmCandidate(candidateId, content("침범")))
                .isInstanceOf(CareerCandidateNotFoundException.class);
    }

    private static DirectCareerContent content(String title) {
        return new DirectCareerContent(title, "커리어핏", "백엔드 개발", "API를 개발했다.");
    }

    private static CareerExtractionCandidate candidate(UserId userId) {
        return new CareerExtractionCandidate(UUID.randomUUID(), new CareerDocumentAnalysisId(UUID.randomUUID()),
                userId, "PROJECT", "커리어핏", "백엔드 개발", "2026", "API를 개발했다.",
                CareerExtractionCandidateStatus.EDITED, 2, "fake", "v1", "v1", UUID.randomUUID(), NOW);
    }

    private static ExperienceEvidence evidence(CareerExtractionCandidate candidate) {
        return new ExperienceEvidence(UUID.randomUUID(), candidate.id(), candidate.analysisId(),
                new CareerDocumentId(UUID.randomUUID()), candidate.userId(), 1, "원문 발췌");
    }

    private static CareerExperienceVersion version(
            CareerExperienceId experienceId, int versionNo, Instant confirmedAt) {
        return new CareerExperienceVersion(CareerExperienceVersionId.newId(), experienceId, USER,
                versionNo, CareerExperienceSourceType.DOCUMENT, content("문서 경력"), NOW,
                confirmedAt, null, null);
    }
}
