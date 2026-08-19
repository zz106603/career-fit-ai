ALTER TABLE career_search_document
    DROP CONSTRAINT chk_career_search_document_indexed;

ALTER TABLE career_search_document
    ADD COLUMN indexing_started_at TIMESTAMPTZ,
    ADD COLUMN failed_at TIMESTAMPTZ,
    ADD COLUMN failure_code VARCHAR(200);

UPDATE career_search_document
SET indexing_started_at = COALESCE(indexed_at, created_at)
WHERE index_status = 'INDEXED';

ALTER TABLE career_search_document
    DROP CONSTRAINT career_search_document_index_status_check;

ALTER TABLE career_search_document
    ADD CONSTRAINT career_search_document_index_status_check
        CHECK (index_status IN ('PENDING', 'INDEXING', 'INDEXED', 'FAILED')),
    ADD CONSTRAINT chk_career_search_document_index_state CHECK (
        (
            index_status = 'PENDING'
            AND embedding IS NULL
            AND embedding_version IS NULL
            AND indexing_started_at IS NULL
            AND indexed_at IS NULL
            AND failed_at IS NULL
            AND failure_code IS NULL
        )
        OR (
            index_status = 'INDEXING'
            AND embedding IS NULL
            AND embedding_version IS NULL
            AND indexing_started_at IS NOT NULL
            AND indexed_at IS NULL
            AND failed_at IS NULL
            AND failure_code IS NULL
        )
        OR (
            index_status = 'INDEXED'
            AND embedding IS NOT NULL
            AND embedding_version IS NOT NULL
            AND indexing_started_at IS NOT NULL
            AND indexed_at IS NOT NULL
            AND failed_at IS NULL
            AND failure_code IS NULL
        )
        OR (
            index_status = 'FAILED'
            AND embedding IS NULL
            AND embedding_version IS NULL
            AND indexing_started_at IS NOT NULL
            AND indexed_at IS NULL
            AND failed_at IS NOT NULL
            AND length(trim(failure_code)) > 0
        )
    );
