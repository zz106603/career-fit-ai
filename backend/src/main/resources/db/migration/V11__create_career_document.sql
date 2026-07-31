CREATE TABLE career_document (
    career_document_id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    original_name VARCHAR(500) NOT NULL,
    storage_reference VARCHAR(1000) NOT NULL,
    byte_size BIGINT NOT NULL CHECK (byte_size > 0 AND byte_size <= 10485760),
    content_type VARCHAR(100) NOT NULL CHECK (content_type = 'application/pdf'),
    checksum_sha256 VARCHAR(64) NOT NULL,
    page_count INTEGER NOT NULL CHECK (page_count BETWEEN 1 AND 50),
    uploaded_at TIMESTAMPTZ NOT NULL,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT uq_career_document_storage_reference UNIQUE (storage_reference)
);

CREATE INDEX idx_career_document_user_active
    ON career_document (user_id, uploaded_at DESC)
    WHERE deleted_at IS NULL;
