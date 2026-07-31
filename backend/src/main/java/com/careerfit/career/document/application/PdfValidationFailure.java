package com.careerfit.career.document.application;

public enum PdfValidationFailure {
    EMPTY,
    CONTENT_TYPE,
    TOO_LARGE,
    SIGNATURE,
    CORRUPTED,
    ENCRYPTED,
    PAGE_COUNT
}
