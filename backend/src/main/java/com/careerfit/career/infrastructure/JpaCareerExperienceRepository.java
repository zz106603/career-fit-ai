package com.careerfit.career.infrastructure;

import com.careerfit.career.application.CareerExperienceRepository;
import com.careerfit.career.domain.CareerExperience;
import com.careerfit.career.domain.CareerExperienceId;
import com.careerfit.career.domain.CareerExperienceVersion;
import com.careerfit.career.domain.CareerExperienceVersionId;
import com.careerfit.career.domain.DirectCareerContent;
import com.careerfit.identity.UserId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaCareerExperienceRepository implements CareerExperienceRepository {

    private final SpringDataCareerExperienceRepository experienceRepository;
    private final SpringDataCareerExperienceVersionRepository versionRepository;

    public JpaCareerExperienceRepository(
            SpringDataCareerExperienceRepository experienceRepository,
            SpringDataCareerExperienceVersionRepository versionRepository) {
        this.experienceRepository = experienceRepository;
        this.versionRepository = versionRepository;
    }

    @Override
    public void saveExperience(CareerExperience experience) {
        experienceRepository.save(new CareerExperienceEntity(
                experience.id().value(),
                experience.userId().value(),
                experience.createdAt(),
                experience.deletedAt()));
    }

    @Override
    public void saveVersion(CareerExperienceVersion version) {
        DirectCareerContent content = version.content();
        versionRepository.save(new CareerExperienceVersionEntity(
                version.id().value(),
                version.experienceId().value(),
                version.userId().value(),
                version.versionNo(),
                version.sourceType(),
                content.experienceType(),
                content.title(),
                content.organization(),
                content.startDate(),
                content.endDate(),
                content.role(),
                content.responsibilities(),
                content.problem(),
                content.action(),
                content.outcome(),
                content.technologies(),
                version.createdAt(),
                version.confirmedAt(),
                version.supersededAt(),
                version.deletedAt()));
    }

    @Override
    public Optional<CareerExperience> findActiveExperience(
            UserId userId, CareerExperienceId experienceId) {
        return experienceRepository
                .findByIdAndUserIdAndDeletedAtIsNull(
                        experienceId.value(), userId.value())
                .map(this::toExperience);
    }

    @Override
    public Optional<CareerExperienceVersion> findActiveVersion(
            UserId userId,
            CareerExperienceId experienceId,
            CareerExperienceVersionId versionId) {
        return versionRepository
                .findByIdAndExperienceIdAndUserIdAndDeletedAtIsNull(
                        versionId.value(), experienceId.value(), userId.value())
                .map(this::toVersion);
    }

    @Override
    public Optional<CareerExperienceVersion> findCurrentConfirmedVersion(
            UserId userId, CareerExperienceVersionId versionId) {
        return versionRepository
                .findCurrentConfirmedVersion(userId.value(), versionId.value())
                .map(this::toVersion);
    }

    @Override
    public Optional<CareerExperienceVersion> findCurrentConfirmedByExperience(
            UserId userId, CareerExperienceId experienceId) {
        return versionRepository
                .findCurrentConfirmedByExperience(userId.value(), experienceId.value())
                .map(this::toVersion);
    }

    @Override
    public int nextVersionNumber(UserId userId, CareerExperienceId experienceId) {
        return versionRepository.nextVersionNumber(userId.value(), experienceId.value());
    }

    @Override
    public void supersedeCurrentVersion(
            UserId userId,
            CareerExperienceId experienceId,
            CareerExperienceVersionId nextVersionId,
            Instant supersededAt) {
        versionRepository.supersedeCurrent(
                userId.value(), experienceId.value(), nextVersionId.value(), supersededAt);
    }

    @Override
    public boolean confirmVersion(
            UserId userId,
            CareerExperienceId experienceId,
            CareerExperienceVersionId versionId,
            Instant confirmedAt) {
        return versionRepository.confirm(
                        userId.value(),
                        experienceId.value(),
                        versionId.value(),
                        confirmedAt)
                == 1;
    }

    @Override
    public List<CareerExperienceVersion> findCurrentConfirmed(UserId userId) {
        return versionRepository.findCurrentConfirmed(userId.value()).stream()
                .map(this::toVersion)
                .toList();
    }

    @Override
    public void delete(UserId userId, CareerExperienceId experienceId, Instant deletedAt) {
        versionRepository.softDeleteByExperience(
                userId.value(), experienceId.value(), deletedAt);
        experienceRepository.softDelete(userId.value(), experienceId.value(), deletedAt);
    }

    private CareerExperience toExperience(CareerExperienceEntity entity) {
        return new CareerExperience(
                new CareerExperienceId(entity.id()),
                new UserId(entity.userId()),
                entity.createdAt(),
                entity.deletedAt());
    }

    private CareerExperienceVersion toVersion(CareerExperienceVersionEntity entity) {
        return new CareerExperienceVersion(
                new CareerExperienceVersionId(entity.id()),
                new CareerExperienceId(entity.experienceId()),
                new UserId(entity.userId()),
                entity.versionNo(),
                entity.sourceType(),
                new DirectCareerContent(
                        entity.experienceType(),
                        entity.title(),
                        entity.organization(),
                        entity.startDate(),
                        entity.endDate(),
                        entity.role(),
                        entity.responsibilities(),
                        entity.problem(),
                        entity.action(),
                        entity.outcome(),
                        entity.technologies()),
                entity.createdAt(),
                entity.confirmedAt(),
                entity.supersededAt(),
                entity.deletedAt());
    }
}
