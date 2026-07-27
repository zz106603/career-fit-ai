package com.careerfit.job.infrastructure;

import com.careerfit.identity.UserId;
import com.careerfit.job.application.JobPostingRepository;
import com.careerfit.job.domain.JobPosting;
import com.careerfit.job.domain.JobPostingId;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcJobPostingRepository implements JobPostingRepository {

    private final JdbcClient jdbcClient;

    public JdbcJobPostingRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public void save(JobPosting jobPosting) {
        jdbcClient
                .sql("""
                        INSERT INTO job_posting (
                            job_posting_id, user_id, original_text, title_hint,
                            company_hint, registered_at, deleted_at
                        ) VALUES (
                            :jobPostingId, :userId, :originalText, :titleHint,
                            :companyHint, :registeredAt, :deletedAt
                        )
                        """)
                .param("jobPostingId", jobPosting.id().value())
                .param("userId", jobPosting.userId().value())
                .param("originalText", jobPosting.originalText())
                .param("titleHint", jobPosting.titleHint())
                .param("companyHint", jobPosting.companyHint())
                .param("registeredAt", atUtc(jobPosting.registeredAt()))
                .param("deletedAt", nullableAtUtc(jobPosting.deletedAt()))
                .update();
    }

    @Override
    public Optional<JobPosting> findActive(UserId userId, JobPostingId jobPostingId) {
        return jdbcClient
                .sql("""
                        SELECT job_posting_id, user_id, original_text, title_hint,
                               company_hint, registered_at, deleted_at
                        FROM job_posting
                        WHERE job_posting_id = :jobPostingId
                          AND user_id = :userId
                          AND deleted_at IS NULL
                        """)
                .param("jobPostingId", jobPostingId.value())
                .param("userId", userId.value())
                .query(this::map)
                .optional();
    }

    @Override
    public boolean delete(UserId userId, JobPostingId jobPostingId, Instant deletedAt) {
        int updated = jdbcClient
                .sql("""
                        UPDATE job_posting
                        SET deleted_at = :deletedAt
                        WHERE job_posting_id = :jobPostingId
                          AND user_id = :userId
                          AND deleted_at IS NULL
                        """)
                .param("deletedAt", atUtc(deletedAt))
                .param("jobPostingId", jobPostingId.value())
                .param("userId", userId.value())
                .update();
        return updated == 1;
    }

    private JobPosting map(ResultSet resultSet, int rowNumber) throws SQLException {
        return new JobPosting(
                new JobPostingId(resultSet.getObject("job_posting_id", UUID.class)),
                new UserId(resultSet.getObject("user_id", UUID.class)),
                resultSet.getString("original_text"),
                resultSet.getString("title_hint"),
                resultSet.getString("company_hint"),
                instant(resultSet, "registered_at"),
                nullableInstant(resultSet, "deleted_at"));
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

    private OffsetDateTime nullableAtUtc(Instant value) {
        return value == null ? null : atUtc(value);
    }
}
