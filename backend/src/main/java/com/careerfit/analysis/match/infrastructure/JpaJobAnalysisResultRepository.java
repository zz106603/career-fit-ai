package com.careerfit.analysis.match.infrastructure;

import com.careerfit.analysis.match.application.JobAnalysisResultRepository;
import com.careerfit.analysis.match.domain.CareerEvidenceSnapshot;
import com.careerfit.analysis.match.domain.JobAnalysisResult;
import com.careerfit.analysis.match.domain.JobAnalysisResultId;
import com.careerfit.analysis.match.domain.RequirementMatchResult;
import com.careerfit.analysis.search.domain.CareerCandidateSearchId;
import com.careerfit.career.domain.CareerExperienceVersionId;
import com.careerfit.identity.UserId;
import com.careerfit.job.domain.JobPostingId;
import com.careerfit.job.structure.domain.JobPostingAnalysisId;
import com.careerfit.job.structure.domain.JobRequirementId;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JpaJobAnalysisResultRepository implements JobAnalysisResultRepository {

    private final SpringDataJobAnalysisResultRepository resultRepository;
    private final SpringDataRequirementMatchResultRepository matchRepository;
    private final SpringDataMatchEvidenceSnapshotRepository evidenceRepository;

    public JpaJobAnalysisResultRepository(
            SpringDataJobAnalysisResultRepository resultRepository,
            SpringDataRequirementMatchResultRepository matchRepository,
            SpringDataMatchEvidenceSnapshotRepository evidenceRepository) {
        this.resultRepository = resultRepository;
        this.matchRepository = matchRepository;
        this.evidenceRepository = evidenceRepository;
    }

    @Override
    @Transactional
    public void save(JobAnalysisResult result) {
        resultRepository.save(new JobAnalysisResultEntity(
                result.id().value(),
                result.userId().value(),
                result.jobPostingId().value(),
                result.candidateSearchId().value(),
                result.workflowVersion(),
                result.completedAt()));
        RequirementMatchResult match = result.match();
        matchRepository.save(new RequirementMatchResultEntity(
                result.id().value(),
                result.userId().value(),
                match.requirementId().value(),
                match.jobPostingAnalysisId().value(),
                match.category(),
                match.requirementText(),
                match.sourceExcerpt(),
                match.sequence(),
                match.status(),
                match.reason()));
        if (match.evidence() != null) {
            CareerEvidenceSnapshot evidence = match.evidence();
            evidenceRepository.save(new MatchEvidenceSnapshotEntity(
                    result.id().value(),
                    result.userId().value(),
                    evidence.experienceVersionId().value(),
                    evidence.sourceType(),
                    evidence.title(),
                    evidence.role(),
                    evidence.responsibilities(),
                    evidence.technologies(),
                    evidence.searchScore(),
                    evidence.searchRank(),
                    evidence.explicitConflict()));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<JobAnalysisResult> find(UserId userId, JobAnalysisResultId resultId) {
        return resultRepository
                .findByIdAndUserId(resultId.value(), userId.value())
                .flatMap(result -> matchRepository
                        .findByResultIdAndUserId(result.id(), userId.value())
                        .map(match -> toDomain(
                                result,
                                match,
                                evidenceRepository
                                        .findByResultIdAndUserId(
                                                result.id(), userId.value())
                                        .orElse(null))));
    }

    private JobAnalysisResult toDomain(
            JobAnalysisResultEntity result,
            RequirementMatchResultEntity match,
            MatchEvidenceSnapshotEntity evidence) {
        CareerEvidenceSnapshot evidenceSnapshot = evidence == null
                ? null
                : new CareerEvidenceSnapshot(
                        new CareerExperienceVersionId(evidence.experienceVersionId()),
                        evidence.sourceType(),
                        evidence.title(),
                        evidence.role(),
                        evidence.responsibilities(),
                        evidence.technologies(),
                        evidence.searchScore(),
                        evidence.searchRank(),
                        evidence.explicitConflict());
        return new JobAnalysisResult(
                new JobAnalysisResultId(result.id()),
                new UserId(result.userId()),
                new JobPostingId(result.jobPostingId()),
                new CareerCandidateSearchId(result.candidateSearchId()),
                result.workflowVersion(),
                result.completedAt(),
                new RequirementMatchResult(
                        new JobRequirementId(match.requirementId()),
                        new JobPostingAnalysisId(match.jobPostingAnalysisId()),
                        match.category(),
                        match.requirementText(),
                        match.sourceExcerpt(),
                        match.sequence(),
                        match.status(),
                        match.reason(),
                        evidenceSnapshot));
    }
}
