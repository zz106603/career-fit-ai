package com.careerfit.career.application;

public class CareerVersionAlreadyConfirmedException extends RuntimeException {

    public CareerVersionAlreadyConfirmedException() {
        super("이미 확정된 경력 버전입니다.");
    }
}
