package com.careerfit.analysis.search.infrastructure;

import com.careerfit.analysis.search.application.CareerCandidateSearchRepository;
import com.careerfit.analysis.search.domain.CareerCandidateSearch;
import com.careerfit.analysis.search.domain.CareerCandidateSearchId;
import com.careerfit.analysis.search.domain.CareerSearchCandidate;
import com.careerfit.career.domain.CareerExperienceVersionId;
import com.careerfit.identity.UserId;
import com.careerfit.job.structure.domain.JobRequirementId;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JdbcCareerCandidateSearchRepository implements CareerCandidateSearchRepository {

    private final JdbcClient jdbcClient;

    public JdbcCareerCandidateSearchRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    @Transactional
    public void save(CareerCandidateSearch search) {
        jdbcClient
                .sql("""
                        INSERT INTO career_candidate_search (
                            candidate_search_id, user_id, requirement_id,
                            query_embedding_version, search_version, searched_at
                        ) VALUES (
                            :searchId, :userId, :requirementId,
                            :queryEmbeddingVersion, :searchVersion, :searchedAt
                        )
                        """)
                .param("searchId", search.id().value())
                .param("userId", search.userId().value())
                .param("requirementId", search.requirementId().value())
                .param("queryEmbeddingVersion", search.queryEmbeddingVersion())
                .param("searchVersion", search.searchVersion())
                .param("searchedAt", search.searchedAt().atOffset(java.time.ZoneOffset.UTC))
                .update();

        for (CareerSearchCandidate candidate : search.candidates()) {
            jdbcClient
                    .sql("""
                            INSERT INTO career_search_candidate_snapshot (
                                candidate_search_id, user_id, experience_version_id,
                                score, candidate_rank, embedding_version
                            ) VALUES (
                                :searchId, :userId, :experienceVersionId,
                                :score, :candidateRank, :embeddingVersion
                            )
                            """)
                    .param("searchId", search.id().value())
                    .param("userId", search.userId().value())
                    .param("experienceVersionId", candidate.experienceVersionId().value())
                    .param("score", candidate.score())
                    .param("candidateRank", candidate.rank())
                    .param("embeddingVersion", candidate.embeddingVersion())
                    .update();
        }
    }

    @Override
    public Optional<CareerCandidateSearch> find(
            UserId userId, CareerCandidateSearchId searchId) {
        Optional<SearchRow> search = jdbcClient
                .sql("""
                        SELECT candidate_search_id, user_id, requirement_id,
                               query_embedding_version, search_version, searched_at
                        FROM career_candidate_search
                        WHERE candidate_search_id = :searchId
                          AND user_id = :userId
                        """)
                .param("searchId", searchId.value())
                .param("userId", userId.value())
                .query(this::mapSearch)
                .optional();
        if (search.isEmpty()) {
            return Optional.empty();
        }
        List<CareerSearchCandidate> candidates = jdbcClient
                .sql("""
                        SELECT experience_version_id, score, candidate_rank,
                               embedding_version
                        FROM career_search_candidate_snapshot
                        WHERE candidate_search_id = :searchId
                          AND user_id = :userId
                        ORDER BY candidate_rank
                        """)
                .param("searchId", searchId.value())
                .param("userId", userId.value())
                .query(this::mapCandidate)
                .list();
        SearchRow row = search.orElseThrow();
        return Optional.of(new CareerCandidateSearch(
                row.id(),
                row.userId(),
                row.requirementId(),
                row.queryEmbeddingVersion(),
                row.searchVersion(),
                row.searchedAt(),
                candidates));
    }

    private SearchRow mapSearch(ResultSet resultSet, int rowNumber) throws SQLException {
        return new SearchRow(
                new CareerCandidateSearchId(
                        resultSet.getObject("candidate_search_id", UUID.class)),
                new UserId(resultSet.getObject("user_id", UUID.class)),
                new JobRequirementId(resultSet.getObject("requirement_id", UUID.class)),
                resultSet.getString("query_embedding_version"),
                resultSet.getString("search_version"),
                resultSet.getObject("searched_at", OffsetDateTime.class).toInstant());
    }

    private CareerSearchCandidate mapCandidate(ResultSet resultSet, int rowNumber)
            throws SQLException {
        return new CareerSearchCandidate(
                new CareerExperienceVersionId(
                        resultSet.getObject("experience_version_id", UUID.class)),
                resultSet.getDouble("score"),
                resultSet.getInt("candidate_rank"),
                resultSet.getString("embedding_version"));
    }

    private record SearchRow(
            CareerCandidateSearchId id,
            UserId userId,
            JobRequirementId requirementId,
            String queryEmbeddingVersion,
            String searchVersion,
            java.time.Instant searchedAt) {}
}
