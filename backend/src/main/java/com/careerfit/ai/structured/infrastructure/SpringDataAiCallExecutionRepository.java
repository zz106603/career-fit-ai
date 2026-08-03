package com.careerfit.ai.structured.infrastructure;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataAiCallExecutionRepository extends JpaRepository<AiCallExecutionEntity, UUID> {}
