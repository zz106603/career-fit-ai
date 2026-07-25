package com.careerfit.career.infrastructure;

import com.careerfit.career.application.CareerExperienceRepository;
import com.careerfit.career.domain.CareerExperience;
import com.careerfit.career.domain.CareerExperienceId;
import com.careerfit.career.domain.CareerExperienceSourceType;
import com.careerfit.career.domain.CareerExperienceVersion;
import com.careerfit.career.domain.CareerExperienceVersionId;
import com.careerfit.career.domain.DirectCareerContent;
import com.careerfit.identity.UserId;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcCareerExperienceRepository implements CareerExperienceRepository {

    private static final String VERSION_COLUMNS = """
            SELECT experience_version_id, experience_id, user_id, version_no, source_type,
                   experience_type, title, organization, start_date, end_date, role,
                   responsibilities, problem, action, outcome, technologies,
                   created_at, confirmed_at, superseded_at, deleted_at
            FROM career_experience_version
            """;

    private final JdbcClient jdbcClient;

    public JdbcCareerExperienceRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public void saveExperience(CareerExperience experience) {
        jdbcClient
                .sql("""
                        INSERT INTO career_experience (
                            experience_id, user_id, created_at, deleted_at
                        ) VALUES (
                            :experienceId, :userId, :createdAt, :deletedAt
                        )
                        """)
                .param("experienceId", experience.id().value())
                .param("userId", experience.userId().value())
                .param("createdAt", atUtc(experience.createdAt()))
                .param("deletedAt", nullableAtUtc(experience.deletedAt()))
                .update();
    }

    @Override
    public void saveVersion(CareerExperienceVersion version) {
        jdbcClient
                .sql("""
                        INSERT INTO career_experience_version (
                            experience_version_id, experience_id, user_id, version_no,
                            source_type, experience_type, title, organization, start_date,
                            end_date, role, responsibilities, problem, action, outcome,
                            technologies, created_at, confirmed_at, superseded_at, deleted_at
                        ) VALUES (
                            :versionId, :experienceId, :userId, :versionNo,
                            :sourceType, :experienceType, :title, :organization, :startDate,
                            :endDate, :role, :responsibilities, :problem, :action, :outcome,
                            :technologies, :createdAt, :confirmedAt, :supersededAt, :deletedAt
                        )
                        """)
                .param("versionId", version.id().value())
                .param("experienceId", version.experienceId().value())
                .param("userId", version.userId().value())
                .param("versionNo", version.versionNo())
                .param("sourceType", version.sourceType().name())
                .param("experienceType", version.content().experienceType())
                .param("title", version.content().title())
                .param("organization", version.content().organization())
                .param("startDate", version.content().startDate())
                .param("endDate", version.content().endDate())
                .param("role", version.content().role())
                .param("responsibilities", version.content().responsibilities())
                .param("problem", version.content().problem())
                .param("action", version.content().action())
                .param("outcome", version.content().outcome())
                .param("technologies", version.content().technologies())
                .param("createdAt", atUtc(version.createdAt()))
                .param("confirmedAt", nullableAtUtc(version.confirmedAt()))
                .param("supersededAt", nullableAtUtc(version.supersededAt()))
                .param("deletedAt", nullableAtUtc(version.deletedAt()))
                .update();
    }

    @Override
    public Optional<CareerExperience> findActiveExperience(
            UserId userId, CareerExperienceId experienceId) {
        return jdbcClient
                .sql("""
                        SELECT experience_id, user_id, created_at, deleted_at
                        FROM career_experience
                        WHERE experience_id = :experienceId
                          AND user_id = :userId
                          AND deleted_at IS NULL
                        """)
                .param("experienceId", experienceId.value())
                .param("userId", userId.value())
                .query(this::mapExperience)
                .optional();
    }

    @Override
    public Optional<CareerExperienceVersion> findActiveVersion(
            UserId userId,
            CareerExperienceId experienceId,
            CareerExperienceVersionId versionId) {
        return jdbcClient
                .sql(VERSION_COLUMNS
                        + """
                         WHERE experience_version_id = :versionId
                           AND experience_id = :experienceId
                           AND user_id = :userId
                           AND deleted_at IS NULL
                        """)
                .param("versionId", versionId.value())
                .param("experienceId", experienceId.value())
                .param("userId", userId.value())
                .query(this::mapVersion)
                .optional();
    }

    @Override
    public int nextVersionNumber(UserId userId, CareerExperienceId experienceId) {
        return jdbcClient
                .sql("""
                        SELECT COALESCE(MAX(version_no), 0) + 1
                        FROM career_experience_version
                        WHERE experience_id = :experienceId
                          AND user_id = :userId
                        """)
                .param("experienceId", experienceId.value())
                .param("userId", userId.value())
                .query(Integer.class)
                .single();
    }

    @Override
    public void supersedeCurrentVersion(
            UserId userId,
            CareerExperienceId experienceId,
            CareerExperienceVersionId nextVersionId,
            Instant supersededAt) {
        jdbcClient
                .sql("""
                        UPDATE career_experience_version
                        SET superseded_at = :supersededAt
                        WHERE experience_id = :experienceId
                          AND user_id = :userId
                          AND experience_version_id <> :nextVersionId
                          AND confirmed_at IS NOT NULL
                          AND superseded_at IS NULL
                          AND deleted_at IS NULL
                        """)
                .param("supersededAt", atUtc(supersededAt))
                .param("experienceId", experienceId.value())
                .param("userId", userId.value())
                .param("nextVersionId", nextVersionId.value())
                .update();
    }

    @Override
    public boolean confirmVersion(
            UserId userId,
            CareerExperienceId experienceId,
            CareerExperienceVersionId versionId,
            Instant confirmedAt) {
        int updated = jdbcClient
                .sql("""
                        UPDATE career_experience_version
                        SET confirmed_at = :confirmedAt
                        WHERE experience_version_id = :versionId
                          AND experience_id = :experienceId
                          AND user_id = :userId
                          AND source_type = 'USER_DIRECT'
                          AND confirmed_at IS NULL
                          AND deleted_at IS NULL
                        """)
                .param("confirmedAt", atUtc(confirmedAt))
                .param("versionId", versionId.value())
                .param("experienceId", experienceId.value())
                .param("userId", userId.value())
                .update();
        return updated == 1;
    }

    @Override
    public List<CareerExperienceVersion> findCurrentConfirmed(UserId userId) {
        return jdbcClient
                .sql(VERSION_COLUMNS
                        + """
                         WHERE user_id = :userId
                           AND confirmed_at IS NOT NULL
                           AND superseded_at IS NULL
                           AND deleted_at IS NULL
                           AND EXISTS (
                               SELECT 1
                               FROM career_experience experience
                               WHERE experience.experience_id =
                                     career_experience_version.experience_id
                                 AND experience.user_id = :userId
                                 AND experience.deleted_at IS NULL
                           )
                         ORDER BY confirmed_at DESC
                        """)
                .param("userId", userId.value())
                .query(this::mapVersion)
                .list();
    }

    @Override
    public void delete(UserId userId, CareerExperienceId experienceId, Instant deletedAt) {
        OffsetDateTime deletedDateTime = atUtc(deletedAt);
        jdbcClient
                .sql("""
                        UPDATE career_experience_version
                        SET deleted_at = :deletedAt
                        WHERE experience_id = :experienceId
                          AND user_id = :userId
                          AND deleted_at IS NULL
                        """)
                .param("deletedAt", deletedDateTime)
                .param("experienceId", experienceId.value())
                .param("userId", userId.value())
                .update();
        jdbcClient
                .sql("""
                        UPDATE career_experience
                        SET deleted_at = :deletedAt
                        WHERE experience_id = :experienceId
                          AND user_id = :userId
                          AND deleted_at IS NULL
                        """)
                .param("deletedAt", deletedDateTime)
                .param("experienceId", experienceId.value())
                .param("userId", userId.value())
                .update();
    }

    private CareerExperience mapExperience(ResultSet resultSet, int rowNumber)
            throws SQLException {
        return new CareerExperience(
                new CareerExperienceId(resultSet.getObject("experience_id", UUID.class)),
                new UserId(resultSet.getObject("user_id", UUID.class)),
                instant(resultSet, "created_at"),
                nullableInstant(resultSet, "deleted_at"));
    }

    private CareerExperienceVersion mapVersion(ResultSet resultSet, int rowNumber)
            throws SQLException {
        return new CareerExperienceVersion(
                new CareerExperienceVersionId(
                        resultSet.getObject("experience_version_id", UUID.class)),
                new CareerExperienceId(resultSet.getObject("experience_id", UUID.class)),
                new UserId(resultSet.getObject("user_id", UUID.class)),
                resultSet.getInt("version_no"),
                CareerExperienceSourceType.valueOf(resultSet.getString("source_type")),
                new DirectCareerContent(
                        resultSet.getString("experience_type"),
                        resultSet.getString("title"),
                        resultSet.getString("organization"),
                        resultSet.getObject("start_date", java.time.LocalDate.class),
                        resultSet.getObject("end_date", java.time.LocalDate.class),
                        resultSet.getString("role"),
                        resultSet.getString("responsibilities"),
                        resultSet.getString("problem"),
                        resultSet.getString("action"),
                        resultSet.getString("outcome"),
                        resultSet.getString("technologies")),
                instant(resultSet, "created_at"),
                nullableInstant(resultSet, "confirmed_at"),
                nullableInstant(resultSet, "superseded_at"),
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
