CREATE TABLE career_document_analysis (
    document_analysis_id UUID PRIMARY KEY,
    document_id UUID NOT NULL REFERENCES career_document(career_document_id),
    user_id UUID NOT NULL,
    job_execution_id UUID NOT NULL REFERENCES job_execution(job_execution_id),
    input_kind VARCHAR(30) NOT NULL CHECK (input_kind IN ('PDF_TEXT', 'PASTED_TEXT')),
    status VARCHAR(20) NOT NULL CHECK (status IN ('QUEUED', 'PROCESSING', 'SUCCEEDED', 'FAILED')),
    input_version VARCHAR(200) NOT NULL,
    workflow_version VARCHAR(100) NOT NULL,
    extracted_text_reference VARCHAR(500),
    failure_code VARCHAR(200),
    created_at TIMESTAMPTZ NOT NULL,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    CONSTRAINT uq_career_document_analysis_job UNIQUE (job_execution_id),
    CONSTRAINT chk_career_document_analysis_state CHECK (
        (status = 'QUEUED' AND started_at IS NULL AND completed_at IS NULL AND failure_code IS NULL)
        OR (status = 'PROCESSING' AND started_at IS NOT NULL AND completed_at IS NULL AND failure_code IS NULL)
        OR (status = 'SUCCEEDED' AND started_at IS NOT NULL AND completed_at IS NOT NULL AND failure_code IS NULL)
        OR (status = 'FAILED' AND completed_at IS NOT NULL AND length(trim(failure_code)) > 0)
    )
);

CREATE UNIQUE INDEX uq_career_document_analysis_active
    ON career_document_analysis (user_id, document_id, input_version)
    WHERE status IN ('QUEUED', 'PROCESSING');

CREATE INDEX idx_career_document_analysis_owner
    ON career_document_analysis (user_id, document_analysis_id);

CREATE TABLE career_document_page (
    document_analysis_id UUID NOT NULL REFERENCES career_document_analysis(document_analysis_id) ON DELETE CASCADE,
    document_id UUID NOT NULL REFERENCES career_document(career_document_id),
    user_id UUID NOT NULL,
    page_number INTEGER NOT NULL CHECK (page_number > 0),
    page_text TEXT NOT NULL,
    text_length INTEGER NOT NULL CHECK (text_length >= 0),
    checksum_sha256 VARCHAR(64) NOT NULL,
    PRIMARY KEY (document_analysis_id, page_number),
    CONSTRAINT chk_career_document_page_length CHECK (text_length = char_length(page_text))
);

CREATE INDEX idx_career_document_page_owner
    ON career_document_page (user_id, document_analysis_id, page_number);
