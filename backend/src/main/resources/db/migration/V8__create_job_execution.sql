CREATE TABLE job_execution (
    job_execution_id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    job_type VARCHAR(50) NOT NULL CHECK (
        job_type IN (
            'CAREER_DOCUMENT_EXTRACTION',
            'CAREER_CANDIDATE_EXTRACTION',
            'CAREER_INDEXING',
            'JOB_POSTING_STRUCTURE',
            'COMPANY_RESEARCH',
            'JOB_ANALYSIS'
        )
    ),
    target_id UUID NOT NULL,
    input_version VARCHAR(200) NOT NULL CHECK (length(trim(input_version)) > 0),
    duplicate_key VARCHAR(500) NOT NULL CHECK (length(trim(duplicate_key)) > 0),
    status VARCHAR(20) NOT NULL CHECK (
        status IN ('QUEUED', 'PROCESSING', 'SUCCEEDED', 'FAILED')
    ),
    failure_code VARCHAR(200),
    created_at TIMESTAMPTZ NOT NULL,
    claimed_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    CONSTRAINT chk_job_execution_state_fields CHECK (
        (
            status = 'QUEUED'
            AND claimed_at IS NULL
            AND completed_at IS NULL
            AND failure_code IS NULL
        )
        OR (
            status = 'PROCESSING'
            AND claimed_at IS NOT NULL
            AND completed_at IS NULL
            AND failure_code IS NULL
        )
        OR (
            status = 'SUCCEEDED'
            AND claimed_at IS NOT NULL
            AND completed_at IS NOT NULL
            AND failure_code IS NULL
        )
        OR (
            status = 'FAILED'
            AND claimed_at IS NOT NULL
            AND completed_at IS NOT NULL
            AND length(trim(failure_code)) > 0
        )
    ),
    CONSTRAINT chk_job_execution_time_order CHECK (
        (claimed_at IS NULL OR claimed_at >= created_at)
        AND (completed_at IS NULL OR completed_at >= claimed_at)
    )
);

CREATE UNIQUE INDEX uq_job_execution_active_duplicate
    ON job_execution (user_id, duplicate_key)
    WHERE status IN ('QUEUED', 'PROCESSING');

CREATE INDEX idx_job_execution_queue
    ON job_execution (status, created_at, job_execution_id)
    WHERE status = 'QUEUED';
