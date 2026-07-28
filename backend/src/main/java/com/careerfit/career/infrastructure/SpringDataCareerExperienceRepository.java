package com.careerfit.career.infrastructure;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SpringDataCareerExperienceRepository
        extends JpaRepository<CareerExperienceEntity, UUID> {

    Optional<CareerExperienceEntity> findByIdAndUserIdAndDeletedAtIsNull(
            UUID id, UUID userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update CareerExperienceEntity experience
               set experience.deletedAt = :deletedAt
             where experience.id = :experienceId
               and experience.userId = :userId
               and experience.deletedAt is null
            """)
    int softDelete(
            @Param("userId") UUID userId,
            @Param("experienceId") UUID experienceId,
            @Param("deletedAt") Instant deletedAt);
}
