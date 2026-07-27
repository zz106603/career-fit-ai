ALTER TABLE career_experience_version
    ADD CONSTRAINT uq_career_experience_version_owner
    UNIQUE (experience_version_id, user_id);

CREATE TABLE career_search_document (
    search_document_id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    experience_version_id UUID NOT NULL,
    searchable_text TEXT NOT NULL CHECK (length(trim(searchable_text)) > 0),
    content_hash CHAR(64) NOT NULL,
    embedding vector(8),
    embedding_version VARCHAR(100),
    index_status VARCHAR(20) NOT NULL CHECK (index_status IN ('PENDING', 'INDEXED')),
    created_at TIMESTAMPTZ NOT NULL,
    indexed_at TIMESTAMPTZ,
    CONSTRAINT fk_career_search_document_owner
        FOREIGN KEY (experience_version_id, user_id)
        REFERENCES career_experience_version (experience_version_id, user_id),
    CONSTRAINT uq_career_search_document_version
        UNIQUE (user_id, experience_version_id),
    CONSTRAINT chk_career_search_document_indexed
        CHECK (
            (index_status = 'PENDING'
                AND embedding IS NULL
                AND embedding_version IS NULL
                AND indexed_at IS NULL)
            OR
            (index_status = 'INDEXED'
                AND embedding IS NOT NULL
                AND embedding_version IS NOT NULL
                AND indexed_at IS NOT NULL)
        )
);

CREATE INDEX idx_career_search_document_user_status
    ON career_search_document (user_id, index_status);
