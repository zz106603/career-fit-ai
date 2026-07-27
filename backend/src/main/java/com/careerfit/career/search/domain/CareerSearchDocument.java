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
        Instant indexedAt) {

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
        validateIndexState(embedding, embeddingVersion, status, indexedAt);
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
                null);
    }

    private static void validateIndexState(
            List<Double> embedding,
            String embeddingVersion,
            CareerSearchIndexStatus status,
            Instant indexedAt) {
        if (status == CareerSearchIndexStatus.PENDING) {
            if (embedding != null || embeddingVersion != null || indexedAt != null) {
                throw new IllegalArgumentException("PENDING 문서는 embedding을 가질 수 없습니다.");
            }
            return;
        }
        if (embedding == null || embedding.size() != EMBEDDING_DIMENSION) {
            throw new IllegalArgumentException("INDEXED 문서는 8차원 embedding이 필요합니다.");
        }
        if (embeddingVersion == null || embeddingVersion.isBlank() || indexedAt == null) {
            throw new IllegalArgumentException("INDEXED 문서는 embedding 메타데이터가 필요합니다.");
        }
    }
}
