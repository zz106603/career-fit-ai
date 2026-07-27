package com.careerfit.job.application;

import com.careerfit.identity.UserId;
import com.careerfit.job.domain.JobPosting;
import com.careerfit.job.domain.JobPostingId;
import java.time.Instant;
import java.util.Optional;

public interface JobPostingRepository {

    void save(JobPosting jobPosting);

    Optional<JobPosting> findActive(UserId userId, JobPostingId jobPostingId);

    boolean delete(UserId userId, JobPostingId jobPostingId, Instant deletedAt);
}
