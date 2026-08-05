package com.careerfit.career.extraction.application;

public class CareerCandidateNotFoundException extends RuntimeException {
    public CareerCandidateNotFoundException() {
        super("경력 후보를 찾을 수 없습니다.");
    }
}
