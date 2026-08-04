CREATE TABLE career_extraction_candidate (
    candidate_id UUID PRIMARY KEY,
    document_analysis_id UUID NOT NULL REFERENCES career_document_analysis(document_analysis_id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    candidate_type VARCHAR(50) NOT NULL,
    organization VARCHAR(300),
    role VARCHAR(300),
    period VARCHAR(200),
    description TEXT NOT NULL,
    status VARCHAR(30) NOT NULL CHECK (status IN ('PENDING_REVIEW')),
    revision_no INTEGER NOT NULL CHECK (revision_no > 0),
    model VARCHAR(200) NOT NULL,
    prompt_version VARCHAR(100) NOT NULL,
    schema_version VARCHAR(100) NOT NULL,
    ai_call_execution_id UUID NOT NULL REFERENCES ai_call_execution(ai_call_execution_id),
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_career_extraction_candidate_owner UNIQUE (candidate_id, user_id)
);

CREATE INDEX idx_career_extraction_candidate_analysis
    ON career_extraction_candidate (user_id, document_analysis_id, created_at);

CREATE TABLE experience_evidence (
    evidence_id UUID PRIMARY KEY,
    candidate_id UUID NOT NULL,
    document_analysis_id UUID NOT NULL,
    document_id UUID NOT NULL REFERENCES career_document(career_document_id),
    user_id UUID NOT NULL,
    page_number INTEGER NOT NULL CHECK (page_number > 0),
    excerpt TEXT NOT NULL CHECK (length(trim(excerpt)) > 0),
    CONSTRAINT fk_experience_evidence_candidate_owner
        FOREIGN KEY (candidate_id, user_id)
        REFERENCES career_extraction_candidate(candidate_id, user_id) ON DELETE CASCADE,
    CONSTRAINT fk_experience_evidence_page
        FOREIGN KEY (document_analysis_id, page_number)
        REFERENCES career_document_page(document_analysis_id, page_number)
);

CREATE INDEX idx_experience_evidence_candidate
    ON experience_evidence (user_id, candidate_id, page_number);
