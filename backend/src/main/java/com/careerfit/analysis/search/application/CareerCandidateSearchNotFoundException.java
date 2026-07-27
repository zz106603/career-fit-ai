package com.careerfit.analysis.search.application;

public class CareerCandidateSearchNotFoundException extends RuntimeException {

    public CareerCandidateSearchNotFoundException() {
        super("경력 후보 검색 결과를 찾을 수 없습니다.");
    }
}
