package com.careerfit.ai.structured.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataAiCallAttemptRepository
        extends JpaRepository<AiCallAttemptEntity, AiCallAttemptId> {}
