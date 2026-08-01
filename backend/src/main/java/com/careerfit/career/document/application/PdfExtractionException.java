package com.careerfit.career.document.application;

public class PdfExtractionException extends RuntimeException {

    private final CareerDocumentExtractionFailure failure;

    public PdfExtractionException(CareerDocumentExtractionFailure failure, String message) {
        super(message);
        this.failure = failure;
    }

    public CareerDocumentExtractionFailure failure() {
        return failure;
    }
}
