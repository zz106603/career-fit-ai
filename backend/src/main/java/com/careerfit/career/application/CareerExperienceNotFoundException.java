package com.careerfit.career.application;

public class CareerExperienceNotFoundException extends RuntimeException {

    public CareerExperienceNotFoundException() {
        super("현재 사용자의 경력을 찾을 수 없습니다.");
    }
}
