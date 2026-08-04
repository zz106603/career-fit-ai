package com.careerfit.career.extraction.infrastructure;

import com.careerfit.career.document.domain.CareerDocumentAnalysisId;
import com.careerfit.career.extraction.application.CareerExtractionCandidateRepository;
import com.careerfit.career.extraction.domain.CareerExtractionCandidate;
import com.careerfit.career.extraction.domain.ExperienceEvidence;
import com.careerfit.identity.UserId;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class JpaCareerExtractionCandidateRepository implements CareerExtractionCandidateRepository {
    private final SpringDataCareerExtractionCandidateRepository candidates;
    private final SpringDataExperienceEvidenceRepository evidences;

    public JpaCareerExtractionCandidateRepository(SpringDataCareerExtractionCandidateRepository candidates,
            SpringDataExperienceEvidenceRepository evidences) {
        this.candidates=candidates; this.evidences=evidences;
    }

    @Override
    public boolean exists(UserId userId, CareerDocumentAnalysisId analysisId) {
        return candidates.existsByUserIdAndAnalysisId(userId.value(), analysisId.value());
    }

    @Override
    public void saveAll(List<CareerExtractionCandidate> values, List<ExperienceEvidence> evidenceValues) {
        candidates.saveAll(values.stream().map(v -> new CareerExtractionCandidateEntity(v.id(),
                v.analysisId().value(), v.userId().value(), v.candidateType(), v.organization(), v.role(),
                v.period(), v.description(), v.status(), v.revisionNo(), v.model(), v.promptVersion(),
                v.schemaVersion(), v.aiCallExecutionId(), v.createdAt())).toList());
        evidences.saveAll(evidenceValues.stream().map(v -> new ExperienceEvidenceEntity(v.id(),
                v.candidateId(), v.analysisId().value(), v.documentId().value(), v.userId().value(),
                v.pageNumber(), v.excerpt())).toList());
    }
}
