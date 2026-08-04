# 비동기 Dispatcher·Worker 운영 가이드 v0.1

## 실행 구조

Dispatcher는 DB에서 `QUEUED` 작업을 생성 시각과 실행 ID 순으로 작은 batch만 조회한다.
Worker는 현재 상태가 `QUEUED`인 경우에만 조건부 UPDATE하여 `PROCESSING`으로 선점한다.
영향받은 행이 1개인 Worker만 작업 유형에 맞는 Handler를 실행한다.

Handler 호출은 선점 트랜잭션이 끝난 뒤 수행한다. 성공·실패 상태 저장도 각각 짧은
트랜잭션으로 처리하므로 외부 호출 중 DB 트랜잭션을 유지하지 않는다.

## 설정

```yaml
career-fit:
  async:
    dispatcher:
      enabled: ${ASYNC_DISPATCHER_ENABLED:false}
      fixed-delay: ${ASYNC_DISPATCHER_FIXED_DELAY:5s}
      batch-size: ${ASYNC_DISPATCHER_BATCH_SIZE:10}
```

- `enabled`: 주기 polling 활성화 여부. 필요한 Handler가 준비된 환경에서만 활성화한다.
- `fixed-delay`: 이전 polling 종료 후 다음 polling까지의 간격
- `batch-size`: 한 번에 조회할 최대 QUEUED 작업 수. 1 이상이어야 한다.

로컬 실제 호출 smoke test에서는 `ASYNC_DISPATCHER_ENABLED=true`로 설정한다. 기본값은
`false`이므로 자동 테스트와 일반 로컬 실행에서 외부 작업이 임의로 실행되지 않는다.

현재 MVP Worker는 batch를 순서대로 동기 실행한다. 유형별 동시 실행 상한이 실제로
필요해질 때 제한된 Executor 도입을 검토한다.

## Handler 계약

- Handler는 하나의 `JobType`만 담당한다.
- 같은 유형의 Handler를 둘 이상 등록하면 애플리케이션 시작을 거절한다.
- 등록되지 않은 유형은 `HANDLER_NOT_FOUND`로 실패한다.
- 예상 가능한 업무 실패는 `JobHandlerException`과 안정적인 실패 코드를 사용한다.
- 그 밖의 Handler 예외는 `UNEXPECTED_HANDLER_ERROR`로 저장한다.

## 현재 제외 범위

- 장시간 `PROCESSING` 작업 탐지와 복구
- 재시도 횟수와 재실행 상한
- 다중 인스턴스 `SELECT FOR UPDATE SKIP LOCKED`
- 메시지 브로커와 단계별 산출물 재사용

정체 작업 복구와 재실행 정책은
[`비동기 정체 작업 복구 운영 가이드`](비동기_정체_작업_복구_운영_가이드_v0.1.md)를
따른다.
