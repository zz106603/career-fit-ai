package com.careerfit.job.structure.application;

import com.careerfit.identity.UserId;
import com.careerfit.job.domain.JobPostingId;
import com.careerfit.job.structure.domain.JobPostingAnalysis;
import java.util.Optional;

public interface JobPostingAnalysisRepository {

    void saveReady(JobPostingAnalysis analysis);

    Optional<JobPostingAnalysis> findLatestReady(UserId userId, JobPostingId jobPostingId);
}
