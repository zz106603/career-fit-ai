package com.careerfit.ai.structured.application;

import com.careerfit.ai.structured.domain.AiCallAttempt;
import com.careerfit.ai.structured.domain.AiCallExecution;

public interface AiCallObservationRepository {

    void saveExecution(AiCallExecution execution);

    void saveAttempt(AiCallAttempt attempt);
}
