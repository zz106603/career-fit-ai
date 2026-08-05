package com.careerfit.career.extraction.domain;

/** 후보 검토 상태이며 문서 분석 작업의 성공·실패 상태와는 별개다. */
public enum CareerExtractionCandidateStatus {
    PENDING_REVIEW, // AI 생성 직후 사용자 검토 대기
    EDITED,         // 사용자가 수정했지만 아직 미확정
    CONFIRMED,      // 명시적 확정 완료(BL-024에서 사용)
    REJECTED        // 논리 삭제되어 검토·분석 대상에서 제외
}
