ALTER TABLE career_extraction_candidate
    DROP CONSTRAINT career_extraction_candidate_status_check;

ALTER TABLE career_extraction_candidate
    ADD CONSTRAINT career_extraction_candidate_status_check
        CHECK (status IN ('PENDING_REVIEW', 'EDITED', 'CONFIRMED', 'REJECTED'));
