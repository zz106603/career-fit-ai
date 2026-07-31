package com.careerfit.career.document.application;

import java.util.Objects;

public record CareerDocumentUpload(String originalName, String contentType, byte[] content) {

    public CareerDocumentUpload {
        Objects.requireNonNull(content, "업로드 파일은 필수입니다.");
        content = content.clone();
    }

    @Override
    public byte[] content() {
        return content.clone();
    }
}
