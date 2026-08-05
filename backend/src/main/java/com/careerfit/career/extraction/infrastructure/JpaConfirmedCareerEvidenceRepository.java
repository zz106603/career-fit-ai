package com.careerfit.career.extraction.infrastructure;

import com.careerfit.career.domain.CareerExperienceVersionId;
import com.careerfit.career.extraction.application.ConfirmedCareerEvidenceRepository;
import com.careerfit.identity.UserId;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
/** Spring Data JPA로 후보 Evidence를 확정 버전의 불변 Snapshot으로 복사한다. */
public class JpaConfirmedCareerEvidenceRepository implements ConfirmedCareerEvidenceRepository {
    private final SpringDataExperienceEvidenceRepository candidateEvidences;
    private final SpringDataCareerExperienceEvidenceRepository confirmedEvidences;

    public JpaConfirmedCareerEvidenceRepository(
            SpringDataExperienceEvidenceRepository candidateEvidences,
            SpringDataCareerExperienceEvidenceRepository confirmedEvidences) {
        this.candidateEvidences = candidateEvidences;
        this.confirmedEvidences = confirmedEvidences;
    }

    @Override
    public int copyFromCandidate(UserId userId, UUID candidateId, CareerExperienceVersionId versionId) {
        List<CareerExperienceEvidenceEntity> copies = candidateEvidences
                .findSources(userId.value(), candidateId).stream()
                .map(source -> new CareerExperienceEvidenceEntity(
                        UUID.randomUUID(), versionId.value(), source.candidateId(), source.analysisId(),
                        source.documentId(), source.userId(), source.documentName(),
                        source.pageNumber(), source.excerpt()))
                .toList();
        confirmedEvidences.saveAll(copies);
        return copies.size();
    }

    @Override
    public int copyFromVersion(
            UserId userId, CareerExperienceVersionId source, CareerExperienceVersionId target) {
        List<CareerExperienceEvidenceEntity> copies = confirmedEvidences
                .findAllByUserIdAndVersionId(userId.value(), source.value()).stream()
                .map(evidence -> new CareerExperienceEvidenceEntity(
                        UUID.randomUUID(), target.value(), evidence.candidateId(), evidence.analysisId(),
                        evidence.documentId(), evidence.userId(), evidence.documentName(),
                        evidence.pageNumber(), evidence.excerpt()))
                .toList();
        confirmedEvidences.saveAll(copies);
        return copies.size();
    }

    @Override
    public boolean exists(UserId userId, CareerExperienceVersionId versionId) {
        return confirmedEvidences.existsByUserIdAndVersionId(userId.value(), versionId.value());
    }
}
