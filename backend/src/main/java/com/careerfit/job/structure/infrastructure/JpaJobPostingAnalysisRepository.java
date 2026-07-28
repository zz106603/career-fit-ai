package com.careerfit.job.structure.infrastructure;

import com.careerfit.identity.UserId;
import com.careerfit.job.domain.JobPostingId;
import com.careerfit.job.structure.application.JobPostingAnalysisRepository;
import com.careerfit.job.structure.domain.JobPostingAnalysis;
import com.careerfit.job.structure.domain.JobPostingAnalysisId;
import com.careerfit.job.structure.domain.JobPostingAnalysisStatus;
import com.careerfit.job.structure.domain.JobRequirement;
import com.careerfit.job.structure.domain.JobRequirementId;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JpaJobPostingAnalysisRepository implements JobPostingAnalysisRepository {

    private final SpringDataJobPostingAnalysisRepository analysisRepository;
    private final SpringDataJobRequirementRepository requirementRepository;

    public JpaJobPostingAnalysisRepository(
            SpringDataJobPostingAnalysisRepository analysisRepository,
            SpringDataJobRequirementRepository requirementRepository) {
        this.analysisRepository = analysisRepository;
        this.requirementRepository = requirementRepository;
    }

    @Override
    @Transactional
    public void saveReady(JobPostingAnalysis analysis) {
        if (analysis.status() != JobPostingAnalysisStatus.READY) {
            throw new IllegalArgumentException("READY 구조화 결과만 저장할 수 있습니다.");
        }
        analysisRepository.save(new JobPostingAnalysisEntity(
                analysis.id().value(),
                analysis.jobPostingId().value(),
                analysis.userId().value(),
                analysis.status(),
                analysis.companyName(),
                analysis.jobTitle(),
                analysis.workflowVersion(),
                analysis.createdAt(),
                analysis.readyAt()));
        JobRequirement requirement = analysis.requirement();
        requirementRepository.save(new JobRequirementEntity(
                requirement.id().value(),
                analysis.id().value(),
                analysis.userId().value(),
                requirement.category(),
                requirement.text(),
                requirement.sourceExcerpt(),
                requirement.sequence()));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<JobPostingAnalysis> findLatestReady(
            UserId userId, JobPostingId jobPostingId) {
        return analysisRepository
                .findFirstByUserIdAndJobPostingIdAndStatusOrderByReadyAtDescIdDesc(
                        userId.value(),
                        jobPostingId.value(),
                        JobPostingAnalysisStatus.READY)
                .flatMap(analysis -> mapWithRequirement(analysis, userId));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<JobPostingAnalysis> findReadyByRequirement(
            UserId userId, JobRequirementId requirementId) {
        return requirementRepository
                .findByIdAndUserId(requirementId.value(), userId.value())
                .flatMap(requirement -> analysisRepository.findById(requirement.analysisId())
                        .filter(analysis -> analysis.userId().equals(userId.value()))
                        .filter(analysis -> analysis.status() == JobPostingAnalysisStatus.READY)
                        .map(analysis -> toDomain(analysis, requirement)));
    }

    private Optional<JobPostingAnalysis> mapWithRequirement(
            JobPostingAnalysisEntity analysis, UserId userId) {
        return requirementRepository
                .findByAnalysisIdAndUserId(analysis.id(), userId.value())
                .map(requirement -> toDomain(analysis, requirement));
    }

    private JobPostingAnalysis toDomain(
            JobPostingAnalysisEntity analysis, JobRequirementEntity requirement) {
        JobPostingAnalysisId analysisId = new JobPostingAnalysisId(analysis.id());
        return new JobPostingAnalysis(
                analysisId,
                new JobPostingId(analysis.jobPostingId()),
                new UserId(analysis.userId()),
                analysis.status(),
                analysis.companyName(),
                analysis.jobTitle(),
                analysis.workflowVersion(),
                analysis.createdAt(),
                analysis.readyAt(),
                new JobRequirement(
                        new JobRequirementId(requirement.id()),
                        analysisId,
                        requirement.category(),
                        requirement.text(),
                        requirement.sourceExcerpt(),
                        requirement.sequence()));
    }
}
