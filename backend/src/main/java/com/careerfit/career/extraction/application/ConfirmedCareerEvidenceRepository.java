package com.careerfit.career.extraction.application;

import com.careerfit.career.domain.CareerExperienceVersionId;
import com.careerfit.identity.UserId;
import java.util.UUID;

/** 후보 Evidence를 확정 경력 버전에 귀속해 불변 provenance로 보존하는 저장소 경계다. */
public interface ConfirmedCareerEvidenceRepository {
    /** 후보에 연결된 모든 원문 Evidence를 최초 확정 버전으로 복사한다. */
    int copyFromCandidate(UserId userId, UUID candidateId, CareerExperienceVersionId versionId);
    /** 기존 확정 버전의 Evidence를 수정 중인 다음 버전으로 복사한다. */
    int copyFromVersion(UserId userId, CareerExperienceVersionId source, CareerExperienceVersionId target);
    /** DOCUMENT 버전을 확정하기 전에 원문 Evidence 존재 여부를 확인한다. */
    boolean exists(UserId userId, CareerExperienceVersionId versionId);
}
