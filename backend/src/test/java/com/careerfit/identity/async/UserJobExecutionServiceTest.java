package com.careerfit.identity.async;

import static com.careerfit.identity.security.SecurityContextUserScope.authenticate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.careerfit.common.async.application.JobExecutionService;
import com.careerfit.common.async.application.JobRecoveryService;
import com.careerfit.common.async.domain.JobExecution;
import com.careerfit.common.async.domain.JobExecutionId;
import com.careerfit.common.async.domain.JobExecutionStatus;
import com.careerfit.common.async.domain.JobType;
import com.careerfit.identity.RequestCurrentUserContext;
import com.careerfit.identity.development.DevelopmentUsers;
import com.careerfit.identity.security.SecurityContextCurrentUserProvider;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("사용자 비동기 작업 서비스 테스트")
class UserJobExecutionServiceTest {

    @Mock
    private JobExecutionService executionService;

    @Mock
    private JobRecoveryService recoveryService;

    @Test
    @DisplayName("작업 조회에는 SecurityContext의 사용자 ID를 전달한다")
    void 작업_조회에는_시큐리티_컨텍스트의_사용자_ID를_전달한다() {
        JobExecutionId executionId = JobExecutionId.newId();
        JobExecution execution = execution(executionId);
        when(executionService.find(
                        DevelopmentUsers.USER_A.userId().value(), executionId))
                .thenReturn(execution);
        UserJobExecutionService service = service();

        try (var ignored = authenticate(DevelopmentUsers.USER_A)) {
            assertThat(service.find(executionId)).isEqualTo(execution);
        }

        verify(executionService)
                .find(DevelopmentUsers.USER_A.userId().value(), executionId);
    }

    @Test
    @DisplayName("작업 재실행에는 SecurityContext의 사용자 ID를 전달한다")
    void 작업_재실행에는_시큐리티_컨텍스트의_사용자_ID를_전달한다() {
        JobExecutionId executionId = JobExecutionId.newId();
        JobExecution execution = execution(executionId);
        when(recoveryService.rerunFailed(
                        DevelopmentUsers.USER_B.userId().value(), executionId))
                .thenReturn(execution);
        UserJobExecutionService service = service();

        try (var ignored = authenticate(DevelopmentUsers.USER_B)) {
            assertThat(service.rerunFailed(executionId)).isEqualTo(execution);
        }

        verify(recoveryService)
                .rerunFailed(DevelopmentUsers.USER_B.userId().value(), executionId);
    }

    private UserJobExecutionService service() {
        return new UserJobExecutionService(
                executionService,
                recoveryService,
                new SecurityContextCurrentUserProvider(new RequestCurrentUserContext()));
    }

    private JobExecution execution(JobExecutionId executionId) {
        return new JobExecution(
                executionId,
                DevelopmentUsers.USER_A.userId().value(),
                JobType.CAREER_DOCUMENT_EXTRACTION,
                UUID.randomUUID(),
                "input-v1",
                "ownership:test",
                JobExecutionStatus.FAILED,
                0,
                "FAILED",
                Instant.parse("2026-07-31T00:00:00Z"),
                Instant.parse("2026-07-31T00:01:00Z"),
                Instant.parse("2026-07-31T00:02:00Z"));
    }
}
