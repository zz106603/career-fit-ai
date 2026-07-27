package com.careerfit.job.structure.application;

import com.careerfit.identity.CurrentUserProvider;
import com.careerfit.identity.UserId;
import com.careerfit.job.application.JobPostingNotFoundException;
import com.careerfit.job.application.JobPostingRepository;
import com.careerfit.job.domain.JobPosting;
import com.careerfit.job.domain.JobPostingId;
import com.careerfit.job.structure.domain.JobPostingAnalysis;
import com.careerfit.job.structure.domain.JobRequirement;
import com.careerfit.job.structure.domain.JobRequirementCategory;
import com.careerfit.job.structure.domain.JobRequirementId;
import java.time.Clock;
import org.springframework.stereotype.Service;

@Service
public class JobPostingStructureService {

    private static final String WORKFLOW_VERSION = "fake-job-structure-v1";

    private final JobPostingRepository jobPostingRepository;
    private final JobPostingAnalysisRepository analysisRepository;
    private final FakeJobStructureGenerator structureGenerator;
    private final CurrentUserProvider currentUserProvider;
    private final Clock clock;

    public JobPostingStructureService(
            JobPostingRepository jobPostingRepository,
            JobPostingAnalysisRepository analysisRepository,
            FakeJobStructureGenerator structureGenerator,
            CurrentUserProvider currentUserProvider,
            Clock clock) {
        this.jobPostingRepository = jobPostingRepository;
        this.analysisRepository = analysisRepository;
        this.structureGenerator = structureGenerator;
        this.currentUserProvider = currentUserProvider;
        this.clock = clock;
    }

    public JobPostingAnalysis structure(JobPostingId jobPostingId) {
        UserId userId = currentUserProvider.currentUserId();
        JobPosting jobPosting = jobPostingRepository
                .findActive(userId, jobPostingId)
                .orElseThrow(JobPostingNotFoundException::new);
        FakeJobStructureResult result = structureGenerator.generate(jobPosting.originalText());
        JobPostingAnalysis processing = JobPostingAnalysis.processing(
                jobPostingId,
                userId,
                jobPosting.companyHint(),
                jobPosting.titleHint(),
                WORKFLOW_VERSION + ":" + result.model(),
                clock.instant());
        JobRequirement requirement = new JobRequirement(
                JobRequirementId.newId(),
                processing.id(),
                JobRequirementCategory.REQUIRED,
                result.requirementText(),
                result.sourceExcerpt(),
                1);
        JobPostingAnalysis ready = processing.ready(requirement, clock.instant());
        analysisRepository.saveReady(ready);
        return ready;
    }

    public JobPostingAnalysis findLatestReady(JobPostingId jobPostingId) {
        return analysisRepository
                .findLatestReady(currentUserProvider.currentUserId(), jobPostingId)
                .orElseThrow(JobPostingNotFoundException::new);
    }
}
