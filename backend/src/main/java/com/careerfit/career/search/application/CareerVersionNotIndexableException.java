package com.careerfit.career.search.application;

public class CareerVersionNotIndexableException extends RuntimeException {

    public CareerVersionNotIndexableException() {
        super("현재 사용자의 확정된 최신 경력 버전만 색인할 수 있습니다.");
    }
}
