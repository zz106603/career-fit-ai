CREATE TABLE job_posting (
    job_posting_id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    original_text TEXT NOT NULL CHECK (length(trim(original_text)) > 0),
    title_hint VARCHAR(500),
    company_hint VARCHAR(500),
    registered_at TIMESTAMPTZ NOT NULL,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT uq_job_posting_owner UNIQUE (job_posting_id, user_id)
);

CREATE INDEX idx_job_posting_user
    ON job_posting (user_id, registered_at DESC)
    WHERE deleted_at IS NULL;

CREATE FUNCTION prevent_job_posting_source_update()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.user_id IS DISTINCT FROM OLD.user_id
        OR NEW.original_text IS DISTINCT FROM OLD.original_text
        OR NEW.title_hint IS DISTINCT FROM OLD.title_hint
        OR NEW.company_hint IS DISTINCT FROM OLD.company_hint
        OR NEW.registered_at IS DISTINCT FROM OLD.registered_at THEN
        RAISE EXCEPTION 'job posting source is immutable';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_job_posting_source_immutable
BEFORE UPDATE ON job_posting
FOR EACH ROW
EXECUTE FUNCTION prevent_job_posting_source_update();
