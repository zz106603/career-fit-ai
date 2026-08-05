package com.careerfit.career.infrastructure;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SpringDataCareerExperienceVersionRepository
        extends JpaRepository<CareerExperienceVersionEntity, UUID> {

    Optional<CareerExperienceVersionEntity>
            findByIdAndExperienceIdAndUserIdAndDeletedAtIsNull(
                    UUID id, UUID experienceId, UUID userId);

    @Query("""
            select version
              from CareerExperienceVersionEntity version
             where version.id = :versionId
               and version.userId = :userId
               and version.confirmedAt is not null
               and version.supersededAt is null
               and version.deletedAt is null
               and exists (
                   select experience.id
                     from CareerExperienceEntity experience
                    where experience.id = version.experienceId
                      and experience.userId = :userId
                      and experience.deletedAt is null
               )
            """)
    Optional<CareerExperienceVersionEntity> findCurrentConfirmedVersion(
            @Param("userId") UUID userId, @Param("versionId") UUID versionId);

    @Query("""
            select version
              from CareerExperienceVersionEntity version
             where version.experienceId = :experienceId
               and version.userId = :userId
               and version.confirmedAt is not null
               and version.supersededAt is null
               and version.deletedAt is null
            """)
    Optional<CareerExperienceVersionEntity> findCurrentConfirmedByExperience(
            @Param("userId") UUID userId, @Param("experienceId") UUID experienceId);

    @Query("""
            select coalesce(max(version.versionNo), 0) + 1
              from CareerExperienceVersionEntity version
             where version.experienceId = :experienceId
               and version.userId = :userId
            """)
    int nextVersionNumber(
            @Param("userId") UUID userId, @Param("experienceId") UUID experienceId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update CareerExperienceVersionEntity version
               set version.supersededAt = :supersededAt
             where version.experienceId = :experienceId
               and version.userId = :userId
               and version.id <> :nextVersionId
               and version.confirmedAt is not null
               and version.supersededAt is null
               and version.deletedAt is null
            """)
    int supersedeCurrent(
            @Param("userId") UUID userId,
            @Param("experienceId") UUID experienceId,
            @Param("nextVersionId") UUID nextVersionId,
            @Param("supersededAt") Instant supersededAt);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update CareerExperienceVersionEntity version
               set version.confirmedAt = :confirmedAt
             where version.id = :versionId
               and version.experienceId = :experienceId
               and version.userId = :userId
               and version.confirmedAt is null
               and version.deletedAt is null
            """)
    int confirm(
            @Param("userId") UUID userId,
            @Param("experienceId") UUID experienceId,
            @Param("versionId") UUID versionId,
            @Param("confirmedAt") Instant confirmedAt);

    @Query("""
            select version
              from CareerExperienceVersionEntity version
             where version.userId = :userId
               and version.confirmedAt is not null
               and version.supersededAt is null
               and version.deletedAt is null
               and exists (
                   select experience.id
                     from CareerExperienceEntity experience
                    where experience.id = version.experienceId
                      and experience.userId = :userId
                      and experience.deletedAt is null
               )
             order by version.confirmedAt desc
            """)
    List<CareerExperienceVersionEntity> findCurrentConfirmed(
            @Param("userId") UUID userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update CareerExperienceVersionEntity version
               set version.deletedAt = :deletedAt
             where version.experienceId = :experienceId
               and version.userId = :userId
               and version.deletedAt is null
            """)
    int softDeleteByExperience(
            @Param("userId") UUID userId,
            @Param("experienceId") UUID experienceId,
            @Param("deletedAt") Instant deletedAt);
}
