ALTER TABLE job_requirement
    ADD CONSTRAINT uq_job_requirement_owner
    UNIQUE (requirement_id, user_id);

CREATE TABLE career_candidate_search (
    candidate_search_id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    requirement_id UUID NOT NULL,
    query_embedding_version VARCHAR(100) NOT NULL
        CHECK (length(trim(query_embedding_version)) > 0),
    search_version VARCHAR(100) NOT NULL
        CHECK (length(trim(search_version)) > 0),
    searched_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_career_candidate_search_owner
        UNIQUE (candidate_search_id, user_id),
    CONSTRAINT fk_career_candidate_search_requirement_owner
        FOREIGN KEY (requirement_id, user_id)
        REFERENCES job_requirement (requirement_id, user_id)
);

CREATE INDEX idx_career_candidate_search_requirement
    ON career_candidate_search (user_id, requirement_id, searched_at DESC);

CREATE TABLE career_search_candidate_snapshot (
    candidate_search_id UUID NOT NULL,
    user_id UUID NOT NULL,
    experience_version_id UUID NOT NULL,
    score DOUBLE PRECISION NOT NULL CHECK (score >= -1 AND score <= 1),
    candidate_rank INTEGER NOT NULL CHECK (candidate_rank > 0),
    embedding_version VARCHAR(100) NOT NULL
        CHECK (length(trim(embedding_version)) > 0),
    PRIMARY KEY (candidate_search_id, candidate_rank),
    CONSTRAINT fk_career_search_candidate_search_owner
        FOREIGN KEY (candidate_search_id, user_id)
        REFERENCES career_candidate_search (candidate_search_id, user_id),
    CONSTRAINT fk_career_search_candidate_version_owner
        FOREIGN KEY (experience_version_id, user_id)
        REFERENCES career_experience_version (experience_version_id, user_id),
    CONSTRAINT uq_career_search_candidate_version
        UNIQUE (candidate_search_id, experience_version_id)
);

CREATE FUNCTION prevent_career_candidate_snapshot_update()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'career candidate search snapshot is immutable';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_career_candidate_search_immutable
BEFORE UPDATE ON career_candidate_search
FOR EACH ROW
EXECUTE FUNCTION prevent_career_candidate_snapshot_update();

CREATE TRIGGER trg_career_search_candidate_snapshot_immutable
BEFORE UPDATE ON career_search_candidate_snapshot
FOR EACH ROW
EXECUTE FUNCTION prevent_career_candidate_snapshot_update();
