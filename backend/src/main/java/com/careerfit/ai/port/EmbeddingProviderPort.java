package com.careerfit.ai.port;

import com.careerfit.ai.port.model.EmbeddingRequest;
import com.careerfit.ai.port.model.EmbeddingResponse;

/** Provider 제품과 무관하게 임베딩 생성을 요청하는 경계다. */
public interface EmbeddingProviderPort {

    EmbeddingResponse embed(EmbeddingRequest request);
}
