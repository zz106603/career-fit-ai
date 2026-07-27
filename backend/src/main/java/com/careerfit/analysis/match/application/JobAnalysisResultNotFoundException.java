package com.careerfit.analysis.match.application;

public class JobAnalysisResultNotFoundException extends RuntimeException {

    public JobAnalysisResultNotFoundException() {
        super("공고 분석 결과를 찾을 수 없습니다.");
    }
}
