CREATE TABLE job_posting_analysis (
    job_posting_analysis_id UUID PRIMARY KEY,
    job_posting_id UUID NOT NULL,
    user_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PROCESSING', 'READY')),
    company_name VARCHAR(500),
    job_title VARCHAR(500),
    workflow_version VARCHAR(200) NOT NULL CHECK (length(trim(workflow_version)) > 0),
    created_at TIMESTAMPTZ NOT NULL,
    ready_at TIMESTAMPTZ,
    CONSTRAINT uq_job_posting_analysis_owner
        UNIQUE (job_posting_analysis_id, user_id),
    CONSTRAINT fk_job_posting_analysis_owner
        FOREIGN KEY (job_posting_id, user_id)
        REFERENCES job_posting (job_posting_id, user_id),
    CONSTRAINT chk_job_posting_analysis_ready
        CHECK (
            (status = 'PROCESSING' AND ready_at IS NULL)
            OR (status = 'READY' AND ready_at IS NOT NULL)
        )
);

CREATE INDEX idx_job_posting_analysis_latest
    ON job_posting_analysis (user_id, job_posting_id, ready_at DESC)
    WHERE status = 'READY';

CREATE TABLE job_requirement (
    requirement_id UUID PRIMARY KEY,
    job_posting_analysis_id UUID NOT NULL,
    user_id UUID NOT NULL,
    category VARCHAR(30) NOT NULL CHECK (category IN ('REQUIRED')),
    requirement_text TEXT NOT NULL CHECK (length(trim(requirement_text)) > 0),
    source_excerpt TEXT NOT NULL CHECK (length(trim(source_excerpt)) > 0),
    sequence_no INTEGER NOT NULL CHECK (sequence_no > 0),
    CONSTRAINT fk_job_requirement_analysis_owner
        FOREIGN KEY (job_posting_analysis_id, user_id)
        REFERENCES job_posting_analysis (job_posting_analysis_id, user_id),
    CONSTRAINT uq_job_requirement_sequence
        UNIQUE (job_posting_analysis_id, sequence_no)
);
