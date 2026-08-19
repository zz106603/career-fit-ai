package com.careerfit.career.search.application;

import com.careerfit.career.application.CareerExperienceRepository;
import com.careerfit.career.domain.CareerExperienceVersion;
import com.careerfit.career.domain.CareerExperienceVersionId;
import com.careerfit.career.search.domain.CareerSearchDocument;
import com.careerfit.career.search.domain.CareerSearchIndexStatus;
import com.careerfit.identity.UserId;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
/** 색인 상태 변경을 짧은 트랜잭션으로 분리해 Provider 호출 중 DB 연결을 점유하지 않는다. */
public class CareerIndexingPersistence {

    private final CareerExperienceRepository experienceRepository;
    private final CareerSearchDocumentRepository searchDocumentRepository;
    private final Clock clock;

    public CareerIndexingPersistence(
            CareerExperienceRepository experienceRepository,
            CareerSearchDocumentRepository searchDocumentRepository,
            Clock clock) {
        this.experienceRepository = experienceRepository;
        this.searchDocumentRepository = searchDocumentRepository;
        this.clock = clock;
    }

    @Transactional
    public Optional<CareerIndexingInput> start(
            UserId userId,
            CareerExperienceVersionId versionId,
            String inputVersion) {
        CareerExperienceVersion version = experienceRepository
                .findConfirmedVersion(userId, versionId)
                .orElseThrow(CareerVersionNotIndexableException::new);
        if (!Integer.toString(version.versionNo()).equals(inputVersion)) {
            throw new IllegalStateException("색인 작업 입력 버전이 확정 경력 버전과 일치하지 않습니다.");
        }

        CareerSearchDocument document = find(userId, versionId);
        if (document.status() == CareerSearchIndexStatus.INDEXED) {
            return Optional.empty();
        }
        CareerSearchDocument indexing = document.start(clock.instant());
        if (!searchDocumentRepository.markIndexing(
                userId, versionId, indexing.indexingStartedAt())) {
            throw new IllegalStateException("검색 문서를 INDEXING 상태로 변경하지 못했습니다.");
        }
        return Optional.of(new CareerIndexingInput(versionId, document.searchableText()));
    }

    @Transactional
    public CareerSearchDocument complete(
            UserId userId,
            CareerExperienceVersionId versionId,
            List<Double> embedding,
            String embeddingVersion) {
        CareerSearchDocument indexed = find(userId, versionId)
                .succeed(embedding, embeddingVersion, clock.instant());
        if (!searchDocumentRepository.markIndexed(
                userId,
                versionId,
                indexed.embedding(),
                indexed.embeddingVersion(),
                indexed.indexedAt())) {
            throw new IllegalStateException("검색 문서를 INDEXED 상태로 변경하지 못했습니다.");
        }
        return find(userId, versionId);
    }

    @Transactional
    public void fail(
            UserId userId,
            CareerExperienceVersionId versionId,
            String failureCode) {
        CareerSearchDocument failed = find(userId, versionId).fail(failureCode, clock.instant());
        if (!searchDocumentRepository.markFailed(
                userId, versionId, failed.failureCode(), failed.failedAt())) {
            throw new IllegalStateException("검색 문서를 FAILED 상태로 변경하지 못했습니다.");
        }
    }

    @Transactional(readOnly = true)
    public CareerSearchDocument find(UserId userId, CareerExperienceVersionId versionId) {
        return searchDocumentRepository
                .findByExperienceVersion(userId, versionId)
                .orElseThrow(() -> new IllegalStateException("경력 검색 문서를 찾을 수 없습니다."));
    }
}
