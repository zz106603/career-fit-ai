package com.careerfit.career.document.application;

public enum CareerDocumentExtractionFailure {
    CAREER_DOCUMENT_NOT_FOUND,
    FILE_STORAGE_READ_FAILED,
    PDF_PARSE_FAILED,
    PDF_ENCRYPTED,
    PDF_TEXT_EMPTY,
    PAGE_TEXT_SAVE_FAILED
}
