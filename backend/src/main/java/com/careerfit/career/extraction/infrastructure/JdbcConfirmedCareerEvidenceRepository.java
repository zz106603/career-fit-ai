package com.careerfit.career.extraction.infrastructure;

import com.careerfit.career.domain.CareerExperienceVersionId;
import com.careerfit.career.extraction.application.ConfirmedCareerEvidenceRepository;
import com.careerfit.identity.UserId;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
/** INSERT SELECT로 문서명·페이지·발췌를 버전별 Evidence Snapshot에 복사한다. */
public class JdbcConfirmedCareerEvidenceRepository implements ConfirmedCareerEvidenceRepository {
    private final JdbcClient jdbcClient;

    public JdbcConfirmedCareerEvidenceRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public int copyFromCandidate(UserId userId, UUID candidateId, CareerExperienceVersionId versionId) {
        return jdbcClient.sql("""
                INSERT INTO career_experience_evidence (
                    evidence_id, experience_version_id, candidate_id, document_analysis_id,
                    document_id, user_id, document_name, page_number, excerpt)
                SELECT gen_random_uuid(), :versionId, evidence.candidate_id,
                       evidence.document_analysis_id, evidence.document_id, evidence.user_id,
                       document.original_name, evidence.page_number, evidence.excerpt
                  FROM experience_evidence evidence
                  JOIN career_document document
                    ON document.career_document_id = evidence.document_id
                   AND document.user_id = evidence.user_id
                 WHERE evidence.user_id = :userId
                   AND evidence.candidate_id = :candidateId
                """)
                .param("versionId", versionId.value())
                .param("userId", userId.value())
                .param("candidateId", candidateId)
                .update();
    }

    @Override
    public int copyFromVersion(
            UserId userId, CareerExperienceVersionId source, CareerExperienceVersionId target) {
        return jdbcClient.sql("""
                INSERT INTO career_experience_evidence (
                    evidence_id, experience_version_id, candidate_id, document_analysis_id,
                    document_id, user_id, document_name, page_number, excerpt)
                SELECT gen_random_uuid(), :targetVersionId, candidate_id, document_analysis_id,
                       document_id, user_id, document_name, page_number, excerpt
                  FROM career_experience_evidence
                 WHERE user_id = :userId
                   AND experience_version_id = :sourceVersionId
                """)
                .param("targetVersionId", target.value())
                .param("sourceVersionId", source.value())
                .param("userId", userId.value())
                .update();
    }

    @Override
    public boolean exists(UserId userId, CareerExperienceVersionId versionId) {
        return jdbcClient.sql("""
                SELECT EXISTS (
                    SELECT 1 FROM career_experience_evidence
                     WHERE user_id = :userId AND experience_version_id = :versionId)
                """)
                .param("userId", userId.value())
                .param("versionId", versionId.value())
                .query(Boolean.class)
                .single();
    }
}
