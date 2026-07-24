package com.careerfit.ai.port;

import com.careerfit.ai.port.model.LlmRequest;
import com.careerfit.ai.port.model.LlmResponse;

/** Provider 제품과 무관하게 텍스트 생성을 요청하는 경계다. */
public interface LlmProviderPort {

    LlmResponse generate(LlmRequest request);
}
