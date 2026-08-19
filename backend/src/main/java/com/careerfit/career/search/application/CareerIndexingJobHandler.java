package com.careerfit.career.search.application;

import com.careerfit.career.domain.CareerExperienceVersionId;
import com.careerfit.common.async.application.JobHandler;
import com.careerfit.common.async.domain.JobExecution;
import com.careerfit.common.async.domain.JobType;
import com.careerfit.identity.UserId;
import org.springframework.stereotype.Component;

@Component
/** Dispatcher가 재발견한 경력 색인 작업을 저장된 사용자·버전 Snapshot으로 실행한다. */
public class CareerIndexingJobHandler implements JobHandler {

    private final CareerIndexingProcessor processor;

    public CareerIndexingJobHandler(CareerIndexingProcessor processor) {
        this.processor = processor;
    }

    @Override
    public JobType type() {
        return JobType.CAREER_INDEXING;
    }

    @Override
    public void handle(JobExecution execution) {
        processor.process(
                new UserId(execution.userId()),
                new CareerExperienceVersionId(execution.targetId()),
                execution.inputVersion());
    }
}
