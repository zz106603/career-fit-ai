ALTER TABLE job_execution
    ADD COLUMN retry_count INTEGER NOT NULL DEFAULT 0;

ALTER TABLE job_execution
    ADD CONSTRAINT chk_job_execution_retry_count
        CHECK (retry_count >= 0);

CREATE INDEX idx_job_execution_stale
    ON job_execution (claimed_at, job_execution_id)
    WHERE status = 'PROCESSING';
