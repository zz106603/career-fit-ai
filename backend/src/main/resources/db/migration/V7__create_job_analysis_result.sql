CREATE TABLE job_analysis_result (
    job_analysis_result_id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    job_posting_id UUID NOT NULL,
    candidate_search_id UUID NOT NULL,
    workflow_version VARCHAR(100) NOT NULL CHECK (length(trim(workflow_version)) > 0),
    completed_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_job_analysis_result_owner UNIQUE (job_analysis_result_id, user_id),
    CONSTRAINT fk_job_analysis_result_posting_owner
        FOREIGN KEY (job_posting_id, user_id)
        REFERENCES job_posting (job_posting_id, user_id),
    CONSTRAINT fk_job_analysis_result_search_owner
        FOREIGN KEY (candidate_search_id, user_id)
        REFERENCES career_candidate_search (candidate_search_id, user_id)
);

CREATE TABLE requirement_match_result (
    job_analysis_result_id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    requirement_id UUID NOT NULL,
    job_posting_analysis_id UUID NOT NULL,
    category VARCHAR(30) NOT NULL CHECK (category IN ('REQUIRED')),
    requirement_text TEXT NOT NULL CHECK (length(trim(requirement_text)) > 0),
    source_excerpt TEXT NOT NULL CHECK (length(trim(source_excerpt)) > 0),
    sequence_no INTEGER NOT NULL CHECK (sequence_no > 0),
    match_status VARCHAR(30) NOT NULL CHECK (
        match_status IN (
            'SATISFIED', 'PARTIALLY_SATISFIED', 'UNKNOWN', 'NOT_SATISFIED'
        )
    ),
    reason TEXT NOT NULL CHECK (length(trim(reason)) > 0),
    CONSTRAINT uq_requirement_match_result_owner
        UNIQUE (job_analysis_result_id, user_id),
    CONSTRAINT fk_requirement_match_result_analysis_owner
        FOREIGN KEY (job_analysis_result_id, user_id)
        REFERENCES job_analysis_result (job_analysis_result_id, user_id),
    CONSTRAINT fk_requirement_match_requirement_owner
        FOREIGN KEY (requirement_id, user_id)
        REFERENCES job_requirement (requirement_id, user_id),
    CONSTRAINT fk_requirement_match_posting_analysis_owner
        FOREIGN KEY (job_posting_analysis_id, user_id)
        REFERENCES job_posting_analysis (job_posting_analysis_id, user_id)
);

CREATE TABLE match_evidence_snapshot (
    job_analysis_result_id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    experience_version_id UUID NOT NULL,
    source_type VARCHAR(20) NOT NULL CHECK (source_type IN ('DOCUMENT', 'USER_DIRECT')),
    title VARCHAR(200) NOT NULL CHECK (length(trim(title)) > 0),
    role VARCHAR(500),
    responsibilities TEXT,
    technologies TEXT,
    search_score DOUBLE PRECISION NOT NULL CHECK (search_score >= -1 AND search_score <= 1),
    search_rank INTEGER NOT NULL CHECK (search_rank > 0),
    explicit_conflict BOOLEAN NOT NULL,
    CONSTRAINT fk_match_evidence_result_owner
        FOREIGN KEY (job_analysis_result_id, user_id)
        REFERENCES job_analysis_result (job_analysis_result_id, user_id),
    CONSTRAINT fk_match_evidence_version_owner
        FOREIGN KEY (experience_version_id, user_id)
        REFERENCES career_experience_version (experience_version_id, user_id)
);

CREATE FUNCTION prevent_job_analysis_result_update()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'job analysis result is immutable';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_job_analysis_result_immutable
BEFORE UPDATE ON job_analysis_result
FOR EACH ROW EXECUTE FUNCTION prevent_job_analysis_result_update();

CREATE TRIGGER trg_requirement_match_result_immutable
BEFORE UPDATE ON requirement_match_result
FOR EACH ROW EXECUTE FUNCTION prevent_job_analysis_result_update();

CREATE TRIGGER trg_match_evidence_snapshot_immutable
BEFORE UPDATE ON match_evidence_snapshot
FOR EACH ROW EXECUTE FUNCTION prevent_job_analysis_result_update();
