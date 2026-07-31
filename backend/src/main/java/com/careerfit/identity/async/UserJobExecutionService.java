package com.careerfit.identity.async;

import com.careerfit.common.async.application.JobExecutionService;
import com.careerfit.common.async.application.JobRecoveryService;
import com.careerfit.common.async.domain.JobExecution;
import com.careerfit.common.async.domain.JobExecutionId;
import com.careerfit.identity.CurrentUserProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 인증된 사용자가 자신의 비동기 작업 상태를 조회하고 재실행하는 진입점이다.
 * Dispatcher와 Worker의 시스템 실행 경로는 {@link JobExecutionService}를 직접 사용한다.
 */
@Service
public class UserJobExecutionService {

    private final JobExecutionService executionService;
    private final JobRecoveryService recoveryService;
    private final CurrentUserProvider currentUserProvider;

    public UserJobExecutionService(
            JobExecutionService executionService,
            JobRecoveryService recoveryService,
            CurrentUserProvider currentUserProvider) {
        this.executionService = executionService;
        this.recoveryService = recoveryService;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public JobExecution find(JobExecutionId executionId) {
        return executionService.find(currentUserProvider.currentUserId().value(), executionId);
    }

    @Transactional
    public JobExecution rerunFailed(JobExecutionId executionId) {
        return recoveryService.rerunFailed(
                currentUserProvider.currentUserId().value(), executionId);
    }
}
