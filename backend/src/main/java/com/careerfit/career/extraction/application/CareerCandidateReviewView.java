package com.careerfit.career.extraction.application;

import com.careerfit.career.extraction.domain.CareerExtractionCandidate;
import java.util.List;

public record CareerCandidateReviewView(
        CareerExtractionCandidate candidate, List<CareerCandidateEvidenceView> evidences) {}
