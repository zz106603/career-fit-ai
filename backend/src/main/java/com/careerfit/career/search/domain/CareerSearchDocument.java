package com.careerfit.career.search.domain;

import com.careerfit.career.domain.CareerExperienceVersionId;
import com.careerfit.identity.UserId;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** 확정 경력 버전에서 파생된 검색 문서와 Fake embedding이다. */
public record CareerSearchDocument(
        CareerSearchDocumentId id,
        UserId userId,
        CareerExperienceVersionId experienceVersionId,
        String searchableText,
        String contentHash,
        List<Double> embedding,
        String embeddingVersion,
        CareerSearchIndexStatus status,
        Instant createdAt,
        Instant indexingStartedAt,
        Instant indexedAt,
        Instant failedAt,
        String failureCode) {

    public static final int EMBEDDING_DIMENSION = 8;

    public CareerSearchDocument {
        Objects.requireNonNull(id, "id는 null일 수 없습니다.");
        Objects.requireNonNull(userId, "userId는 null일 수 없습니다.");
        Objects.requireNonNull(experienceVersionId, "experienceVersionId는 null일 수 없습니다.");
        if (searchableText == null || searchableText.isBlank()) {
            throw new IllegalArgumentException("searchableText는 필수입니다.");
        }
        if (contentHash == null || contentHash.length() != 64) {
            throw new IllegalArgumentException("contentHash는 SHA-256 형식이어야 합니다.");
        }
        embedding = embedding == null ? null : List.copyOf(embedding);
        Objects.requireNonNull(status, "status는 null일 수 없습니다.");
        Objects.requireNonNull(createdAt, "createdAt은 null일 수 없습니다.");
        failureCode = normalize(failureCode);
        validateIndexState(
                embedding,
                embeddingVersion,
                status,
                indexingStartedAt,
                indexedAt,
                failedAt,
                failureCode);
    }

    public static CareerSearchDocument pending(
            UserId userId,
            CareerExperienceVersionId experienceVersionId,
            String searchableText,
            String contentHash,
            Instant createdAt) {
        return new CareerSearchDocument(
                CareerSearchDocumentId.newId(),
                userId,
                experienceVersionId,
                searchableText,
                contentHash,
                null,
                null,
                CareerSearchIndexStatus.PENDING,
                createdAt,
                null,
                null,
                null,
                null);
    }

    public CareerSearchDocument start(Instant startedAt) {
        requireStatus(CareerSearchIndexStatus.PENDING, CareerSearchIndexStatus.INDEXING);
        return copy(null, null, CareerSearchIndexStatus.INDEXING, startedAt, null, null, null);
    }

    public CareerSearchDocument succeed(
            List<Double> embedding, String embeddingVersion, Instant indexedAt) {
        requireStatus(CareerSearchIndexStatus.INDEXING, CareerSearchIndexStatus.INDEXED);
        return copy(
                embedding,
                embeddingVersion,
                CareerSearchIndexStatus.INDEXED,
                indexingStartedAt,
                indexedAt,
                null,
                null);
    }

    public CareerSearchDocument fail(String failureCode, Instant failedAt) {
        requireStatus(CareerSearchIndexStatus.INDEXING, CareerSearchIndexStatus.FAILED);
        return copy(
                null,
                null,
                CareerSearchIndexStatus.FAILED,
                indexingStartedAt,
                null,
                failedAt,
                failureCode);
    }

    private CareerSearchDocument copy(
            List<Double> embedding,
            String embeddingVersion,
            CareerSearchIndexStatus status,
            Instant indexingStartedAt,
            Instant indexedAt,
            Instant failedAt,
            String failureCode) {
        return new CareerSearchDocument(
                id,
                userId,
                experienceVersionId,
                searchableText,
                contentHash,
                embedding,
                embeddingVersion,
                status,
                createdAt,
                indexingStartedAt,
                indexedAt,
                failedAt,
                failureCode);
    }

    private static void validateIndexState(
            List<Double> embedding,
            String embeddingVersion,
            CareerSearchIndexStatus status,
            Instant indexingStartedAt,
            Instant indexedAt,
            Instant failedAt,
            String failureCode) {
        if (status == CareerSearchIndexStatus.PENDING) {
            if (embedding != null
                    || embeddingVersion != null
                    || indexingStartedAt != null
                    || indexedAt != null
                    || failedAt != null
                    || failureCode != null) {
                throw new IllegalArgumentException("PENDING 문서는 embedding을 가질 수 없습니다.");
            }
            return;
        }
        if (indexingStartedAt == null) {
            throw new IllegalArgumentException("색인을 시작한 문서는 시작 시각이 필요합니다.");
        }
        if (status == CareerSearchIndexStatus.INDEXING) {
            if (embedding != null
                    || embeddingVersion != null
                    || indexedAt != null
                    || failedAt != null
                    || failureCode != null) {
                throw new IllegalArgumentException("INDEXING 문서는 완료 정보를 가질 수 없습니다.");
            }
            return;
        }
        if (status == CareerSearchIndexStatus.FAILED) {
            if (embedding != null
                    || embeddingVersion != null
                    || indexedAt != null
                    || failedAt == null
                    || failureCode == null) {
                throw new IllegalArgumentException("FAILED 문서는 실패 시각과 코드가 필요합니다.");
            }
            return;
        }
        if (embedding == null || embedding.size() != EMBEDDING_DIMENSION) {
            throw new IllegalArgumentException("INDEXED 문서는 8차원 embedding이 필요합니다.");
        }
        if (embeddingVersion == null
                || embeddingVersion.isBlank()
                || indexedAt == null
                || failedAt != null
                || failureCode != null) {
            throw new IllegalArgumentException("INDEXED 문서는 embedding 메타데이터가 필요합니다.");
        }
    }

    private void requireStatus(
            CareerSearchIndexStatus expected, CareerSearchIndexStatus target) {
        if (status != expected) {
            throw new IllegalStateException(status + "에서 " + target + " 상태로 전이할 수 없습니다.");
        }
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
