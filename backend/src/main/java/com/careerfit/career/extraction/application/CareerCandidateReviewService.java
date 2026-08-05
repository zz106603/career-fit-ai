package com.careerfit.career.extraction.application;

import com.careerfit.career.extraction.domain.CareerCandidateContent;
import com.careerfit.career.extraction.domain.CareerExtractionCandidate;
import com.careerfit.career.extraction.domain.CareerExtractionCandidateStatus;
import com.careerfit.career.extraction.domain.ExperienceEvidence;
import com.careerfit.identity.CurrentUserProvider;
import com.careerfit.identity.UserId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
/** 사용자가 AI 후보를 확정 전에 수정·거절·병합·분리하도록 조정하는 검토 유스케이스다. */
public class CareerCandidateReviewService {
    private final CareerExtractionCandidateRepository repository;
    private final CurrentUserProvider currentUserProvider;

    public CareerCandidateReviewService(
            CareerExtractionCandidateRepository repository,
            CurrentUserProvider currentUserProvider) {
        this.repository = repository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional
    public CareerExtractionCandidate edit(UUID candidateId, CareerCandidateContent content) {
        CareerExtractionCandidate edited = requireOne(candidateId).edit(content);
        repository.saveAll(List.of(edited), List.of());
        return edited;
    }

    @Transactional
    public void reject(UUID candidateId) {
        repository.saveAll(List.of(requireOne(candidateId).reject()), List.of());
    }

    @Transactional
    /** 병합 결과는 새 EDITED 후보로 만들고 원본 후보는 계보 보존을 위해 REJECTED로 남긴다. */
    public CareerExtractionCandidate merge(List<UUID> candidateIds, CareerCandidateContent content) {
        if (candidateIds == null || new HashSet<>(candidateIds).size() < 2) {
            throw new IllegalArgumentException("병합 후보는 2개 이상이어야 합니다.");
        }
        UserId userId = currentUserProvider.currentUserId();
        List<CareerExtractionCandidate> sources = requireAll(userId, candidateIds);
        requireEditable(sources);
        CareerExtractionCandidate merged = derivedFrom(sources.getFirst(), userId, content);
        List<ExperienceEvidence> copied = copyDistinctEvidences(userId, candidateIds, merged.id());
        repository.saveAll(withRejectedSources(sources, merged), copied);
        return merged;
    }

    @Transactional
    /** 분리 결과마다 원본 Evidence를 복제하고 원본 후보는 중복 확정을 막기 위해 거절한다. */
    public List<CareerExtractionCandidate> split(UUID candidateId, List<CareerCandidateContent> contents) {
        if (contents == null || contents.size() < 2) {
            throw new IllegalArgumentException("분리 결과는 2개 이상이어야 합니다.");
        }
        UserId userId = currentUserProvider.currentUserId();
        CareerExtractionCandidate source = requireOne(candidateId);
        requireEditable(List.of(source));
        List<ExperienceEvidence> sourceEvidences = repository.findEvidences(userId, List.of(candidateId));
        List<CareerExtractionCandidate> results = contents.stream()
                .map(content -> derivedFrom(source, userId, content))
                .toList();
        List<ExperienceEvidence> copied = results.stream()
                .flatMap(result -> sourceEvidences.stream().map(evidence -> copy(evidence, result.id())))
                .toList();
        repository.saveAll(withRejectedSources(List.of(source), results), copied);
        return results;
    }

    private CareerExtractionCandidate requireOne(UUID candidateId) {
        UserId userId = currentUserProvider.currentUserId();
        return requireAll(userId, List.of(candidateId)).getFirst();
    }

    private List<CareerExtractionCandidate> requireAll(UserId userId, List<UUID> candidateIds) {
        List<CareerExtractionCandidate> found = repository.findAllForUpdate(userId, candidateIds);
        if (found.size() != new HashSet<>(candidateIds).size()) throw new CareerCandidateNotFoundException();
        return found;
    }

    private static void requireEditable(List<CareerExtractionCandidate> candidates) {
        if (candidates.stream().anyMatch(candidate -> !candidate.isEditable())) {
            throw new IllegalStateException("검토 가능한 후보만 변경할 수 있습니다.");
        }
    }

    private static CareerExtractionCandidate derivedFrom(
            CareerExtractionCandidate source, UserId userId, CareerCandidateContent content) {
        return new CareerExtractionCandidate(UUID.randomUUID(), source.analysisId(), userId,
                content.candidateType(), content.organization(), content.role(), content.period(),
                content.description(), CareerExtractionCandidateStatus.EDITED, 1, source.model(),
                source.promptVersion(), source.schemaVersion(), source.aiCallExecutionId(), source.createdAt());
    }

    private List<ExperienceEvidence> copyDistinctEvidences(
            UserId userId, List<UUID> sourceIds, UUID targetId) {
        Set<String> seen = new HashSet<>();
        return repository.findEvidences(userId, sourceIds).stream()
                .filter(e -> seen.add(e.analysisId().value() + ":" + e.documentId().value() + ":"
                        + e.pageNumber() + ":" + e.excerpt()))
                .map(e -> copy(e, targetId))
                .toList();
    }

    private static ExperienceEvidence copy(ExperienceEvidence evidence, UUID candidateId) {
        return new ExperienceEvidence(UUID.randomUUID(), candidateId, evidence.analysisId(),
                evidence.documentId(), evidence.userId(), evidence.pageNumber(), evidence.excerpt());
    }

    private static List<CareerExtractionCandidate> withRejectedSources(
            List<CareerExtractionCandidate> sources, CareerExtractionCandidate result) {
        return withRejectedSources(sources, List.of(result));
    }

    private static List<CareerExtractionCandidate> withRejectedSources(
            List<CareerExtractionCandidate> sources, List<CareerExtractionCandidate> results) {
        java.util.ArrayList<CareerExtractionCandidate> changed = new java.util.ArrayList<>(results);
        changed.addAll(sources.stream().map(CareerExtractionCandidate::reject).toList());
        return changed;
    }
}
