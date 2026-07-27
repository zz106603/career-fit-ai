package com.careerfit.career.search.infrastructure;

import com.careerfit.career.domain.CareerExperienceVersionId;
import com.careerfit.career.search.application.CareerSearchDocumentRepository;
import com.careerfit.career.search.domain.CareerSearchDocument;
import com.careerfit.career.search.domain.CareerSearchDocumentId;
import com.careerfit.career.search.domain.CareerSearchIndexStatus;
import com.careerfit.identity.UserId;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcCareerSearchDocumentRepository implements CareerSearchDocumentRepository {

    private final JdbcClient jdbcClient;

    public JdbcCareerSearchDocumentRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public void savePending(CareerSearchDocument document) {
        jdbcClient
                .sql("""
                        INSERT INTO career_search_document (
                            search_document_id, user_id, experience_version_id,
                            searchable_text, content_hash, index_status, created_at
                        ) VALUES (
                            :documentId, :userId, :versionId,
                            :searchableText, :contentHash, 'PENDING', :createdAt
                        )
                        ON CONFLICT (user_id, experience_version_id) DO NOTHING
                        """)
                .param("documentId", document.id().value())
                .param("userId", document.userId().value())
                .param("versionId", document.experienceVersionId().value())
                .param("searchableText", document.searchableText())
                .param("contentHash", document.contentHash())
                .param("createdAt", atUtc(document.createdAt()))
                .update();
    }

    @Override
    public Optional<CareerSearchDocument> findByExperienceVersion(
            UserId userId, CareerExperienceVersionId experienceVersionId) {
        return jdbcClient
                .sql("""
                        SELECT search_document_id, user_id, experience_version_id,
                               searchable_text, content_hash, embedding::text AS embedding,
                               embedding_version, index_status, created_at, indexed_at
                        FROM career_search_document
                        WHERE user_id = :userId
                          AND experience_version_id = :versionId
                        """)
                .param("userId", userId.value())
                .param("versionId", experienceVersionId.value())
                .query(this::mapDocument)
                .optional();
    }

    @Override
    public boolean markIndexed(
            UserId userId,
            CareerExperienceVersionId experienceVersionId,
            List<Double> embedding,
            String embeddingVersion,
            Instant indexedAt) {
        if (embedding.size() != CareerSearchDocument.EMBEDDING_DIMENSION) {
            throw new IllegalArgumentException("Fake embedding은 8차원이어야 합니다.");
        }
        int updated = jdbcClient
                .sql("""
                        UPDATE career_search_document
                        SET embedding = CAST(:embedding AS vector),
                            embedding_version = :embeddingVersion,
                            index_status = 'INDEXED',
                            indexed_at = :indexedAt
                        WHERE user_id = :userId
                          AND experience_version_id = :versionId
                          AND index_status = 'PENDING'
                        """)
                .param("embedding", vectorLiteral(embedding))
                .param("embeddingVersion", embeddingVersion)
                .param("indexedAt", atUtc(indexedAt))
                .param("userId", userId.value())
                .param("versionId", experienceVersionId.value())
                .update();
        return updated == 1;
    }

    private CareerSearchDocument mapDocument(ResultSet resultSet, int rowNumber)
            throws SQLException {
        return new CareerSearchDocument(
                new CareerSearchDocumentId(resultSet.getObject("search_document_id", UUID.class)),
                new UserId(resultSet.getObject("user_id", UUID.class)),
                new CareerExperienceVersionId(
                        resultSet.getObject("experience_version_id", UUID.class)),
                resultSet.getString("searchable_text"),
                resultSet.getString("content_hash"),
                parseVector(resultSet.getString("embedding")),
                resultSet.getString("embedding_version"),
                CareerSearchIndexStatus.valueOf(resultSet.getString("index_status")),
                instant(resultSet, "created_at"),
                nullableInstant(resultSet, "indexed_at"));
    }

    private List<Double> parseVector(String value) {
        if (value == null) {
            return null;
        }
        String content = value.substring(1, value.length() - 1);
        return Arrays.stream(content.split(",")).map(Double::valueOf).toList();
    }

    private String vectorLiteral(List<Double> embedding) {
        return embedding.stream()
                .map(String::valueOf)
                .collect(java.util.stream.Collectors.joining(",", "[", "]"));
    }

    private Instant instant(ResultSet resultSet, String column) throws SQLException {
        return resultSet.getObject(column, OffsetDateTime.class).toInstant();
    }

    private Instant nullableInstant(ResultSet resultSet, String column) throws SQLException {
        OffsetDateTime value = resultSet.getObject(column, OffsetDateTime.class);
        return value == null ? null : value.toInstant();
    }

    private OffsetDateTime atUtc(Instant value) {
        return value.atOffset(ZoneOffset.UTC);
    }
}
