package com.careerfit.ai.structured.application;

import com.careerfit.ai.structured.domain.AiCallAttempt;
import com.careerfit.ai.structured.domain.AiCallExecution;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiCallObservationPersistence {

    private final AiCallObservationRepository repository;

    public AiCallObservationPersistence(AiCallObservationRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void saveExecution(AiCallExecution execution) {
        repository.saveExecution(execution);
    }

    @Transactional
    public void saveAttempt(AiCallAttempt attempt) {
        repository.saveAttempt(attempt);
    }
}
