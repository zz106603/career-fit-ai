package com.careerfit.career.search.application;

import com.careerfit.career.application.CareerExperienceRepository;
import com.careerfit.career.domain.CareerExperienceVersion;
import com.careerfit.career.domain.CareerExperienceVersionId;
import com.careerfit.career.search.domain.CareerSearchDocument;
import com.careerfit.identity.CurrentUserProvider;
import com.careerfit.identity.UserId;
import org.springframework.stereotype.Service;

@Service
/** 확정된 최신 경력 버전만 검색 문서와 Vector 색인 대상으로 전환한다. */
public class CareerIndexService {

    private final CareerExperienceRepository experienceRepository;
    private final CareerIndexingProcessor processor;
    private final CurrentUserProvider currentUserProvider;

    public CareerIndexService(
            CareerExperienceRepository experienceRepository,
            CareerIndexingProcessor processor,
            CurrentUserProvider currentUserProvider) {
        this.experienceRepository = experienceRepository;
        this.processor = processor;
        this.currentUserProvider = currentUserProvider;
    }

    public CareerSearchDocument index(CareerExperienceVersionId versionId) {
        UserId userId = currentUserProvider.currentUserId();
        CareerExperienceVersion version = experienceRepository
                .findCurrentConfirmedVersion(userId, versionId)
                .orElseThrow(CareerVersionNotIndexableException::new);
        return processor.process(userId, versionId, Integer.toString(version.versionNo()));
    }
}
