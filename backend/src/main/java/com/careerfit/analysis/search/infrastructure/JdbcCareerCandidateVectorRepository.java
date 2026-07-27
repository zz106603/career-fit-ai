package com.careerfit.analysis.search.infrastructure;

import com.careerfit.analysis.search.application.CareerCandidateVectorRepository;
import com.careerfit.analysis.search.domain.CareerSearchCandidate;
import com.careerfit.career.domain.CareerExperienceVersionId;
import com.careerfit.career.search.domain.CareerSearchDocument;
import com.careerfit.identity.UserId;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcCareerCandidateVectorRepository implements CareerCandidateVectorRepository {

    private final JdbcClient jdbcClient;

    public JdbcCareerCandidateVectorRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public List<CareerSearchCandidate> searchActiveIndexed(
            UserId userId, List<Double> queryEmbedding, int limit) {
        if (queryEmbedding.size() != CareerSearchDocument.EMBEDDING_DIMENSION) {
            throw new IllegalArgumentException("검색 embedding은 8차원이어야 합니다.");
        }
        if (limit < 1) {
            throw new IllegalArgumentException("검색 후보 수는 1 이상이어야 합니다.");
        }
        return jdbcClient
                .sql("""
                        SELECT document.experience_version_id,
                               1 - (document.embedding <=> CAST(:embedding AS vector)) AS score,
                               ROW_NUMBER() OVER (
                                   ORDER BY document.embedding <=> CAST(:embedding AS vector),
                                            document.experience_version_id
                               ) AS candidate_rank,
                               document.embedding_version
                        FROM career_search_document document
                        JOIN career_experience_version version
                          ON version.experience_version_id =
                             document.experience_version_id
                         AND version.user_id = document.user_id
                        JOIN career_experience experience
                          ON experience.experience_id = version.experience_id
                         AND experience.user_id = version.user_id
                        WHERE document.user_id = :userId
                          AND document.index_status = 'INDEXED'
                          AND version.confirmed_at IS NOT NULL
                          AND version.superseded_at IS NULL
                          AND version.deleted_at IS NULL
                          AND experience.deleted_at IS NULL
                        ORDER BY candidate_rank
                        LIMIT :limit
                        """)
                .param("embedding", vectorLiteral(queryEmbedding))
                .param("userId", userId.value())
                .param("limit", limit)
                .query(this::map)
                .list();
    }

    private CareerSearchCandidate map(ResultSet resultSet, int rowNumber)
            throws SQLException {
        return new CareerSearchCandidate(
                new CareerExperienceVersionId(
                        resultSet.getObject("experience_version_id", UUID.class)),
                resultSet.getDouble("score"),
                resultSet.getInt("candidate_rank"),
                resultSet.getString("embedding_version"));
    }

    private String vectorLiteral(List<Double> embedding) {
        return embedding.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(",", "[", "]"));
    }
}
