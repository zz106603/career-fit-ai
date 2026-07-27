package com.careerfit.job.structure.application;

public class InvalidJobStructureException extends RuntimeException {

    public InvalidJobStructureException() {
        super("유효한 채용공고 구조를 만들 수 없습니다.");
    }
}
