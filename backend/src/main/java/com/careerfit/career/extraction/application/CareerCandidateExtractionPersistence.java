package com.careerfit.career.extraction.application;

import com.careerfit.career.document.application.CareerDocumentAnalysisRepository;
import com.careerfit.career.document.domain.CareerDocumentAnalysis;
import com.careerfit.career.extraction.domain.CareerExtractionCandidate;
import com.careerfit.career.extraction.domain.ExperienceEvidence;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CareerCandidateExtractionPersistence {
    private final CareerExtractionCandidateRepository candidates;
    private final CareerDocumentAnalysisRepository analyses;
    private final Clock clock;

    public CareerCandidateExtractionPersistence(CareerExtractionCandidateRepository candidates,
            CareerDocumentAnalysisRepository analyses, Clock clock) {
        this.candidates = candidates; this.analyses = analyses; this.clock = clock;
    }

    @Transactional
    public void save(CareerDocumentAnalysis analysis, List<CareerExtractionCandidate> values,
            List<ExperienceEvidence> evidences) {
        if (candidates.exists(analysis.userId(), analysis.id())) return;
        candidates.saveAll(values, evidences);
        analyses.save(analysis.succeed(clock.instant()));
    }

    @Transactional
    public void fail(CareerDocumentAnalysis analysis, String failureCode) {
        analyses.save(analysis.fail(failureCode, clock.instant()));
    }
}
