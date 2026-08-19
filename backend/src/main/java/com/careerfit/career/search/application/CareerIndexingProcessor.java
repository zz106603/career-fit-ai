package com.careerfit.career.search.application;

import com.careerfit.ai.port.EmbeddingProviderPort;
import com.careerfit.ai.port.model.EmbeddingRequest;
import com.careerfit.ai.port.model.EmbeddingResponse;
import com.careerfit.career.domain.CareerExperienceVersionId;
import com.careerfit.career.search.domain.CareerSearchDocument;
import com.careerfit.common.async.application.JobHandlerException;
import com.careerfit.identity.UserId;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
/** 저장된 Snapshot으로 embedding을 만든 뒤 성공 또는 실패 상태를 별도 트랜잭션에 기록한다. */
public class CareerIndexingProcessor {

    static final String FAILURE_CODE = "CAREER_INDEXING_FAILED";

    private final CareerIndexingPersistence persistence;
    private final EmbeddingProviderPort embeddingProvider;

    public CareerIndexingProcessor(
            CareerIndexingPersistence persistence,
            EmbeddingProviderPort embeddingProvider) {
        this.persistence = persistence;
        this.embeddingProvider = embeddingProvider;
    }

    public CareerSearchDocument process(
            UserId userId,
            CareerExperienceVersionId versionId,
            String inputVersion) {
        Optional<CareerIndexingInput> input = persistence.start(userId, versionId, inputVersion);
        if (input.isEmpty()) {
            return persistence.find(userId, versionId);
        }

        try {
            EmbeddingResponse response = embeddingProvider.embed(
                    new EmbeddingRequest(input.orElseThrow().searchableText()));
            return persistence.complete(
                    userId, versionId, response.vector(), response.model());
        } catch (RuntimeException exception) {
            persistence.fail(userId, versionId, FAILURE_CODE);
            throw new JobHandlerException(FAILURE_CODE, "경력 색인 Provider 호출에 실패했습니다.");
        }
    }
}
