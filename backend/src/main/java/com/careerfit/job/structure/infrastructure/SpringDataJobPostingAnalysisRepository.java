package com.careerfit.job.structure.infrastructure;

import com.careerfit.job.structure.domain.JobPostingAnalysisStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataJobPostingAnalysisRepository
        extends JpaRepository<JobPostingAnalysisEntity, UUID> {

    Optional<JobPostingAnalysisEntity>
            findFirstByUserIdAndJobPostingIdAndStatusOrderByReadyAtDescIdDesc(
                    UUID userId,
                    UUID jobPostingId,
                    JobPostingAnalysisStatus status);
}
