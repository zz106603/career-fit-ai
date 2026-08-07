package com.careerfit.career.extraction.application;

import com.careerfit.career.document.domain.CareerDocumentAnalysisId;
import com.careerfit.career.extraction.domain.CareerExtractionCandidate;
import com.careerfit.career.extraction.domain.ExperienceEvidence;
import com.careerfit.identity.UserId;
import java.util.List;
import java.util.UUID;

public interface CareerExtractionCandidateRepository {
    boolean exists(UserId userId, CareerDocumentAnalysisId analysisId);
    void saveAll(List<CareerExtractionCandidate> candidates, List<ExperienceEvidence> evidences);
    List<CareerExtractionCandidate> findAll(UserId userId, CareerDocumentAnalysisId analysisId);
    List<CareerExtractionCandidate> findAllForUpdate(UserId userId, List<UUID> candidateIds);
    List<ExperienceEvidence> findEvidences(UserId userId, List<UUID> candidateIds);
    List<CareerCandidateEvidenceView> findEvidenceViews(UserId userId, List<UUID> candidateIds);
}
