package com.careerfit.career.search.application;

import com.careerfit.career.application.CareerExperienceRepository;
import com.careerfit.career.domain.CareerExperienceVersion;
import com.careerfit.career.domain.CareerExperienceVersionId;
import com.careerfit.career.search.domain.CareerSearchDocument;
import com.careerfit.common.async.application.JobExecutionService;
import com.careerfit.common.async.domain.JobExecution;
import com.careerfit.common.async.domain.JobType;
import com.careerfit.identity.UserId;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.HexFormat;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
/** 확정 경력 버전에 검색 문서와 내구성 있는 색인 작업을 같은 트랜잭션으로 등록한다. */
public class CareerIndexJobService {

    private final CareerExperienceRepository experienceRepository;
    private final CareerSearchDocumentRepository searchDocumentRepository;
    private final CareerSearchTextBuilder textBuilder;
    private final JobExecutionService executionService;
    private final Clock clock;

    public CareerIndexJobService(
            CareerExperienceRepository experienceRepository,
            CareerSearchDocumentRepository searchDocumentRepository,
            CareerSearchTextBuilder textBuilder,
            JobExecutionService executionService,
            Clock clock) {
        this.experienceRepository = experienceRepository;
        this.searchDocumentRepository = searchDocumentRepository;
        this.textBuilder = textBuilder;
        this.executionService = executionService;
        this.clock = clock;
    }

    @Transactional
    public JobExecution enqueue(UserId userId, CareerExperienceVersionId versionId) {
        CareerExperienceVersion version = experienceRepository
                .findCurrentConfirmedVersion(userId, versionId)
                .orElseThrow(CareerVersionNotIndexableException::new);
        String searchableText = textBuilder.build(version);
        searchDocumentRepository.savePending(CareerSearchDocument.pending(
                userId, versionId, searchableText, sha256(searchableText), clock.instant()));

        String inputVersion = Integer.toString(version.versionNo());
        return executionService.create(
                userId.value(),
                JobType.CAREER_INDEXING,
                versionId.value(),
                inputVersion,
                duplicateKey(versionId, inputVersion));
    }

    private String duplicateKey(CareerExperienceVersionId versionId, String inputVersion) {
        return JobType.CAREER_INDEXING + ":" + versionId.value() + ":" + inputVersion;
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", exception);
        }
    }
}
