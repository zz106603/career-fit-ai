CREATE TABLE ai_call_execution (
    ai_call_execution_id UUID PRIMARY KEY,
    workflow_execution_id UUID NOT NULL,
    request_id VARCHAR(100),
    purpose VARCHAR(100) NOT NULL,
    prompt_version VARCHAR(100) NOT NULL,
    schema_version VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PROCESSING', 'SUCCEEDED', 'FAILED')),
    failure_code VARCHAR(100),
    attempt_count INTEGER NOT NULL CHECK (attempt_count >= 0),
    started_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    CONSTRAINT chk_ai_call_execution_state CHECK (
        (status = 'PROCESSING' AND completed_at IS NULL AND failure_code IS NULL)
        OR (status = 'SUCCEEDED' AND completed_at IS NOT NULL AND failure_code IS NULL)
        OR (status = 'FAILED' AND completed_at IS NOT NULL AND length(trim(failure_code)) > 0)
    )
);

CREATE INDEX idx_ai_call_execution_workflow
    ON ai_call_execution (workflow_execution_id, started_at);

CREATE TABLE ai_call_attempt (
    ai_call_execution_id UUID NOT NULL
        REFERENCES ai_call_execution(ai_call_execution_id) ON DELETE CASCADE,
    attempt_number INTEGER NOT NULL CHECK (attempt_number > 0),
    status VARCHAR(20) NOT NULL CHECK (status IN ('PROCESSING', 'SUCCEEDED', 'FAILED')),
    provider VARCHAR(100),
    model VARCHAR(200),
    provider_request_id VARCHAR(200),
    failure_code VARCHAR(100),
    started_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    input_tokens INTEGER CHECK (input_tokens >= 0),
    output_tokens INTEGER CHECK (output_tokens >= 0),
    total_tokens INTEGER CHECK (total_tokens >= 0),
    prompt_length INTEGER NOT NULL CHECK (prompt_length >= 0),
    prompt_checksum_sha256 VARCHAR(64) NOT NULL
        CHECK (prompt_checksum_sha256 ~ '^[0-9a-f]{64}$'),
    response_length INTEGER,
    response_checksum_sha256 VARCHAR(64)
        CHECK (response_checksum_sha256 IS NULL OR response_checksum_sha256 ~ '^[0-9a-f]{64}$'),
    PRIMARY KEY (ai_call_execution_id, attempt_number),
    CONSTRAINT chk_ai_call_attempt_state CHECK (
        (status = 'PROCESSING' AND completed_at IS NULL AND failure_code IS NULL)
        OR (status = 'SUCCEEDED' AND completed_at IS NOT NULL AND failure_code IS NULL)
        OR (status = 'FAILED' AND completed_at IS NOT NULL AND length(trim(failure_code)) > 0)
    ),
    CONSTRAINT chk_ai_call_attempt_response_metadata CHECK (
        (response_length IS NULL AND response_checksum_sha256 IS NULL)
        OR (response_length >= 0 AND response_checksum_sha256 IS NOT NULL)
    )
);
