package com.careerfit.career.application;

import com.careerfit.career.domain.CareerExperience;
import com.careerfit.career.domain.CareerExperienceId;
import com.careerfit.career.domain.CareerExperienceVersion;
import com.careerfit.career.domain.CareerExperienceVersionId;
import com.careerfit.identity.UserId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface CareerExperienceRepository {

    void saveExperience(CareerExperience experience);

    void saveVersion(CareerExperienceVersion version);

    Optional<CareerExperience> findActiveExperience(UserId userId, CareerExperienceId experienceId);

    Optional<CareerExperienceVersion> findActiveVersion(
            UserId userId,
            CareerExperienceId experienceId,
            CareerExperienceVersionId versionId);

    Optional<CareerExperienceVersion> findCurrentConfirmedVersion(
            UserId userId, CareerExperienceVersionId versionId);

    Optional<CareerExperienceVersion> findCurrentConfirmedByExperience(
            UserId userId, CareerExperienceId experienceId);

    int nextVersionNumber(UserId userId, CareerExperienceId experienceId);

    void supersedeCurrentVersion(
            UserId userId,
            CareerExperienceId experienceId,
            CareerExperienceVersionId nextVersionId,
            Instant supersededAt);

    boolean confirmVersion(
            UserId userId,
            CareerExperienceId experienceId,
            CareerExperienceVersionId versionId,
            Instant confirmedAt);

    List<CareerExperienceVersion> findCurrentConfirmed(UserId userId);

    void delete(UserId userId, CareerExperienceId experienceId, Instant deletedAt);
}
