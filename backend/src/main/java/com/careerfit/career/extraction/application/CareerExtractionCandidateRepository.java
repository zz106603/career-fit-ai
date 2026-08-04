package com.careerfit.career.extraction.application;

import com.careerfit.career.document.domain.CareerDocumentAnalysisId;
import com.careerfit.career.extraction.domain.CareerExtractionCandidate;
import com.careerfit.career.extraction.domain.ExperienceEvidence;
import com.careerfit.identity.UserId;
import java.util.List;

public interface CareerExtractionCandidateRepository {
    boolean exists(UserId userId, CareerDocumentAnalysisId analysisId);
    void saveAll(List<CareerExtractionCandidate> candidates, List<ExperienceEvidence> evidences);
}
