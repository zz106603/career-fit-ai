package com.careerfit.analysis.search.infrastructure;

import com.careerfit.analysis.search.application.CareerCandidateSearchRepository;
import com.careerfit.analysis.search.domain.CareerCandidateSearch;
import com.careerfit.analysis.search.domain.CareerCandidateSearchId;
import com.careerfit.analysis.search.domain.CareerSearchCandidate;
import com.careerfit.career.domain.CareerExperienceVersionId;
import com.careerfit.identity.UserId;
import com.careerfit.job.structure.domain.JobRequirementId;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JpaCareerCandidateSearchRepository
        implements CareerCandidateSearchRepository {

    private final SpringDataCareerCandidateSearchRepository searchRepository;
    private final SpringDataCareerSearchCandidateSnapshotRepository candidateRepository;

    public JpaCareerCandidateSearchRepository(
            SpringDataCareerCandidateSearchRepository searchRepository,
            SpringDataCareerSearchCandidateSnapshotRepository candidateRepository) {
        this.searchRepository = searchRepository;
        this.candidateRepository = candidateRepository;
    }

    @Override
    @Transactional
    public void save(CareerCandidateSearch search) {
        searchRepository.save(new CareerCandidateSearchEntity(
                search.id().value(),
                search.userId().value(),
                search.requirementId().value(),
                search.queryEmbeddingVersion(),
                search.searchVersion(),
                search.searchedAt()));
        List<CareerSearchCandidateSnapshotEntity> candidates = search.candidates().stream()
                .map(candidate -> new CareerSearchCandidateSnapshotEntity(
                        new CareerSearchCandidateSnapshotId(
                                search.id().value(), candidate.rank()),
                        search.userId().value(),
                        candidate.experienceVersionId().value(),
                        candidate.score(),
                        candidate.embeddingVersion()))
                .toList();
        candidateRepository.saveAll(candidates);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CareerCandidateSearch> find(
            UserId userId, CareerCandidateSearchId searchId) {
        return searchRepository
                .findByIdAndUserId(searchId.value(), userId.value())
                .map(search -> new CareerCandidateSearch(
                        new CareerCandidateSearchId(search.id()),
                        new UserId(search.userId()),
                        new JobRequirementId(search.requirementId()),
                        search.queryEmbeddingVersion(),
                        search.searchVersion(),
                        search.searchedAt(),
                        candidateRepository
                                .findByIdSearchIdAndUserIdOrderByIdRank(
                                        search.id(), userId.value())
                                .stream()
                                .map(candidate -> new CareerSearchCandidate(
                                        new CareerExperienceVersionId(
                                                candidate.experienceVersionId()),
                                        candidate.score(),
                                        candidate.rank(),
                                        candidate.embeddingVersion()))
                                .toList()));
    }
}
