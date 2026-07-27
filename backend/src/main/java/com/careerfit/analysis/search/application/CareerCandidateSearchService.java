package com.careerfit.analysis.search.application;

import com.careerfit.ai.port.EmbeddingProviderPort;
import com.careerfit.ai.port.model.EmbeddingRequest;
import com.careerfit.ai.port.model.EmbeddingResponse;
import com.careerfit.analysis.search.domain.CareerCandidateSearch;
import com.careerfit.analysis.search.domain.CareerCandidateSearchId;
import com.careerfit.analysis.search.domain.CareerSearchCandidate;
import com.careerfit.identity.CurrentUserProvider;
import com.careerfit.identity.UserId;
import com.careerfit.job.domain.JobPostingId;
import com.careerfit.job.structure.application.JobPostingAnalysisRepository;
import com.careerfit.job.structure.domain.JobPostingAnalysis;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CareerCandidateSearchService {

    private static final int M0_CANDIDATE_LIMIT = 10;
    private static final String SEARCH_VERSION = "pgvector-cosine-v1";

    private final JobPostingAnalysisRepository jobPostingAnalysisRepository;
    private final CareerCandidateVectorRepository vectorRepository;
    private final CareerCandidateSearchRepository searchRepository;
    private final EmbeddingProviderPort embeddingProviderPort;
    private final CurrentUserProvider currentUserProvider;
    private final Clock clock;

    public CareerCandidateSearchService(
            JobPostingAnalysisRepository jobPostingAnalysisRepository,
            CareerCandidateVectorRepository vectorRepository,
            CareerCandidateSearchRepository searchRepository,
            EmbeddingProviderPort embeddingProviderPort,
            CurrentUserProvider currentUserProvider,
            Clock clock) {
        this.jobPostingAnalysisRepository = jobPostingAnalysisRepository;
        this.vectorRepository = vectorRepository;
        this.searchRepository = searchRepository;
        this.embeddingProviderPort = embeddingProviderPort;
        this.currentUserProvider = currentUserProvider;
        this.clock = clock;
    }

    public CareerCandidateSearch search(JobPostingId jobPostingId) {
        UserId userId = currentUserProvider.currentUserId();
        JobPostingAnalysis analysis = jobPostingAnalysisRepository
                .findLatestReady(userId, jobPostingId)
                .orElseThrow(CareerCandidateSearchNotFoundException::new);
        EmbeddingResponse embedding = embeddingProviderPort.embed(
                new EmbeddingRequest(analysis.requirement().text()));
        List<CareerSearchCandidate> candidates = vectorRepository.searchActiveIndexed(
                userId, embedding.vector(), M0_CANDIDATE_LIMIT);
        CareerCandidateSearch search = new CareerCandidateSearch(
                CareerCandidateSearchId.newId(),
                userId,
                analysis.requirement().id(),
                embedding.model(),
                SEARCH_VERSION,
                clock.instant(),
                candidates);
        searchRepository.save(search);
        return search;
    }

    public CareerCandidateSearch find(CareerCandidateSearchId searchId) {
        return searchRepository
                .find(currentUserProvider.currentUserId(), searchId)
                .orElseThrow(CareerCandidateSearchNotFoundException::new);
    }
}
