package com.careerfit.career.extraction.application;

import com.careerfit.career.application.CareerExperienceNotFoundException;
import com.careerfit.career.application.CareerExperienceRepository;
import com.careerfit.career.domain.CareerExperienceId;
import com.careerfit.career.domain.CareerExperienceVersion;
import com.careerfit.career.domain.CareerExperienceVersionId;
import com.careerfit.identity.CurrentUserProvider;
import com.careerfit.identity.UserId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
/** 분석 기준으로 사용할 수 있는 현재 확정 경력과 불변 과거 버전을 조회한다. */
public class ConfirmedCareerQueryService {
    private final CareerExperienceRepository experiences;
    private final ConfirmedCareerEvidenceRepository evidences;
    private final CurrentUserProvider currentUserProvider;

    public ConfirmedCareerQueryService(
            CareerExperienceRepository experiences,
            ConfirmedCareerEvidenceRepository evidences,
            CurrentUserProvider currentUserProvider) {
        this.experiences = experiences;
        this.evidences = evidences;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public List<ConfirmedCareerVersionView> findCurrent() {
        UserId userId = currentUserProvider.currentUserId();
        return withEvidences(userId, experiences.findCurrentConfirmed(userId));
    }

    @Transactional(readOnly = true)
    public List<ConfirmedCareerVersionView> findVersions(CareerExperienceId experienceId) {
        UserId userId = currentUserProvider.currentUserId();
        List<CareerExperienceVersion> versions =
                experiences.findConfirmedVersions(userId, experienceId);
        if (versions.isEmpty()) throw new CareerExperienceNotFoundException();
        return withEvidences(userId, versions);
    }

    private List<ConfirmedCareerVersionView> withEvidences(
            UserId userId, List<CareerExperienceVersion> versions) {
        List<CareerExperienceVersionId> versionIds =
                versions.stream().map(CareerExperienceVersion::id).toList();
        Map<CareerExperienceVersionId, List<ConfirmedCareerEvidenceView>> byVersion =
                evidences.findAll(userId, versionIds).stream()
                        .collect(Collectors.groupingBy(ConfirmedCareerEvidenceView::versionId));
        return versions.stream()
                .map(version -> new ConfirmedCareerVersionView(
                        version, byVersion.getOrDefault(version.id(), List.of())))
                .toList();
    }
}
