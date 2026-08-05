package com.careerfit.career.extraction.application;

import com.careerfit.career.application.CareerExperienceNotFoundException;
import com.careerfit.career.application.CareerExperienceRepository;
import com.careerfit.career.application.CareerVersionAlreadyConfirmedException;
import com.careerfit.career.domain.CareerExperience;
import com.careerfit.career.domain.CareerExperienceId;
import com.careerfit.career.domain.CareerExperienceSourceType;
import com.careerfit.career.domain.CareerExperienceVersion;
import com.careerfit.career.domain.CareerExperienceVersionId;
import com.careerfit.career.domain.DirectCareerContent;
import com.careerfit.career.extraction.domain.CareerExtractionCandidate;
import com.careerfit.identity.CurrentUserProvider;
import com.careerfit.identity.UserId;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
/** 검토 후보를 DOCUMENT 경력으로 확정하고 이후 변경을 불변 버전으로 관리한다. */
public class DocumentCareerConfirmationService {
    private final CareerExtractionCandidateRepository candidateRepository;
    private final CareerExperienceRepository experienceRepository;
    private final ConfirmedCareerEvidenceRepository evidenceRepository;
    private final CurrentUserProvider currentUserProvider;
    private final Clock clock;

    public DocumentCareerConfirmationService(
            CareerExtractionCandidateRepository candidateRepository,
            CareerExperienceRepository experienceRepository,
            ConfirmedCareerEvidenceRepository evidenceRepository,
            CurrentUserProvider currentUserProvider,
            Clock clock) {
        this.candidateRepository = candidateRepository;
        this.experienceRepository = experienceRepository;
        this.evidenceRepository = evidenceRepository;
        this.currentUserProvider = currentUserProvider;
        this.clock = clock;
    }

    @Transactional
    /** 검토 후보와 원문 Evidence를 하나의 DOCUMENT 확정 경력 첫 버전으로 저장한다. */
    public CareerExperienceVersion confirmCandidate(UUID candidateId, DirectCareerContent content) {
        UserId userId = currentUserProvider.currentUserId();
        CareerExtractionCandidate candidate = candidateRepository
                .findAllForUpdate(userId, List.of(candidateId)).stream()
                .findFirst()
                .orElseThrow(CareerCandidateNotFoundException::new);
        if (!candidate.isEditable()) {
            throw new IllegalStateException("검토 가능한 후보만 확정할 수 있습니다.");
        }
        if (candidateRepository.findEvidences(userId, List.of(candidateId)).isEmpty()) {
            throw new IllegalStateException("DOCUMENT 경력 확정에는 원문 Evidence가 필요합니다.");
        }

        Instant now = clock.instant();
        CareerExperienceId experienceId = CareerExperienceId.newId();
        CareerExperienceVersion version = new CareerExperienceVersion(
                CareerExperienceVersionId.newId(), experienceId, userId, 1,
                CareerExperienceSourceType.DOCUMENT, content, now, now, null, null);
        experienceRepository.saveExperience(new CareerExperience(experienceId, userId, now, null));
        experienceRepository.saveVersion(version);
        if (evidenceRepository.copyFromCandidate(userId, candidateId, version.id()) < 1) {
            throw new IllegalStateException("DOCUMENT 경력 Evidence를 저장할 수 없습니다.");
        }
        candidateRepository.saveAll(List.of(candidate.confirm()), List.of());
        return version;
    }

    @Transactional
    /** 현재 DOCUMENT 경력은 유지하고 같은 경력 ID에 다음 미확정 버전을 만든다. */
    public CareerExperienceVersion revise(CareerExperienceId experienceId, DirectCareerContent content) {
        UserId userId = currentUserProvider.currentUserId();
        CareerExperienceVersion current = experienceRepository
                .findCurrentConfirmedByExperience(userId, experienceId)
                .orElseThrow(CareerExperienceNotFoundException::new);
        if (current.sourceType() != CareerExperienceSourceType.DOCUMENT) {
            throw new IllegalStateException("DOCUMENT 경력만 이 경로에서 수정할 수 있습니다.");
        }
        CareerExperienceVersion next = new CareerExperienceVersion(
                CareerExperienceVersionId.newId(), experienceId, userId,
                experienceRepository.nextVersionNumber(userId, experienceId),
                CareerExperienceSourceType.DOCUMENT, content, clock.instant(), null, null, null);
        experienceRepository.saveVersion(next);
        if (evidenceRepository.copyFromVersion(userId, current.id(), next.id()) < 1) {
            throw new IllegalStateException("DOCUMENT 경력 Evidence를 새 버전에 저장할 수 없습니다.");
        }
        return next;
    }

    @Transactional
    /** 새 버전의 Evidence를 확인한 뒤 기존 현재 버전을 대체하고 새 버전을 확정한다. */
    public CareerExperienceVersionId confirmRevision(
            CareerExperienceId experienceId, CareerExperienceVersionId versionId) {
        UserId userId = currentUserProvider.currentUserId();
        CareerExperienceVersion version = experienceRepository
                .findActiveVersion(userId, experienceId, versionId)
                .orElseThrow(CareerExperienceNotFoundException::new);
        if (version.sourceType() != CareerExperienceSourceType.DOCUMENT) {
            throw new IllegalStateException("DOCUMENT 경력 버전만 이 경로에서 확정할 수 있습니다.");
        }
        if (version.isConfirmed()) throw new CareerVersionAlreadyConfirmedException();
        if (!evidenceRepository.exists(userId, versionId)) {
            throw new IllegalStateException("DOCUMENT 경력 확정에는 원문 Evidence가 필요합니다.");
        }

        Instant now = clock.instant();
        experienceRepository.supersedeCurrentVersion(userId, experienceId, versionId, now);
        if (!experienceRepository.confirmVersion(userId, experienceId, versionId, now)) {
            throw new CareerExperienceNotFoundException();
        }
        return versionId;
    }
}
