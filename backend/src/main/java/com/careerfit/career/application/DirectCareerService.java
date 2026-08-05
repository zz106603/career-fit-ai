package com.careerfit.career.application;

import com.careerfit.career.domain.CareerExperience;
import com.careerfit.career.domain.CareerExperienceId;
import com.careerfit.career.domain.CareerExperienceSourceType;
import com.careerfit.career.domain.CareerExperienceVersion;
import com.careerfit.career.domain.CareerExperienceVersionId;
import com.careerfit.career.domain.DirectCareerContent;
import com.careerfit.identity.CurrentUserProvider;
import com.careerfit.identity.UserId;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
/** 사용자가 직접 입력한 경력을 버전으로 저장하고 명시적 확정 이후에만 검색 가능하게 한다. */
public class DirectCareerService {

    private final CareerExperienceRepository repository;
    private final CurrentUserProvider currentUserProvider;
    private final Clock clock;

    public DirectCareerService(
            CareerExperienceRepository repository,
            CurrentUserProvider currentUserProvider,
            Clock clock) {
        this.repository = repository;
        this.currentUserProvider = currentUserProvider;
        this.clock = clock;
    }

    @Transactional
    public CareerExperienceVersion create(DirectCareerContent content) {
        UserId userId = currentUserProvider.currentUserId();
        Instant now = clock.instant();
        CareerExperienceId experienceId = CareerExperienceId.newId();
        CareerExperienceVersion version = newVersion(experienceId, userId, 1, content, now);

        repository.saveExperience(new CareerExperience(experienceId, userId, now, null));
        repository.saveVersion(version);
        return version;
    }

    @Transactional
    public CareerExperienceVersion revise(
            CareerExperienceId experienceId, DirectCareerContent content) {
        UserId userId = currentUserProvider.currentUserId();
        requireExperience(userId, experienceId);
        CareerExperienceVersion version = newVersion(
                experienceId,
                userId,
                repository.nextVersionNumber(userId, experienceId),
                content,
                clock.instant());
        repository.saveVersion(version);
        return version;
    }

    @Transactional
    public CareerExperienceVersionId confirm(
            CareerExperienceId experienceId, CareerExperienceVersionId versionId) {
        UserId userId = currentUserProvider.currentUserId();
        requireExperience(userId, experienceId);
        CareerExperienceVersion version = repository
                .findActiveVersion(userId, experienceId, versionId)
                .orElseThrow(CareerExperienceNotFoundException::new);
        if (version.isConfirmed()) {
            throw new CareerVersionAlreadyConfirmedException();
        }
        if (version.sourceType() != CareerExperienceSourceType.USER_DIRECT) {
            throw new IllegalStateException("사용자 직접 입력 경력만 이 경로에서 확정할 수 있습니다.");
        }

        Instant now = clock.instant();
        repository.supersedeCurrentVersion(userId, experienceId, versionId, now);
        if (!repository.confirmVersion(userId, experienceId, versionId, now)) {
            throw new CareerExperienceNotFoundException();
        }
        return versionId;
    }

    @Transactional(readOnly = true)
    public List<CareerExperienceVersion> findConfirmed() {
        return repository.findCurrentConfirmed(currentUserProvider.currentUserId());
    }

    @Transactional
    public void delete(CareerExperienceId experienceId) {
        UserId userId = currentUserProvider.currentUserId();
        requireExperience(userId, experienceId);
        repository.delete(userId, experienceId, clock.instant());
    }

    private void requireExperience(UserId userId, CareerExperienceId experienceId) {
        repository
                .findActiveExperience(userId, experienceId)
                .orElseThrow(CareerExperienceNotFoundException::new);
    }

    private CareerExperienceVersion newVersion(
            CareerExperienceId experienceId,
            UserId userId,
            int versionNo,
            DirectCareerContent content,
            Instant createdAt) {
        return new CareerExperienceVersion(
                CareerExperienceVersionId.newId(),
                experienceId,
                userId,
                versionNo,
                CareerExperienceSourceType.USER_DIRECT,
                content,
                createdAt,
                null,
                null,
                null);
    }
}
