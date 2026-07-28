package com.careerfit.common.async.application;

import com.careerfit.common.async.domain.JobType;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class JobHandlerRegistry {

    private static final String HANDLER_NOT_FOUND = "HANDLER_NOT_FOUND";

    private final Map<JobType, JobHandler> handlers;

    public JobHandlerRegistry(List<JobHandler> handlers) {
        EnumMap<JobType, JobHandler> mappedHandlers = new EnumMap<>(JobType.class);
        for (JobHandler handler : handlers) {
            JobHandler duplicate = mappedHandlers.put(handler.type(), handler);
            if (duplicate != null) {
                throw new IllegalStateException(
                        "작업 유형별 Handler는 하나만 등록할 수 있습니다: " + handler.type());
            }
        }
        this.handlers = Map.copyOf(mappedHandlers);
    }

    public JobHandler resolve(JobType type) {
        JobHandler handler = handlers.get(type);
        if (handler == null) {
            throw new JobHandlerException(
                    HANDLER_NOT_FOUND, "등록된 작업 Handler가 없습니다: " + type);
        }
        return handler;
    }
}
