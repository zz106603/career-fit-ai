package com.careerfit.job.application;

public class JobPostingNotFoundException extends RuntimeException {

    public JobPostingNotFoundException() {
        super("채용공고를 찾을 수 없습니다.");
    }
}
