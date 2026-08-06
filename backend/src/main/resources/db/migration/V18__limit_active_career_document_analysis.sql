DROP INDEX uq_career_document_analysis_active;

CREATE UNIQUE INDEX uq_career_document_analysis_active
    ON career_document_analysis (user_id, document_id)
    WHERE status IN ('QUEUED', 'PROCESSING');
