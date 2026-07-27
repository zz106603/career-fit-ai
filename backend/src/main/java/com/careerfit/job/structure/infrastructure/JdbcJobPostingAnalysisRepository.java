package com.careerfit.job.structure.infrastructure;

import com.careerfit.identity.UserId;
import com.careerfit.job.domain.JobPostingId;
import com.careerfit.job.structure.application.JobPostingAnalysisRepository;
import com.careerfit.job.structure.domain.JobPostingAnalysis;
import com.careerfit.job.structure.domain.JobPostingAnalysisId;
import com.careerfit.job.structure.domain.JobPostingAnalysisStatus;
import com.careerfit.job.structure.domain.JobRequirement;
import com.careerfit.job.structure.domain.JobRequirementCategory;
import com.careerfit.job.structure.domain.JobRequirementId;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JdbcJobPostingAnalysisRepository implements JobPostingAnalysisRepository {

    private final JdbcClient jdbcClient;

    public JdbcJobPostingAnalysisRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    @Transactional
    public void saveReady(JobPostingAnalysis analysis) {
        if (analysis.status() != JobPostingAnalysisStatus.READY) {
            throw new IllegalArgumentException("READY 구조화 결과만 저장할 수 있습니다.");
        }
        jdbcClient
                .sql("""
                        INSERT INTO job_posting_analysis (
                            job_posting_analysis_id, job_posting_id, user_id, status,
                            company_name, job_title, workflow_version, created_at, ready_at
                        ) VALUES (
                            :analysisId, :jobPostingId, :userId, 'READY',
                            :companyName, :jobTitle, :workflowVersion, :createdAt, :readyAt
                        )
                        """)
                .param("analysisId", analysis.id().value())
                .param("jobPostingId", analysis.jobPostingId().value())
                .param("userId", analysis.userId().value())
                .param("companyName", analysis.companyName())
                .param("jobTitle", analysis.jobTitle())
                .param("workflowVersion", analysis.workflowVersion())
                .param("createdAt", atUtc(analysis.createdAt()))
                .param("readyAt", atUtc(analysis.readyAt()))
                .update();

        JobRequirement requirement = analysis.requirement();
        jdbcClient
                .sql("""
                        INSERT INTO job_requirement (
                            requirement_id, job_posting_analysis_id, user_id,
                            category, requirement_text, source_excerpt, sequence_no
                        ) VALUES (
                            :requirementId, :analysisId, :userId,
                            :category, :requirementText, :sourceExcerpt, :sequenceNo
                        )
                        """)
                .param("requirementId", requirement.id().value())
                .param("analysisId", analysis.id().value())
                .param("userId", analysis.userId().value())
                .param("category", requirement.category().name())
                .param("requirementText", requirement.text())
                .param("sourceExcerpt", requirement.sourceExcerpt())
                .param("sequenceNo", requirement.sequence())
                .update();
    }

    @Override
    public Optional<JobPostingAnalysis> findLatestReady(
            UserId userId, JobPostingId jobPostingId) {
        return jdbcClient
                .sql("""
                        SELECT analysis.job_posting_analysis_id, analysis.job_posting_id,
                               analysis.user_id, analysis.status, analysis.company_name,
                               analysis.job_title, analysis.workflow_version,
                               analysis.created_at, analysis.ready_at,
                               requirement.requirement_id, requirement.category,
                               requirement.requirement_text, requirement.source_excerpt,
                               requirement.sequence_no
                        FROM job_posting_analysis analysis
                        JOIN job_requirement requirement
                          ON requirement.job_posting_analysis_id =
                             analysis.job_posting_analysis_id
                         AND requirement.user_id = analysis.user_id
                        WHERE analysis.job_posting_id = :jobPostingId
                          AND analysis.user_id = :userId
                          AND analysis.status = 'READY'
                        ORDER BY analysis.ready_at DESC,
                                 analysis.job_posting_analysis_id DESC
                        LIMIT 1
                        """)
                .param("jobPostingId", jobPostingId.value())
                .param("userId", userId.value())
                .query(this::map)
                .optional();
    }

    @Override
    public Optional<JobPostingAnalysis> findReadyByRequirement(
            UserId userId, JobRequirementId requirementId) {
        return jdbcClient
                .sql("""
                        SELECT analysis.job_posting_analysis_id, analysis.job_posting_id,
                               analysis.user_id, analysis.status, analysis.company_name,
                               analysis.job_title, analysis.workflow_version,
                               analysis.created_at, analysis.ready_at,
                               requirement.requirement_id, requirement.category,
                               requirement.requirement_text, requirement.source_excerpt,
                               requirement.sequence_no
                        FROM job_posting_analysis analysis
                        JOIN job_requirement requirement
                          ON requirement.job_posting_analysis_id =
                             analysis.job_posting_analysis_id
                         AND requirement.user_id = analysis.user_id
                        WHERE requirement.requirement_id = :requirementId
                          AND analysis.user_id = :userId
                          AND analysis.status = 'READY'
                        """)
                .param("requirementId", requirementId.value())
                .param("userId", userId.value())
                .query(this::map)
                .optional();
    }

    private JobPostingAnalysis map(ResultSet resultSet, int rowNumber) throws SQLException {
        JobPostingAnalysisId analysisId = new JobPostingAnalysisId(
                resultSet.getObject("job_posting_analysis_id", UUID.class));
        JobRequirement requirement = new JobRequirement(
                new JobRequirementId(resultSet.getObject("requirement_id", UUID.class)),
                analysisId,
                JobRequirementCategory.valueOf(resultSet.getString("category")),
                resultSet.getString("requirement_text"),
                resultSet.getString("source_excerpt"),
                resultSet.getInt("sequence_no"));
        return new JobPostingAnalysis(
                analysisId,
                new JobPostingId(resultSet.getObject("job_posting_id", UUID.class)),
                new UserId(resultSet.getObject("user_id", UUID.class)),
                JobPostingAnalysisStatus.valueOf(resultSet.getString("status")),
                resultSet.getString("company_name"),
                resultSet.getString("job_title"),
                resultSet.getString("workflow_version"),
                instant(resultSet, "created_at"),
                instant(resultSet, "ready_at"),
                requirement);
    }

    private Instant instant(ResultSet resultSet, String column) throws SQLException {
        return resultSet.getObject(column, OffsetDateTime.class).toInstant();
    }

    private OffsetDateTime atUtc(Instant value) {
        return value.atOffset(ZoneOffset.UTC);
    }
}
