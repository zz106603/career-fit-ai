CREATE TABLE career_experience (
    experience_id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT uq_career_experience_owner UNIQUE (experience_id, user_id)
);

CREATE INDEX idx_career_experience_user
    ON career_experience (user_id, experience_id)
    WHERE deleted_at IS NULL;

CREATE TABLE career_experience_version (
    experience_version_id UUID PRIMARY KEY,
    experience_id UUID NOT NULL,
    user_id UUID NOT NULL,
    version_no INTEGER NOT NULL CHECK (version_no > 0),
    source_type VARCHAR(20) NOT NULL CHECK (source_type IN ('DOCUMENT', 'USER_DIRECT')),
    experience_type VARCHAR(100),
    title VARCHAR(200) NOT NULL CHECK (length(trim(title)) > 0),
    organization VARCHAR(200),
    start_date DATE,
    end_date DATE,
    role VARCHAR(500),
    responsibilities TEXT,
    problem TEXT,
    action TEXT,
    outcome TEXT,
    technologies TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    confirmed_at TIMESTAMPTZ,
    superseded_at TIMESTAMPTZ,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT fk_career_experience_version_owner
        FOREIGN KEY (experience_id, user_id)
        REFERENCES career_experience (experience_id, user_id),
    CONSTRAINT uq_career_experience_version UNIQUE (experience_id, version_no),
    CONSTRAINT chk_career_experience_period
        CHECK (start_date IS NULL OR end_date IS NULL OR start_date <= end_date),
    CONSTRAINT chk_career_experience_required_content
        CHECK (
            (role IS NOT NULL AND length(trim(role)) > 0)
            OR (
                responsibilities IS NOT NULL
                AND length(trim(responsibilities)) > 0
            )
        )
);

CREATE INDEX idx_career_experience_version_current
    ON career_experience_version (user_id, experience_id, version_no DESC)
    WHERE confirmed_at IS NOT NULL
      AND superseded_at IS NULL
      AND deleted_at IS NULL;

CREATE UNIQUE INDEX uq_career_experience_current_version
    ON career_experience_version (experience_id)
    WHERE confirmed_at IS NOT NULL
      AND superseded_at IS NULL
      AND deleted_at IS NULL;
