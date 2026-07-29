package com.careerfit.common.async.infrastructure;

import com.careerfit.common.async.domain.JobExecutionStatus;
import com.careerfit.common.async.domain.JobType;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SpringDataJobExecutionRepository
        extends JpaRepository<JobExecutionEntity, UUID> {

    Optional<JobExecutionEntity> findByIdAndUserId(UUID id, UUID userId);

    Optional<JobExecutionEntity> findByUserIdAndDuplicateKeyAndStatusIn(
            UUID userId,
            String duplicateKey,
            Collection<JobExecutionStatus> statuses);

    List<JobExecutionEntity> findByStatusOrderByCreatedAtAscIdAsc(
            JobExecutionStatus status, Pageable pageable);

    List<JobExecutionEntity> findByStatusAndClaimedAtLessThanEqualOrderByClaimedAtAscIdAsc(
            JobExecutionStatus status, Instant claimedAt, Pageable pageable);

    @Modifying(flushAutomatically = true)
    @Query(
            value = """
                    INSERT INTO job_execution (
                        job_execution_id, user_id, job_type, target_id, input_version,
                        duplicate_key, status, created_at
                    ) VALUES (
                        :executionId, :userId, :jobType, :targetId, :inputVersion,
                        :duplicateKey, 'QUEUED', :createdAt
                    )
                    ON CONFLICT (user_id, duplicate_key)
                        WHERE status IN ('QUEUED', 'PROCESSING')
                    DO NOTHING
                    """,
            nativeQuery = true)
    int insertQueued(
            @Param("executionId") UUID executionId,
            @Param("userId") UUID userId,
            @Param("jobType") String jobType,
            @Param("targetId") UUID targetId,
            @Param("inputVersion") String inputVersion,
            @Param("duplicateKey") String duplicateKey,
            @Param("createdAt") Instant createdAt);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update JobExecutionEntity execution
               set execution.status = :status,
                   execution.failureCode = :failureCode,
                   execution.retryCount = :retryCount,
                   execution.claimedAt = :claimedAt,
                   execution.completedAt = :completedAt
             where execution.id = :executionId
               and execution.userId = :userId
               and execution.status = :expectedStatus
            """)
    int updateStatus(
            @Param("executionId") UUID executionId,
            @Param("userId") UUID userId,
            @Param("expectedStatus") JobExecutionStatus expectedStatus,
            @Param("status") JobExecutionStatus status,
            @Param("retryCount") int retryCount,
            @Param("failureCode") String failureCode,
            @Param("claimedAt") Instant claimedAt,
            @Param("completedAt") Instant completedAt);
}
