package com.careerfit.career.document.application;

public class InvalidPdfException extends RuntimeException {

    private final PdfValidationFailure failure;

    public InvalidPdfException(PdfValidationFailure failure, String message) {
        super(message);
        this.failure = failure;
    }

    public PdfValidationFailure failure() {
        return failure;
    }
}
