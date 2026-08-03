ALTER TABLE career_document_analysis
    ALTER COLUMN job_execution_id DROP NOT NULL;

ALTER TABLE career_document_analysis
    ADD CONSTRAINT chk_career_document_analysis_input_job CHECK (
        (input_kind = 'PDF_TEXT' AND job_execution_id IS NOT NULL)
        OR (input_kind = 'PASTED_TEXT' AND job_execution_id IS NULL)
    );

CREATE TABLE career_document_alternative_text (
    document_analysis_id UUID PRIMARY KEY
        REFERENCES career_document_analysis(document_analysis_id) ON DELETE CASCADE,
    document_id UUID NOT NULL REFERENCES career_document(career_document_id),
    user_id UUID NOT NULL,
    alternative_text TEXT NOT NULL,
    text_length INTEGER NOT NULL CHECK (text_length > 0 AND text_length <= 200000),
    checksum_sha256 VARCHAR(64) NOT NULL CHECK (checksum_sha256 ~ '^[0-9a-f]{64}$'),
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_career_document_alternative_text_length
        CHECK (text_length = char_length(alternative_text))
);

CREATE INDEX idx_career_document_alternative_text_owner
    ON career_document_alternative_text (user_id, document_id, document_analysis_id);
