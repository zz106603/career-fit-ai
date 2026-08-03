# AI Structured Output 실행 규칙 v0.1

## 책임

공통 `ai` 실행기는 Provider 호출, JSON 파싱, 호출 업무가 제공한 decoder 검증,
제한 재시도와 호출 메타데이터 기록을 담당한다. 업무별 DTO·Prompt·의미 검증과
업무 데이터 저장은 호출 업무 모듈이 담당한다.

검증되지 않은 응답은 업무 데이터로 반환하지 않는다. 누락 필드를 기본값으로
보완하거나 일부 파싱 결과를 성공으로 저장하지 않는다.

## 재시도

기본값은 최초 호출을 포함한 최대 3회다. 200ms에서 시작해 2배로 증가하며 최대
2초로 제한한다. timeout, rate limit, Provider 일시 장애, JSON 파싱 실패와 schema
검증 실패만 재시도한다. 정책 거절, API 키 등 Provider 설정 오류와 애플리케이션
내부 오류는 즉시 실패한다.

AI 호출 시도 횟수는 `JobExecution.retryCount`와 별도로 기록한다. 전자는 한 업무
단계 안의 Provider 재호출이고, 후자는 중단되거나 정체된 업무 단계의 재실행이다.

## 트랜잭션

관측 시작과 결과 저장만 각각 짧은 트랜잭션으로 처리한다. Provider 호출, 응답 대기,
backoff 중에는 DB 트랜잭션과 락을 유지하지 않는다. 업무 결과 저장은 호출 업무가
검증 성공 후 별도 트랜잭션으로 수행한다.

## 관측과 민감 정보

`workflowExecutionId` 아래 논리 `aiCallExecutionId`를 만들고 각 호출을 1부터 시작하는
시도 번호로 연결한다. Provider·모델·지연시간·토큰·failureCode·Prompt/Response의
길이와 SHA-256 checksum을 저장한다.

Prompt와 Response 원문, 문서·공고 전체 텍스트, API 키와 인증 토큰은 관측 DB,
일반 로그와 예외 메시지에 저장하지 않는다. Provider가 토큰이나 request ID를
제공하지 않으면 해당 메타데이터는 `unknown`으로 두며 호출 성공 여부를 바꾸지 않는다.
