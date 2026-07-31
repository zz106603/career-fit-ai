package com.careerfit.career.document.application;

import com.careerfit.career.document.domain.CareerDocument;

public record CareerDocumentContent(CareerDocument document, byte[] content) {

    public CareerDocumentContent {
        content = content.clone();
    }

    @Override
    public byte[] content() {
        return content.clone();
    }
}
