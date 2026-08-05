package com.careerfit.career.extraction.infrastructure;

import java.util.UUID;

/** 후보 Evidence와 문서 표시명을 확정 버전 Snapshot으로 옮기기 위한 JPA 조회 결과다. */
public record ConfirmedEvidenceSource(
        UUID candidateId, UUID analysisId, UUID documentId, UUID userId,
        String documentName, int pageNumber, String excerpt) {}
