package com.careerfit.career.document.application;

public class CareerDocumentNotFoundException extends RuntimeException {

    public CareerDocumentNotFoundException() {
        super("경력 문서를 찾을 수 없습니다.");
    }
}
