package com.careerfit.job.infrastructure;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SpringDataJobPostingRepository extends JpaRepository<JobPostingEntity, UUID> {

    Optional<JobPostingEntity> findByIdAndUserIdAndDeletedAtIsNull(UUID id, UUID userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update JobPostingEntity posting
               set posting.deletedAt = :deletedAt
             where posting.id = :id
               and posting.userId = :userId
               and posting.deletedAt is null
            """)
    int softDelete(
            @Param("id") UUID id,
            @Param("userId") UUID userId,
            @Param("deletedAt") Instant deletedAt);
}
