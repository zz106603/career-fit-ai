package com.careerfit.analysis.match.application;

import com.careerfit.analysis.match.domain.JobAnalysisResult;
import com.careerfit.analysis.match.domain.JobAnalysisResultId;
import com.careerfit.analysis.match.domain.RequirementMatchResult;
import com.careerfit.analysis.search.application.CareerCandidateSearchRepository;
import com.careerfit.analysis.search.domain.CareerCandidateSearch;
import com.careerfit.analysis.search.domain.CareerCandidateSearchId;
import com.careerfit.career.application.CareerExperienceRepository;
import com.careerfit.career.domain.CareerExperienceVersion;
import com.careerfit.identity.CurrentUserProvider;
import com.careerfit.identity.UserId;
import com.careerfit.job.structure.application.JobPostingAnalysisRepository;
import com.careerfit.job.structure.domain.JobPostingAnalysis;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DeterministicJobAnalysisService {

    private static final String WORKFLOW_VERSION = "deterministic-match-v1";

    private final CareerCandidateSearchRepository searchRepository;
    private final JobPostingAnalysisRepository postingAnalysisRepository;
    private final CareerExperienceRepository experienceRepository;
    private final JobAnalysisResultRepository resultRepository;
    private final DeterministicRequirementMatcher matcher;
    private final CurrentUserProvider currentUserProvider;
    private final Clock clock;

    public DeterministicJobAnalysisService(
            CareerCandidateSearchRepository searchRepository,
            JobPostingAnalysisRepository postingAnalysisRepository,
            CareerExperienceRepository experienceRepository,
            JobAnalysisResultRepository resultRepository,
            DeterministicRequirementMatcher matcher,
            CurrentUserProvider currentUserProvider,
            Clock clock) {
        this.searchRepository = searchRepository;
        this.postingAnalysisRepository = postingAnalysisRepository;
        this.experienceRepository = experienceRepository;
        this.resultRepository = resultRepository;
        this.matcher = matcher;
        this.currentUserProvider = currentUserProvider;
        this.clock = clock;
    }

    public JobAnalysisResult analyze(CareerCandidateSearchId searchId) {
        UserId userId = currentUserProvider.currentUserId();
        CareerCandidateSearch search = searchRepository
                .find(userId, searchId)
                .orElseThrow(JobAnalysisResultNotFoundException::new);
        JobPostingAnalysis postingAnalysis = postingAnalysisRepository
                .findReadyByRequirement(userId, search.requirementId())
                .orElseThrow(JobAnalysisResultNotFoundException::new);
        List<CareerExperienceVersion> versions = search.candidates().stream()
                .map(candidate -> experienceRepository
                        .findCurrentConfirmedVersion(userId, candidate.experienceVersionId())
                        .orElse(null))
                .filter(java.util.Objects::nonNull)
                .toList();
        RequirementMatchResult match =
                matcher.match(postingAnalysis.requirement(), search, versions);
        JobAnalysisResult result = new JobAnalysisResult(
                JobAnalysisResultId.newId(),
                userId,
                postingAnalysis.jobPostingId(),
                search.id(),
                WORKFLOW_VERSION,
                clock.instant(),
                match);
        resultRepository.save(result);
        return result;
    }

    public JobAnalysisResult find(JobAnalysisResultId resultId) {
        return resultRepository
                .find(currentUserProvider.currentUserId(), resultId)
                .orElseThrow(JobAnalysisResultNotFoundException::new);
    }
}
