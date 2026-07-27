package com.careerfit.analysis.match.infrastructure;

import com.careerfit.analysis.match.application.JobAnalysisResultRepository;
import com.careerfit.analysis.match.domain.CareerEvidenceSnapshot;
import com.careerfit.analysis.match.domain.JobAnalysisResult;
import com.careerfit.analysis.match.domain.JobAnalysisResultId;
import com.careerfit.analysis.match.domain.RequirementMatchResult;
import com.careerfit.analysis.match.domain.RequirementMatchStatus;
import com.careerfit.analysis.search.domain.CareerCandidateSearchId;
import com.careerfit.career.domain.CareerExperienceSourceType;
import com.careerfit.career.domain.CareerExperienceVersionId;
import com.careerfit.identity.UserId;
import com.careerfit.job.domain.JobPostingId;
import com.careerfit.job.structure.domain.JobPostingAnalysisId;
import com.careerfit.job.structure.domain.JobRequirementCategory;
import com.careerfit.job.structure.domain.JobRequirementId;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JdbcJobAnalysisResultRepository implements JobAnalysisResultRepository {

    private final JdbcClient jdbcClient;

    public JdbcJobAnalysisResultRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    @Transactional
    public void save(JobAnalysisResult result) {
        jdbcClient
                .sql("""
                        INSERT INTO job_analysis_result (
                            job_analysis_result_id, user_id, job_posting_id,
                            candidate_search_id, workflow_version, completed_at
                        ) VALUES (
                            :resultId, :userId, :jobPostingId,
                            :searchId, :workflowVersion, :completedAt
                        )
                        """)
                .param("resultId", result.id().value())
                .param("userId", result.userId().value())
                .param("jobPostingId", result.jobPostingId().value())
                .param("searchId", result.candidateSearchId().value())
                .param("workflowVersion", result.workflowVersion())
                .param("completedAt", result.completedAt().atOffset(ZoneOffset.UTC))
                .update();

        RequirementMatchResult match = result.match();
        jdbcClient
                .sql("""
                        INSERT INTO requirement_match_result (
                            job_analysis_result_id, user_id, requirement_id,
                            job_posting_analysis_id, category, requirement_text,
                            source_excerpt, sequence_no, match_status, reason
                        ) VALUES (
                            :resultId, :userId, :requirementId,
                            :postingAnalysisId, :category, :requirementText,
                            :sourceExcerpt, :sequenceNo, :status, :reason
                        )
                        """)
                .param("resultId", result.id().value())
                .param("userId", result.userId().value())
                .param("requirementId", match.requirementId().value())
                .param("postingAnalysisId", match.jobPostingAnalysisId().value())
                .param("category", match.category().name())
                .param("requirementText", match.requirementText())
                .param("sourceExcerpt", match.sourceExcerpt())
                .param("sequenceNo", match.sequence())
                .param("status", match.status().name())
                .param("reason", match.reason())
                .update();

        if (match.evidence() != null) {
            saveEvidence(result, match.evidence());
        }
    }

    private void saveEvidence(JobAnalysisResult result, CareerEvidenceSnapshot evidence) {
        jdbcClient
                .sql("""
                        INSERT INTO match_evidence_snapshot (
                            job_analysis_result_id, user_id, experience_version_id,
                            source_type, title, role, responsibilities, technologies,
                            search_score, search_rank, explicit_conflict
                        ) VALUES (
                            :resultId, :userId, :versionId,
                            :sourceType, :title, :role, :responsibilities, :technologies,
                            :searchScore, :searchRank, :explicitConflict
                        )
                        """)
                .param("resultId", result.id().value())
                .param("userId", result.userId().value())
                .param("versionId", evidence.experienceVersionId().value())
                .param("sourceType", evidence.sourceType().name())
                .param("title", evidence.title())
                .param("role", evidence.role())
                .param("responsibilities", evidence.responsibilities())
                .param("technologies", evidence.technologies())
                .param("searchScore", evidence.searchScore())
                .param("searchRank", evidence.searchRank())
                .param("explicitConflict", evidence.explicitConflict())
                .update();
    }

    @Override
    public Optional<JobAnalysisResult> find(UserId userId, JobAnalysisResultId resultId) {
        return jdbcClient
                .sql("""
                        SELECT result.job_analysis_result_id, result.user_id,
                               result.job_posting_id, result.candidate_search_id,
                               result.workflow_version, result.completed_at,
                               match.requirement_id, match.job_posting_analysis_id,
                               match.category, match.requirement_text, match.source_excerpt,
                               match.sequence_no, match.match_status, match.reason,
                               evidence.experience_version_id, evidence.source_type,
                               evidence.title, evidence.role, evidence.responsibilities,
                               evidence.technologies, evidence.search_score,
                               evidence.search_rank, evidence.explicit_conflict
                        FROM job_analysis_result result
                        JOIN requirement_match_result match
                          ON match.job_analysis_result_id =
                             result.job_analysis_result_id
                         AND match.user_id = result.user_id
                        LEFT JOIN match_evidence_snapshot evidence
                          ON evidence.job_analysis_result_id =
                             result.job_analysis_result_id
                         AND evidence.user_id = result.user_id
                        WHERE result.job_analysis_result_id = :resultId
                          AND result.user_id = :userId
                        """)
                .param("resultId", resultId.value())
                .param("userId", userId.value())
                .query(this::map)
                .optional();
    }

    private JobAnalysisResult map(ResultSet resultSet, int rowNumber) throws SQLException {
        CareerEvidenceSnapshot evidence = mapEvidence(resultSet);
        RequirementMatchResult match = new RequirementMatchResult(
                new JobRequirementId(resultSet.getObject("requirement_id", UUID.class)),
                new JobPostingAnalysisId(
                        resultSet.getObject("job_posting_analysis_id", UUID.class)),
                JobRequirementCategory.valueOf(resultSet.getString("category")),
                resultSet.getString("requirement_text"),
                resultSet.getString("source_excerpt"),
                resultSet.getInt("sequence_no"),
                RequirementMatchStatus.valueOf(resultSet.getString("match_status")),
                resultSet.getString("reason"),
                evidence);
        return new JobAnalysisResult(
                new JobAnalysisResultId(
                        resultSet.getObject("job_analysis_result_id", UUID.class)),
                new UserId(resultSet.getObject("user_id", UUID.class)),
                new JobPostingId(resultSet.getObject("job_posting_id", UUID.class)),
                new CareerCandidateSearchId(
                        resultSet.getObject("candidate_search_id", UUID.class)),
                resultSet.getString("workflow_version"),
                resultSet.getObject("completed_at", OffsetDateTime.class).toInstant(),
                match);
    }

    private CareerEvidenceSnapshot mapEvidence(ResultSet resultSet) throws SQLException {
        UUID versionId = resultSet.getObject("experience_version_id", UUID.class);
        if (versionId == null) {
            return null;
        }
        return new CareerEvidenceSnapshot(
                new CareerExperienceVersionId(versionId),
                CareerExperienceSourceType.valueOf(resultSet.getString("source_type")),
                resultSet.getString("title"),
                resultSet.getString("role"),
                resultSet.getString("responsibilities"),
                resultSet.getString("technologies"),
                resultSet.getDouble("search_score"),
                resultSet.getInt("search_rank"),
                resultSet.getBoolean("explicit_conflict"));
    }
}
