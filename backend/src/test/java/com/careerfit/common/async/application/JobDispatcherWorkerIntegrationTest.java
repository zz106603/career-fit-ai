package com.careerfit.common.async.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.careerfit.PostgresIntegrationTest;
import com.careerfit.common.async.domain.JobExecution;
import com.careerfit.common.async.domain.JobExecutionStatus;
import com.careerfit.common.async.domain.JobType;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest(properties = "career-fit.async.dispatcher.batch-size=2")
@DisplayName("Dispatcher와 Worker 통합 테스트")
class JobDispatcherWorkerIntegrationTest extends PostgresIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-07-28T00:00:00Z");

    @Autowired
    private JobExecutionService executionService;

    @Autowired
    private JobDispatcher dispatcher;

    @Autowired
    private JobWorker worker;

    @Autowired
    private RecordingSuccessHandler successHandler;

    @Autowired
    private JdbcClient jdbcClient;

    @BeforeEach
    void 데이터베이스와_Handler를_초기화한다() {
        jdbcClient.sql("TRUNCATE job_execution").update();
        successHandler.reset();
    }

    @Test
    @DisplayName("Dispatcher는 설정된 batch 크기만 조회해 작업 유형 Handler를 실행한다")
    void Dispatcher는_설정된_batch_크기만_조회해_작업_유형_Handler를_실행한다() {
        UUID userId = UUID.randomUUID();
        create(userId, JobType.CAREER_DOCUMENT_EXTRACTION, "batch-1");
        create(userId, JobType.CAREER_DOCUMENT_EXTRACTION, "batch-2");
        create(userId, JobType.CAREER_DOCUMENT_EXTRACTION, "batch-3");

        int executed = dispatcher.dispatchBatch();

        assertThat(executed).isEqualTo(2);
        assertThat(successHandler.executionCount()).isEqualTo(2);
        assertThat(countByStatus(JobExecutionStatus.SUCCEEDED)).isEqualTo(2);
        assertThat(countByStatus(JobExecutionStatus.QUEUED)).isEqualTo(1);
    }

    @Test
    @DisplayName("두 Worker가 동시에 선점해도 Handler는 한 번만 실행된다")
    void 두_Worker가_동시에_선점해도_Handler는_한_번만_실행된다()
            throws Exception {
        JobExecution queued = create(
                UUID.randomUUID(), JobType.CAREER_DOCUMENT_EXTRACTION, "claim-once");
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<Boolean> first =
                    executor.submit(() -> executeAfterSignal(queued, ready, start));
            Future<Boolean> second =
                    executor.submit(() -> executeAfterSignal(queued, ready, start));

            ready.await();
            start.countDown();
            assertThat(java.util.List.of(first.get(), second.get()))
                    .containsExactlyInAnyOrder(true, false);
        }
        assertThat(successHandler.executionCount()).isEqualTo(1);
        assertThat(executionService.find(queued.userId(), queued.id()).status())
                .isEqualTo(JobExecutionStatus.SUCCEEDED);
    }

    @Test
    @DisplayName("Handler의 업무 실패 코드를 FAILED 상태에 저장한다")
    void Handler의_업무_실패_코드를_FAILED_상태에_저장한다() {
        JobExecution queued = create(
                UUID.randomUUID(), JobType.CAREER_CANDIDATE_EXTRACTION, "handler-failure");

        assertThat(dispatcher.dispatchBatch()).isEqualTo(1);

        JobExecution failed = executionService.find(queued.userId(), queued.id());
        assertThat(failed.status()).isEqualTo(JobExecutionStatus.FAILED);
        assertThat(failed.failureCode()).isEqualTo("CANDIDATE_STRUCTURE_INVALID");
        assertThat(failed.claimedAt()).isEqualTo(NOW);
        assertThat(failed.completedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("등록되지 않은 작업 유형은 HANDLER_NOT_FOUND로 실패한다")
    void 등록되지_않은_작업_유형은_HANDLER_NOT_FOUND로_실패한다() {
        JobExecution queued =
                create(UUID.randomUUID(), JobType.JOB_ANALYSIS, "missing-handler");

        assertThat(dispatcher.dispatchBatch()).isEqualTo(1);

        JobExecution failed = executionService.find(queued.userId(), queued.id());
        assertThat(failed.status()).isEqualTo(JobExecutionStatus.FAILED);
        assertThat(failed.failureCode()).isEqualTo("HANDLER_NOT_FOUND");
    }

    private JobExecution create(UUID userId, JobType type, String keySuffix) {
        UUID targetId = UUID.randomUUID();
        return executionService.create(
                userId, type, targetId, "input-v1", type + ":" + keySuffix);
    }

    private boolean executeAfterSignal(
            JobExecution execution, CountDownLatch ready, CountDownLatch start)
            throws InterruptedException {
        ready.countDown();
        start.await();
        return worker.execute(execution);
    }

    private int countByStatus(JobExecutionStatus status) {
        return jdbcClient
                .sql("SELECT COUNT(*) FROM job_execution WHERE status = :status")
                .param("status", status.name())
                .query(Integer.class)
                .single();
    }

    static class RecordingSuccessHandler implements JobHandler {

        private final AtomicInteger executionCount = new AtomicInteger();

        @Override
        public JobType type() {
            return JobType.CAREER_DOCUMENT_EXTRACTION;
        }

        @Override
        public void handle(JobExecution execution) {
            executionCount.incrementAndGet();
        }

        int executionCount() {
            return executionCount.get();
        }

        void reset() {
            executionCount.set(0);
        }
    }

    static class RecordingFailureHandler implements JobHandler {

        @Override
        public JobType type() {
            return JobType.CAREER_CANDIDATE_EXTRACTION;
        }

        @Override
        public void handle(JobExecution execution) {
            throw new JobHandlerException(
                    "CANDIDATE_STRUCTURE_INVALID", "후보 구조 검증 실패");
        }
    }

    @TestConfiguration
    static class TestHandlerConfiguration {

        @Bean
        RecordingSuccessHandler recordingSuccessHandler() {
            return new RecordingSuccessHandler();
        }

        @Bean
        RecordingFailureHandler recordingFailureHandler() {
            return new RecordingFailureHandler();
        }

        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }
}
