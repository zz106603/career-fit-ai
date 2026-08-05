CREATE TABLE career_experience_evidence (
    evidence_id UUID PRIMARY KEY,
    experience_version_id UUID NOT NULL,
    candidate_id UUID NOT NULL,
    document_analysis_id UUID NOT NULL,
    document_id UUID NOT NULL,
    user_id UUID NOT NULL,
    document_name VARCHAR(500) NOT NULL,
    page_number INTEGER NOT NULL CHECK (page_number > 0),
    excerpt TEXT NOT NULL CHECK (length(trim(excerpt)) > 0),
    CONSTRAINT fk_career_experience_evidence_version_owner
        FOREIGN KEY (experience_version_id, user_id)
        REFERENCES career_experience_version(experience_version_id, user_id) ON DELETE CASCADE,
    CONSTRAINT fk_career_experience_evidence_candidate_owner
        FOREIGN KEY (candidate_id, user_id)
        REFERENCES career_extraction_candidate(candidate_id, user_id),
    CONSTRAINT fk_career_experience_evidence_document
        FOREIGN KEY (document_id)
        REFERENCES career_document(career_document_id),
    CONSTRAINT fk_career_experience_evidence_page
        FOREIGN KEY (document_analysis_id, page_number)
        REFERENCES career_document_page(document_analysis_id, page_number)
);

CREATE INDEX idx_career_experience_evidence_version
    ON career_experience_evidence (user_id, experience_version_id, page_number);
