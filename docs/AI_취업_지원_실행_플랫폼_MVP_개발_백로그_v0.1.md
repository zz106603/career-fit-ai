# AI 취업 지원 실행 플랫폼 MVP 개발 백로그 v0.1

- 문서 상태: 개발 착수 기준 초안
- 작성일: 2026-07-22
- 기준 문서: 프로젝트 기획서 v0.2, 핵심 사용자 여정 및 유스케이스 v0.2, 기능·비기능 요구사항 명세서 v0.2, 도메인·데이터 설계 초안 v0.2, 초기 시스템 아키텍처 초안 v0.2
- 목적: 확정된 MVP 범위를 개인 개발자가 실행할 수 있는 개발 순서와 GitHub Issue 단위로 변환한다.

## 0. 백로그 운영 원칙

1. 첫 Vertical Slice에서 `확정 경력 → 색인 → 공고 요구사항 → 검색 → 판정 근거 → 자동 저장`을 먼저 검증한다.
2. AI 후보와 사용자 확정 경력, 공고 원문과 구조화 결과를 저장·상태·UI에서 구분한다.
3. 검색 결과는 판정이 아니라 근거 후보이며, 근거가 없는 `SATISFIED`와 `NOT_SATISFIED`는 저장하지 못한다.
4. 회사 조사는 분석의 기본 단계지만 실패를 격리하여 경력 매칭을 계속한다.
5. DB 작업 레코드가 비동기 실행 내구성의 기준이며, 외부 호출 중 DB 트랜잭션을 유지하지 않는다.
6. 각 Issue는 원칙적으로 하나의 PR로 완료한다. S는 0.5~1일, M은 1~3일, L은 3~5일 범위이며 L을 넘으면 구현 전에 분할한다.
7. Must도 사용자 가치 검증 순서에 따라 배치한다. Should·Could는 핵심 폐쇄 루프 이후에만 착수한다.

## 1. 개발 목표와 완료 정의

MVP 완료는 단순 기능 구현이 아니라, 서로 다른 사용자의 데이터가 격리된 상태에서 다음 폐쇄 루프가 실제 자료로 반복 실행되는 것을 뜻한다.

- 사용자가 PDF 경력 문서를 등록하고, 실패 시 텍스트를 대체 입력할 수 있다.
- PDF 텍스트와 AI 경력 후보가 원문 근거와 함께 저장되며 사용자가 수정·삭제·병합·분리 후 명시적으로 확정할 수 있다.
- AI 후보와 확정 경력이 구분되고 확정된 `CareerExperienceVersion`만 pgvector 검색 대상으로 색인된다.
- 사용자가 채용공고 본문을 등록하고 원문을 보존한 채 요구사항을 구조화·확인·수정하여 `READY`로 만들 수 있다.
- READY 공고에 대해 요구사항별 사용자 경험 후보, 네 상태 판정, 판정 이유, 문서명·페이지·발췌 근거를 확인할 수 있다.
- 회사 공식 정보가 URL과 발행일 또는 확인일과 함께 제공되거나, 식별·검색 실패의 제한사항이 표시된다.
- 합격 확률·임의 점수 없이 지원 판단 근거, 주의·확인 항목, 준비 전략이 생성된다.
- 완료 또는 부분 완료 결과가 별도 저장 버튼 없이 보존되고 과거 실행도 조회된다.
- 사용자가 공고의 현재 지원 상태를 생성·변경하고 목록에서 확인할 수 있다.
- 다른 사용자의 문서·경력·공고·분석·작업·Vector 검색 결과에 접근할 수 없다.
- 애플리케이션 재시작 뒤에도 QUEUED 작업과 장시간 PROCESSING 작업을 다시 발견하고 제한적으로 재실행할 수 있다.
- 첨부된 3개 경력 자료와 2개 공고를 이용한 내부 E2E 및 최소 3명의 실사용 테스트 기준을 통과한다.

## 2. 개발 단계

### Phase 0: 프로젝트 기반 구성

- **목표:** 실행·테스트·마이그레이션 가능한 모듈형 모놀리스 기반을 만든다.
- **사용자 가치:** 아직 직접 기능은 없지만 이후 기능을 안전하게 수직 연결할 기반이 생긴다.
- **선행 조건:** 저장소와 Java 21 개발 환경.
- **포함 기능:** Spring Boot/Gradle, PostgreSQL+pgvector, Migration, 모듈 경계, 환경변수, CI, Fake Provider, 최소 인증 컨텍스트, DB 작업 골격.
- **제외 기능:** 실제 외부 Provider, 운영 배포, 메시지 브로커.
- **완료 조건:** 로컬·CI에서 애플리케이션과 DB 통합 테스트가 실행되고 금지 모듈 의존을 탐지한다.
- **검증 방법:** clean build, migration, context test, architecture test, 비밀정보·로그 점검.
- **관련 요구사항:** NFR-MAINT-001~003, NFR-DATA-001, NFR-SEC-002~003, NFR-PERF-002.

### Phase 1: 최소 Vertical Slice

- **목표:** PDF와 실제 AI 없이 핵심 가치 흐름을 끝까지 관통한다.
- **사용자 가치:** 직접 입력한 확정 경력과 공고 요구사항을 비교한 근거 결과를 저장·조회한다.
- **선행 조건:** Phase 0.
- **포함 기능:** 테스트 사용자, 직접 입력 경력 확정, Fake embedding 색인, 공고 본문·요구사항 1개, 검색, 결정론적 판정, 결과 저장.
- **제외 기능:** PDF, 후보 검토, 회사 조사, 복잡한 인증 UI, 실제 LLM.
- **완료 조건:** 한 사용자에게서 확정 경력 1건이 검색되어 요구사항 판정·근거와 함께 자동 저장된다.
- **검증 방법:** Vertical Slice 통합 테스트와 수동 시연.
- **관련 요구사항:** FR-CAREER-010, FR-JOB-001~002, FR-ANALYSIS-001~005,010,012, FR-APPLICATION-001, NFR-AI-005, NFR-DATA-001~004.

### Phase 2: 사용자와 경력 문서·추출·확정

- **목표:** 실제 PDF에서 사용자가 신뢰할 수 있는 확정 경력을 만든다.
- **사용자 가치:** 분산된 경력 자료를 근거가 보존된 기준정보로 전환한다.
- **선행 조건:** Phase 1.
- **포함 기능:** 가입·로그인, PDF 검증·저장·추출, 실패 대체 입력, 후보 추출·근거, 검토·확정·재실행.
- **제외 기능:** OCR, DOCX, 유사 경험 자동 병합(Should), 좌표 하이라이트.
- **완료 조건:** 10MB·50페이지 이내 정상 PDF의 후보를 수정·확정하며 실패 문서는 대체 입력으로 복구한다.
- **검증 방법:** 실제 PDF, 손상·암호화 PDF, Structured Output 오류, 소유권 통합 테스트.
- **관련 요구사항:** FR-AUTH-001~004, FR-CAREER-001~010,012, NFR-SEC-001~003, NFR-AI-001~003,005.

### Phase 3: 경력 Vector 색인 완성

- **목표:** 확정 경력 버전과 검색 문서 생명주기를 내구성 있게 연결한다.
- **사용자 가치:** 새 공고마다 확정 경력을 반복 입력하지 않고 재사용한다.
- **선행 조건:** Phase 2 및 DB Worker.
- **포함 기능:** PENDING/INDEXING/INDEXED/FAILED, 실제 Embedding Adapter, 사용자 필터, 재실행, 버전 교체.
- **제외 기능:** 별도 Vector DB, 복잡한 fallback 검색, 자동 병합.
- **완료 조건:** 새 버전 색인 성공 뒤 이전 문서가 비활성화되고 실패 작업은 재실행 가능하다.
- **검증 방법:** 색인 멱등성·실패·사용자 격리·버전 전환 통합 테스트.
- **관련 요구사항:** FR-ANALYSIS-001~002, NFR-AVAIL-001,003~004, NFR-AI-004~005, NFR-DATA-001~002.

### Phase 4: 채용공고 등록·구조화

- **목표:** 공고 원문을 보존하면서 분석 가능한 READY 구조를 만든다.
- **사용자 가치:** 서로 다른 형식의 공고를 요구사항 단위로 확인·수정한다.
- **선행 조건:** Phase 2, 공통 AI 구조 검증.
- **포함 기능:** 본문 등록, 별도 비동기 구조화 Workflow, 이상 징후, 사용자 확인·수정, 재실행.
- **제외 기능:** URL 자동 수집, 모든 사이트 지원, 중복 감지(Should).
- **완료 조건:** 두 샘플 공고가 구조화되고 필요한 확인 후 READY가 된다.
- **검증 방법:** 단일·복수 직무, 모호한 회사, 분류 오류, 원문 불변 테스트.
- **관련 요구사항:** FR-JOB-001~008, NFR-PERF-003, NFR-AI-001~002, NFR-DATA-002~004.

### Phase 5: 공고 분석 핵심 Workflow

- **목표:** READY 공고와 색인된 확정 경력으로 근거 제약 판정을 자동 저장한다.
- **사용자 가치:** 요구사항별 관련 경험과 충족·부분 충족·확인 불가·미충족 이유를 확인한다.
- **선행 조건:** Phase 3~4.
- **포함 기능:** 입력 검증, 검색 후보 기록, 판정, 근거, 부분 완료, 재실행, 과거 결과.
- **제외 기능:** 회사 공식 정보와 최종 회사 지원 근거는 Phase 6, 사용자 판정 직접 수정.
- **완료 조건:** 근거 없는 양극 판정을 차단하고 실패 시 최소 판정·근거를 부분 완료로 보존한다.
- **검증 방법:** 골든 케이스, 검색 0건, 명시적 충돌, 저장 실패, 외부 호출 트랜잭션 테스트.
- **관련 요구사항:** FR-ANALYSIS-001~007,009~012, FR-FEEDBACK-002~003, NFR-MAINT-002, NFR-AI-001~005.

### Phase 6: 회사 정보 조사와 최종 전략

- **목표:** 공식 출처를 분석에 결합하되 실패를 격리한다.
- **사용자 가치:** 회사 선택 근거와 현실적인 준비 행동을 출처·자기 경험에 연결해 확인한다.
- **선행 조건:** Phase 5.
- **포함 기능:** 회사 식별, 공식 검색, 최대 5건 채택, 출처 Snapshot, 회사 지원 근거, 제한사항.
- **제외 기능:** 제3자 자료 대체, 회사 조사 단독 재실행(Should), 자유형 Agent.
- **완료 조건:** 성공 시 공식 출처가 연결되고 실패 시 COMPLETED+제한사항으로 경력 판정이 유지된다.
- **검증 방법:** 식별 성공·동명·검색 실패·자료 없음·5건 상한 테스트.
- **관련 요구사항:** FR-COMPANY-001~006, FR-ANALYSIS-006~010,012, NFR-AVAIL-002, NFR-DATA-004.

### Phase 7: 지원 관리·안정화·MVP 검증

- **목표:** 반복 사용 가능한 제품 흐름과 릴리스 품질을 완성한다.
- **사용자 가치:** 공고·분석 이력과 현재 지원 상태를 한곳에서 관리한다.
- **선행 조건:** Phase 6.
- **포함 기능:** 상태·목록·이력, 관측성, 삭제, E2E, 골든 샘플과 3명 테스트.
- **제외 기능:** 상태 이력·전형 단계, Gmail·Calendar, 자동 제출.
- **완료 조건:** 1절 완료 정의와 10절 릴리스 기준을 충족한다.
- **검증 방법:** 보안·복구·E2E·실사용 평가 기록.
- **관련 요구사항:** FR-APPLICATION-001~005, NFR-OBS-001~003, NFR-SEC-001~003, NFR-DATA-001~004.

## 3. Epic 구성

| Epic ID | Epic명 | 목적·사용자 가치 | 포함 범위 | 제외 범위 | 관련 요구사항 | 선행 Epic | 완료 조건 |
|---|---|---|---|---|---|---|---|
| EPIC-FOUNDATION | 프로젝트 기반 | 반복 가능한 개발·배포 기반 확보 | 빌드, DB, Migration, 모듈, CI, 설정 | 운영 인프라 | NFR-MAINT-001~003 | 없음 | clean build와 DB 통합 테스트 성공 |
| EPIC-IDENTITY | 인증·소유권 | 본인 데이터만 안전하게 사용 | 가입·로그인·로그아웃, 사용자 컨텍스트 | 소셜 로그인, 계정 복구 | FR-AUTH-001~004, NFR-DATA-001 | FOUNDATION | 인증과 타 사용자 접근 차단 |
| EPIC-CAREER-DOCUMENT | 경력 문서 | 원본과 추출 텍스트를 안전하게 관리 | PDF·대체 텍스트·상태·재실행 | OCR·DOCX | FR-CAREER-001~005,012 | FOUNDATION, IDENTITY | 정상·실패 문서 처리 가능 |
| EPIC-CAREER-EXTRACTION | 경력 추출·확정 | AI 후보를 사용자가 신뢰 가능한 경력으로 확정 | 후보·근거·수정·확정·직접 입력 | 자동 사실 보완·유사 자동 병합 | FR-CAREER-006~010 | CAREER-DOCUMENT | 확정 경력과 provenance 조회 가능 |
| EPIC-CAREER-INDEX | 경력 검색 색인 | 확정 경력을 공고마다 재사용 | embedding, pgvector, 상태·재색인 | 별도 Vector DB·fallback | FR-ANALYSIS-001~002, NFR-DATA-001 | CAREER-EXTRACTION | 사용자 필터 검색과 버전 전환 성공 |
| EPIC-JOB-POSTING | 공고 등록 | 공고 원문을 사용자 소유 자료로 보존 | 본문·제목·회사 단서·원문 버전 | URL 수집·중복 감지 | FR-JOB-001~002 | IDENTITY | 원문 불변 등록·조회 |
| EPIC-JOB-STRUCTURE | 공고 구조화 | 공고를 READY 분석 입력으로 전환 | 구조화·이상 징후·확인·수정 | 분석 실행 중 사용자 대기 | FR-JOB-003~008 | JOB-POSTING | 구조화 결과가 검증되어 READY |
| EPIC-COMPANY-RESEARCH | 회사 조사 | 공식 출처 기반 회사 근거 제공 | 식별·공식 검색·최대 5건·제한 | 제3자 대체·단독 재실행 | FR-COMPANY-001~006 | JOB-STRUCTURE | 출처 또는 명시적 제한 저장 |
| EPIC-JOB-ANALYSIS | 공고 분석 | 요구사항별 경험과 근거 중심 전략 제공 | 검색·판정·근거·전략·Snapshot·재실행 | 합격 점수·판정 직접 수정 | FR-ANALYSIS-001~012, FR-FEEDBACK-002~003 | CAREER-INDEX, JOB-STRUCTURE | 완료/부분 완료 결과 자동 저장 |
| EPIC-APPLICATION | 지원 관리 | 분석 결과와 현재 지원 상태 반복 관리 | 목록·상세·이력·현재 상태 | 상태 변경 이력·전형 단계 | FR-APPLICATION-001~005 | JOB-ANALYSIS | 본인 공고·결과·상태 조회 |
| EPIC-OBSERVABILITY | 관측·복구 | 실패 작업을 발견하고 원인을 추적 | 실행 ID, 메타데이터, Worker 복구 | 원문 로그·고급 APM | NFR-OBS-001~003, NFR-AVAIL-003~004 | FOUNDATION | 재발견·상관관계 추적 가능 |
| EPIC-SECURITY | 보안·삭제 | 사용자 자료와 비밀정보 보호 | 격리, 로그 제한, 완전 삭제 | 복잡한 비식별 보존 | NFR-SEC-001~003, NFR-DATA-001 | IDENTITY | 격리·삭제 테스트 통과 |
| EPIC-MVP-VALIDATION | MVP 검증 | 실제 가치와 허위 연결 위험 검증 | 골든 샘플, E2E, 3명 테스트 | 대규모 베타·통계 모델 | NFR-AI-003~005 및 전체 Must | 전체 핵심 Epic | 10절 릴리스 기준 충족 |

## 4. GitHub Issue 단위 백로그

아래 `완료/테스트`는 PR 인수 기준이다. 문서 갱신은 최소 대상만 표시한다.

### Phase 0

| ID | 제목 | Epic | 목적·구현 범위 | 제외 범위 | 선행 | 요구사항 | 완료 조건·테스트 | 문서 | 우선/난이도 |
|---|---|---|---|---|---|---|---|---|---|
| BL-001 | [Foundation] Java 21 Spring Boot 모듈형 모놀리스 초기화 | FOUNDATION | Gradle, 기본 모듈 경계와 실행 프로필 구성 | 업무 기능 | 없음 | NFR-MAINT-001~002 | 앱 기동, 모듈 구조 테스트 | README | P0/M |
| BL-002 | [Foundation] PostgreSQL·pgvector 개발 환경과 Migration 구성 | FOUNDATION | Compose DB, 확장 활성화, Migration baseline | 업무 DDL 상세 최적화 | BL-001 | NFR-MAINT-001 | 새 DB·기존 DB migration 및 pgvector 확인 | README, ADR | P0/M |
| BL-003 | [Foundation] 환경변수·비밀정보·민감 로그 정책 적용 | SECURITY | 설정 분리, 커밋 금지 예시, 로그 마스킹 기본 | 운영 Secret 제품 | BL-001 | NFR-SEC-002~003, NFR-MAINT-001 | 비밀 누락 실패, 원문·토큰 비출력 테스트 | README | P0/S |
| BL-004 | [Foundation] CI 빌드·테스트·모듈 의존 검증 구성 | FOUNDATION | clean build, unit/integration, 금지 의존 검사 | 배포 | BL-001~002 | NFR-MAINT-002~003 | PR CI 성공·위반 fixture 실패 | CONTRIBUTING | P0/M |
| BL-005 | [AI] 공통 Provider Port와 Fake Adapter 구성 | FOUNDATION | LLM·Embedding·Search 포트, 결정론 Fake, 오류 유형 | 실제 제품·Prompt | BL-001 | NFR-MAINT-003, NFR-AI-001 | 성공·timeout·invalid 응답 Fake 테스트 | ADR | P0/M |
| BL-006 | [Async] DB 작업 실행 모델과 상태 전이 구현 | OBSERVABILITY | 작업 유형, QUEUED/PROCESSING/SUCCEEDED/FAILED, 중복키·시각 | Worker 실행 | BL-002 | NFR-PERF-002, NFR-AVAIL-003, NFR-DATA-003 | 허용·금지 전이 및 중복 생성 테스트 | 상태 문서 | P0/M |
| BL-007 | [Async] Dispatcher 원자 선점과 Worker Handler 라우팅 | OBSERVABILITY | QUEUED 조회, 단일 인스턴스 원자 선점, 유형별 Handler, 결과 저장 | SKIP LOCKED 다중 인스턴스 | BL-006 | NFR-PERF-002, NFR-AVAIL-003~004 | 동시 선점 1회, 성공·실패 Handler 테스트 | 운영 가이드 | P0/L |
| BL-008 | [Async] 정체 작업 복구와 수동 재실행 구현 | OBSERVABILITY | 장시간 PROCESSING 탐지, QUEUED 복귀/최종 실패, 재실행 상한 | 단계별 부분 재사용 | BL-007 | NFR-AVAIL-003~004, NFR-OBS-001 | 앱 재시작 재발견·stale 복구·무한 재시도 차단 | 운영 가이드 | P0/M |

### Phase 1

| ID | 제목 | Epic | 목적·구현 범위 | 제외 범위 | 선행 | 요구사항 | 완료 조건·테스트 | 문서 | 우선/난이도 |
|---|---|---|---|---|---|---|---|---|---|
| BL-009 | [Identity] 개발용 테스트 사용자와 사용자 컨텍스트 구성 | IDENTITY | 고정 테스트 사용자, 요청별 userId 전파, 보호 경계 | 가입 UI | BL-001 | FR-AUTH-004, NFR-DATA-001 | 미인증·타 사용자 요청 차단 | README | P0/M |
| BL-010 | [Career] 사용자 직접 입력 경력 저장·확정 | CAREER-EXTRACTION | USER_DIRECT 버전, 필수값, confirmedAt, 논리 삭제 | PDF Evidence | BL-009 | FR-CAREER-010 | 미확정 검색 제외, 새 버전·기존 보존 테스트 | 도메인 메모 | P0/M |
| BL-011 | [Career] 확정 경력 검색 문서 생성과 Fake 색인 | CAREER-INDEX | 경력 1건 검색 문서, PENDING→INDEXED, Fake embedding | 실제 API·재색인 | BL-005~007,010 | FR-ANALYSIS-001~002 | 확정 경력만 색인되고 재실행 멱등 | RAG 메모 | P0/M |
| BL-012 | [Job] 채용공고 본문 등록과 원문 보존 | JOB-POSTING | 본문·제목·회사 단서, 소유권, 원문 불변 | URL 수집 | BL-009 | FR-JOB-001~002, NFR-PERF-003 | 빈 입력 거절, 수정 구조와 원문 분리 | README | P0/M |
| BL-013 | [Job] Fake 공고 요구사항 1건 저장·READY 전환 | JOB-STRUCTURE | Fake 구조화 결과, 최소 검증, READY | 실제 LLM·사용자 확인 UI | BL-005,012 | FR-JOB-003~008 | 원문과 구조 분리, READY 전이 검사 | 상태 문서 | P0/M |
| BL-014 | [Analysis] 요구사항별 경력 검색 후보 저장 | JOB-ANALYSIS | userId 필터 Vector 검색, 점수·순위·버전 기록 | 판정 | BL-011,013 | FR-ANALYSIS-001~002 | 타 사용자·미확정 제외, 0건 정상 | RAG 메모 | P0/M |
| BL-015 | [Analysis] 결정론적 판정·근거 저장과 자동 결과 조회 | JOB-ANALYSIS | 4상태, MatchEvidence, Snapshot, 자동 저장·조회 | 회사 조사·생성형 전략 | BL-014 | FR-ANALYSIS-003~005,010,012, FR-APPLICATION-001 | 근거 없는 SATISFIED/NOT_SATISFIED 차단, 결과 재조회 | README | P0/L |

### Phase 2

| ID | 제목 | Epic | 목적·구현 범위 | 제외 범위 | 선행 | 요구사항 | 완료 조건·테스트 | 문서 | 우선/난이도 |
|---|---|---|---|---|---|---|---|---|---|
| BL-016 | [Identity] 회원가입·로그인·로그아웃 구현 | IDENTITY | 최소 인증 방식, 비밀 보호, 인증 종료 | 소셜·복구 | BL-009 | FR-AUTH-001~003 | 정상·중복·오류·로그아웃 접근 테스트 | README | P0/L |
| BL-017 | [Security] Repository·Service 사용자 소유권 통합 검증 | SECURITY | 모든 현재 사용자 소유 모델 접근 규칙·공통 테스트 fixture | Company 전역 기준정보 | BL-016 | FR-AUTH-004, NFR-SEC-001, NFR-DATA-001 | 사용자 A가 B의 ID로 조회·변경·작업·검색 불가 | 보안 체크리스트 | P0/L |
| BL-018 | [Career] 경력 PDF 업로드·검증·원본 저장 | CAREER-DOCUMENT | PDF 10MB/50p, 손상·암호화 거절, FileStoragePort | OCR·DOCX | BL-016 | FR-CAREER-001~002, NFR-PERF-003 | 정상 저장, 잘못된 형식·상한·손상 실패 | README | P0/L |
| BL-019 | [Career] PDF 텍스트 추출 작업과 페이지 근거 저장 | CAREER-DOCUMENT | 비동기 추출, 페이지 텍스트, 분석 4상태 | OCR | BL-007,018 | FR-CAREER-003~004 | 페이지 순서, 추출 실패 상태, 재시작 복구 | 운영 가이드 | P0/L |
| BL-020 | [Career] PDF 추출 실패 대체 텍스트 입력 | CAREER-DOCUMENT | 실패 문서 텍스트 붙여넣기, source 표시, 재실행 연결 | 페이지 번호 생성 | BL-019 | FR-CAREER-005,012 | 빈 입력 차단, 유효 입력 재실행 | README | P0/M |
| BL-021 | [AI] Structured Output 공통 검증·재시도·관측 구현 | FOUNDATION | schema 검증, parse 실패 분류, 제한 재시도, 메타데이터 | 실제 Prompt 내용 | BL-005,008 | NFR-AI-001~002, NFR-OBS-002~003 | invalid 응답 미저장, 상한 뒤 실패, 실행 ID 추적 | AI 실행 규칙 | P0/L |
| BL-022 | [Career] 경력 후보 추출과 원문 Evidence 저장 | CAREER-EXTRACTION | 실제 LLM Adapter, 후보 구조, 문서명·페이지·발췌, 버전 | 사용자 검토 | BL-019~021 | FR-CAREER-006~007, NFR-AI-003~005 | 원문 밖 사실 골든 테스트, 근거 없는 후보 경고 | Prompt 설계서(이 시점 작성) | P0/L |
| BL-023 | [Career] 경력 후보 수정·삭제·병합·분리 | CAREER-EXTRACTION | 후보 상태와 provenance 보존, 검증 | 자동 유사 병합 | BL-022 | FR-CAREER-008 | 수정 후 미확정 유지, N:M provenance 사례 | 사용자 도움말 | P0/L |
| BL-024 | [Career] 후보 확정과 확정 경력 버전 관리 | CAREER-EXTRACTION | 명시적 확정, DOCUMENT Evidence 필수, 새 버전 | 과거 분석 변경 | BL-023 | FR-CAREER-009~010, NFR-DATA-002~004 | 확정 전 검색 불가, 문서 경력 Evidence 필수 | 도메인 메모 | P0/L |
| BL-025 | [Career] 문서 분석 상태 조회와 전체 재실행 | CAREER-DOCUMENT | 단계·실패·다음 행동, 새 실행, 중복 차단 | 세부 단계 재실행 | BL-008,022 | FR-CAREER-003,012 | 백분율 없음, 기존 확정 경력 불변 | 운영 가이드 | P0/M |

### Phase 3

| ID | 제목 | Epic | 목적·구현 범위 | 제외 범위 | 선행 | 요구사항 | 완료 조건·테스트 | 문서 | 우선/난이도 |
|---|---|---|---|---|---|---|---|---|---|
| BL-026 | [Career] 경력 색인 상태와 비동기 Handler 구현 | CAREER-INDEX | PENDING/INDEXING/INDEXED/FAILED, Worker 연결 | 실제 embedding | BL-007,024 | FR-ANALYSIS-001~002, NFR-DATA-003 | 허용 전이·실패 보존·재발견 | 상태 문서 | P0/M |
| BL-027 | [AI] Embedding Provider Adapter와 pgvector 저장 연동 | CAREER-INDEX | 실제 embedding 호출, vector·메타데이터 저장 | Provider 확정 문서 외 제품 종속 확산 | BL-021,026 | NFR-MAINT-003, NFR-AI-004 | timeout·차원 오류·성공 저장, 외부 호출 중 tx 없음 | Prompt/RAG 결정 기록 | P0/L |
| BL-028 | [Career] 사용자 필터 Vector 검색 구현 | CAREER-INDEX | userId, INDEXED, active 최신 버전 필터, top-K 설정 | hybrid/fallback | BL-027 | FR-ANALYSIS-002, NFR-DATA-001 | 사용자 A 검색에 B와 이전 버전 0건 | RAG 결정 기록 | P0/L |
| BL-029 | [Career] 색인 재실행과 버전 안전 전환 | CAREER-INDEX | FAILED 재실행, 새 버전 성공 후 이전 비활성 | 동시 다중 인스턴스 | BL-027~028 | NFR-AVAIL-001,003~004, NFR-DATA-002 | 실패 시 이전 active 유지, 중복 vector 없음 | 운영 가이드 | P0/L |
| BL-030 | [Analysis] 색인 준비 상태 사전 검증과 제한사항 | JOB-ANALYSIS | 분석 전 INDEXED 존재·FAILED 감지, 재색인 안내 | 복잡한 fallback | BL-029 | FR-ANALYSIS-001, NFR-AVAIL-001 | 0건 차단, 일부 실패 제한 표시 정책 테스트 | 사용자 도움말 | P0/M |

### Phase 4

| ID | 제목 | Epic | 목적·구현 범위 | 제외 범위 | 선행 | 요구사항 | 완료 조건·테스트 | 문서 | 우선/난이도 |
|---|---|---|---|---|---|---|---|---|---|
| BL-031 | [Job] 공고 구조화 비동기 Workflow와 상태 구현 | JOB-STRUCTURE | JobPostingAnalysis 생성, QUEUED 작업, 상태·중복 차단 | 분석 Workflow | BL-007,012 | FR-JOB-001, NFR-PERF-002, NFR-AVAIL-003 | 등록 후 작업 생성·재발견·중복 없음 | 상태 문서 | P0/L |
| BL-032 | [Job] 공고 Structured Output 추출·저장 | JOB-STRUCTURE | 회사·직무·업무·필수/우대·연차·기술·근무 조건 일괄 구조화 | 사용자 확인 | BL-021,031 | FR-JOB-003~006 | 원문 명시값만 저장, schema 오류 미확정 | Prompt 설계서(이 시점 작성) | P0/L |
| BL-033 | [Job] 공고 이상 징후와 확인 필요 판정 | JOB-STRUCTURE | 복수 직무·회사 모호·타 직무 혼입·분류 불확실 | 모든 공고 강제 확인 | BL-032 | FR-JOB-007 | 4종 fixture, 경고 없는 공고 자동 진행 가능 | 사용자 도움말 | P0/M |
| BL-034 | [Job] 구조화 결과 확인·수정과 READY 전환 | JOB-STRUCTURE | 수정값·시점, 회사명·핵심 구조 최소 확인, READY | 분석 중 사용자 대기 | BL-033 | FR-JOB-007~008, NFR-DATA-003 | 필수 구조 없으면 READY 불가, 원문 불변 | 상태 문서 | P0/L |
| BL-035 | [Job] 공고 구조화 실패 조회와 전체 재실행 | JOB-STRUCTURE | 실패 유형, 새 실행, 기존 READY 보존, 중복 차단 | 단계 재사용 | BL-008,034 | NFR-AVAIL-003~004, NFR-OBS-001 | 실패 재실행 1개, 과거 실행 식별 | 운영 가이드 | P0/M |

### Phase 5

| ID | 제목 | Epic | 목적·구현 범위 | 제외 범위 | 선행 | 요구사항 | 완료 조건·테스트 | 문서 | 우선/난이도 |
|---|---|---|---|---|---|---|---|---|---|
| BL-036 | [Analysis] READY 공고 분석 실행·사전 검증·작업 생성 | JOB-ANALYSIS | READY·색인 경력·소유권 검증, JobAnalysis QUEUED, 중복 방지 | 구조화 | BL-030,034 | FR-ANALYSIS-001,011, NFR-AVAIL-003 | 비READY·경력 없음 차단, 동일 입력 중복 0 | Workflow 문서 | P0/L |
| BL-037 | [Analysis] 요구사항별 Vector 검색과 후보 Snapshot 저장 | JOB-ANALYSIS | 요구사항별 질의, 후보 ID·점수·순위·버전 저장 | 판정 | BL-028,036 | FR-ANALYSIS-002, NFR-AI-004 | 검색 0건 정상, Snapshot 불변 | RAG 결정 기록 | P0/L |
| BL-038 | [Analysis] 요구사항별 AI 판정과 근거 원자 저장 | JOB-ANALYSIS | 4상태·이유, RequirementMatch+Evidence transaction | 종합 전략 | BL-021,037 | FR-ANALYSIS-003~005, NFR-AI-005 | 근거 없는 양극 판정 차단, rollback 원자성 | Prompt 설계서(이 시점 작성) | P0/L |
| BL-039 | [Analysis] 확인 불가와 미충족 판정 불변 조건 강화 | JOB-ANALYSIS | 미언급=확인 불가, 명시적 반대 사실만 미충족 | 사용자 직접 덮어쓰기 | BL-038 | FR-ANALYSIS-004, FR-FEEDBACK-004 | Swagger 무언급·명시 충돌 골든 테스트 | 판정 규칙 | P0/M |
| BL-040 | [Analysis] 강점·주의·확인 항목과 준비 전략 생성 | JOB-ANALYSIS | 판정 종합, 확률 없는 판단 근거, 현실적 행동 | 회사 연결 근거 | BL-038~039 | FR-ANALYSIS-006~007,009 | 확인 불가를 부족 단정 금지, 허위 경험 제안 차단 | Prompt 설계서(이 시점 작성) | P0/L |
| BL-041 | [Analysis] 결과 검증·Snapshot·자동 저장 | JOB-ANALYSIS | Requirement/채택 경험 Snapshot, Workflow·Prompt·모델 버전, 완료 전 저장 | 전체 경력 복사 | BL-040 | FR-ANALYSIS-012, NFR-AI-004, NFR-DATA-002 | 저장 성공 전 COMPLETED 금지, 변경 후 과거 불변 | Workflow 문서 | P0/L |
| BL-042 | [Analysis] 부분 완료 경계와 실패 상태 처리 | JOB-ANALYSIS | 판정+근거 저장 성공을 최소 경계, 전략 실패 제한 | 요구사항별 부분 재실행 | BL-038,041 | FR-ANALYSIS-010, NFR-AVAIL-001 | 전략 실패 PARTIALLY_COMPLETED, 판정 미저장 FAILED | 운영 가이드 | P0/M |
| BL-043 | [Analysis] 전체 재실행과 이전 결과 보존 | JOB-ANALYSIS | 새 실행·Snapshot, 이전 결과 유지, 중복 제어 | 결과 비교 UI | BL-041~042 | FR-ANALYSIS-011, FR-FEEDBACK-003 | 2회 결과 모두 조회, 기존 불변 | 사용자 도움말 | P0/M |
| BL-044 | [Analysis] 외부 호출 트랜잭션 경계 검증 | JOB-ANALYSIS | tx 조회→종료→외부 호출→짧은 저장 tx 패턴 검증 | 성능 튜닝 | BL-027,032,038,040 | NFR-MAINT-002, NFR-AVAIL-001 | Provider 블로킹 중 DB tx 미유지, 실패 입력 보존 | ADR | P0/M |

### Phase 6

| ID | 제목 | Epic | 목적·구현 범위 | 제외 범위 | 선행 | 요구사항 | 완료 조건·테스트 | 문서 | 우선/난이도 |
|---|---|---|---|---|---|---|---|---|---|
| BL-045 | [Company] 회사 식별과 사용자별 조사 실행 생성 | COMPANY-RESEARCH | 전역 Company, userId 소유 run, 공식 도메인 식별 | 분석 중 사용자 대기 | BL-034,036 | FR-COMPANY-001 | 모호하면 임의 선택 금지, 타 사용자 run 격리 | Workflow 문서 | P0/L |
| BL-046 | [Company] 공식 정보 Search Provider Adapter 연동 | COMPANY-RESEARCH | 공식 소개·채용·최근 직무 자료 후보 수집 | 제3자 자료 | BL-005,021,045 | FR-COMPANY-002~003, NFR-MAINT-003 | 공식 도메인만 후보, timeout 분류, tx 미유지 | 검색 결정 기록·Prompt 필요 시 이때 | P0/L |
| BL-047 | [Company] 공식 출처 최대 5건 채택·메타데이터 저장 | COMPANY-RESEARCH | 관련성·공식성·시점, URL·제목·발행/확인일 | 완전 자동 시점 분류 | BL-046 | FR-COMPANY-004~005, NFR-DATA-004 | 8개 후보→5개 이하, 날짜 없는 출처 거절 | 검색 결정 기록 | P0/M |
| BL-048 | [Company] 조사 실패 격리와 AnalysisLimitation 저장 | COMPANY-RESEARCH | 식별 실패·자료 없음·검색 실패 구분, 분석 계속 | 제3자 fallback | BL-045~047 | FR-COMPANY-006, FR-ANALYSIS-010, NFR-AVAIL-002 | 회사 실패에도 판정 완료, 제한 표시 | 운영 가이드 | P0/L |
| BL-049 | [Analysis] 회사 출처 Snapshot과 회사 지원 근거 결합 | JOB-ANALYSIS | 공식 사실+사용자 경험 연결, 출처 Snapshot, 최종 전략 재검증 | 회사 정보 없을 때 생성 | BL-040,047~048 | FR-ANALYSIS-008~009,012, NFR-AI-005 | 모든 회사 사실에 source, 없으면 빈 결과+제한 | Prompt 설계서(이 시점 보강) | P0/L |

### Phase 7

| ID | 제목 | Epic | 목적·구현 범위 | 제외 범위 | 선행 | 요구사항 | 완료 조건·테스트 | 문서 | 우선/난이도 |
|---|---|---|---|---|---|---|---|---|---|
| BL-050 | [Application] 분석 결과·제한사항·이력 조회 | APPLICATION | 완료/부분 완료 상세, 제한, 과거 실행 | 비교 UI | BL-043,049 | FR-APPLICATION-001~002,005 | 소유권·Snapshot·실패 상태 조회 | 사용자 도움말 | P0/L |
| BL-051 | [Application] 현재 지원 상태 설정·변경과 공고 목록 | APPLICATION | 최초 설정 시 Application 생성, 6상태 현재값, 최근 분석 | 상태 이력·사유·전형 | BL-012,050 | FR-APPLICATION-003~004 | 허용값·동시 충돌·타 사용자·빈 목록 테스트 | 사용자 도움말 | P0/L |
| BL-052 | [Feedback] 누락 경력 보완과 재분석 연결 | JOB-ANALYSIS | 확인 불가→경력 관리→확정·색인→전체 재분석 | 판정 직접 수정·이의 피드백 Should | BL-029,043,050 | FR-FEEDBACK-002~004 | 미확정/미색인 재분석 차단, 과거 보존 | 사용자 도움말 | P0/M |
| BL-053 | [Observability] Workflow 구조화 로그와 AI 사용량 기록 | OBSERVABILITY | request/user/run/step/time/failure/provider/token/result/retry | 원문 요청·응답 | BL-021,049 | NFR-OBS-001~003, NFR-SEC-002~003 | 실행 ID 상관 추적, 민감 원문 부재 | 운영 가이드 | P0/M |
| BL-054 | [Security] 사용자 탈퇴·완전 삭제 Orchestration | SECURITY | 모듈 공개 삭제 호출, 원본·텍스트·후보·경력·공고·Snapshot·Vector 삭제 | 부분 비식별 보존 | BL-017,050~051 | NFR-SEC-001, NFR-DATA-001~002 | 사용자 데이터 0건, 개인 문맥 없는 Company 유지 | 운영 가이드 | P0/L |
| BL-055 | [Validation] 핵심 사용자 흐름 End-to-End 테스트 | MVP-VALIDATION | PDF→후보→확정→색인→공고→분석→저장→상태 | 브라우저 전 화면 시각 QA | BL-018~054 | 전체 핵심 Must | 정상·PDF 실패·회사 실패·재시작 복구 E2E | 테스트 보고서 | P0/L |
| BL-056 | [Validation] 골든 샘플 품질 평가 자동화 | MVP-VALIDATION | 첨부 3개 경력 자료·2개 공고, 허위·누락·근거 평가 | 통계적 벤치마크 | BL-055 | NFR-AI-003~005 | 문서명·페이지·발췌, 없는 경험 0 목표 및 오류 기록 | 평가 보고서 | P0/L |
| BL-057 | [Validation] 3인 실사용 테스트와 MVP 릴리스 판정 | MVP-VALIDATION | 사용자별 문서≥1·공고≥2, 시간·오류·출처·반복입력 측정 | 공개 베타 | BL-056 | 전체 MVP | 10절 기준 기록과 Go/보완 결정 | 릴리스 노트 | P0/L |

### 핵심 흐름 이후 Should 백로그

| ID | 제목 | Epic | Phase | 선행 | 요구사항 | 완료 조건 | 우선/난이도 |
|---|---|---|---|---|---|---|---|
| BL-058 | [Career] 유사 경험 중복 후보 표시 | CAREER-EXTRACTION | 후순위 | BL-024 | FR-CAREER-011 | 자동 병합 없이 양쪽 근거와 후보만 표시 | P1/L |
| BL-059 | [Job] 동일 공고 중복 후보 표시 | JOB-POSTING | 후순위 | BL-034 | FR-JOB-009 | 소유자 범위 제안, 계속 등록 허용 | P1/M |
| BL-060 | [Company] 회사 정보 조사 단독 재실행 | COMPANY-RESEARCH | 후순위 | BL-048 | FR-COMPANY-007 | 새 run 생성, 이전 결과 보존 | P1/M |
| BL-061 | [Feedback] AI 판정 이의 피드백 기록 | JOB-ANALYSIS | 후순위 | BL-050 | FR-FEEDBACK-001 | 판정 불변, 피드백만 연결 | P1/M |
| BL-062 | [Performance] 일반 조회 p95 측정·개선 | OBSERVABILITY | 후순위 | BL-051,053 | NFR-PERF-001 | 정상 부하 p95 1초 이내 측정 보고 | P1/M |

## 5. 핵심 Vertical Slice

### 포함 기능

- BL-009~015: 테스트 사용자, USER_DIRECT 확정 경력 1건, Fake embedding 색인, 공고 본문, Fake 요구사항 1개, 사용자 필터 검색, 결정론 판정·근거, 분석 자동 저장·조회.
- 최소 상태는 경력 확정 여부, 색인 상태, 공고 READY, 분석 실행 상태만 사용한다.

### 임시 처리 또는 Fake Adapter

- 인증은 고정 테스트 사용자 컨텍스트로 처리한다.
- 공고 구조화는 정해진 요구사항 1개를 반환하는 Fake LLM을 사용한다.
- Embedding은 결정론적 테스트 벡터를 사용한다.
- 판정은 테스트 fixture에 대해 명시적 규칙으로 반환하며 생성형 전략은 생략한다.
- 회사 조사 단계는 아직 Workflow에 연결하지 않는다.

### 검증할 핵심 가설

1. 사용자는 자신의 확정 경력을 공고 요구사항 단위로 다시 찾는 결과에서 가치를 느끼는가.
2. 검색 후보와 최종 판정을 분리해도 흐름이 과도하게 복잡하지 않은가.
3. 근거 ID와 표시용 Snapshot만으로 과거 결과를 이해할 수 있는가.
4. 사용자 범위 필터를 저장·검색 전 경로에 일관되게 적용할 수 있는가.

### 완료 조건

- 경력 1건과 공고 요구사항 1건을 입력하면 관련 경력, 상태, 이유, 근거가 반환된다.
- 결과가 자동 저장되고 재조회된다.
- 다른 테스트 사용자에게 해당 경력·공고·결과가 노출되지 않는다.
- 근거 없는 SATISFIED/NOT_SATISFIED 저장이 거절된다.

### 실제 기능으로 교체할 부분

- 테스트 사용자 → BL-016 인증
- 직접 입력만 → BL-018~025 PDF·추출·확정
- Fake embedding → BL-027 실제 Adapter
- Fake 요구사항 → BL-031~034 공고 구조화
- 결정론 판정 → BL-038~040 AI 판정·전략
- 회사 단계 생략 → BL-045~049 공식 정보 조사

첫 Vertical Slice는 **BL-015 완료 시점**에 완성된다.

## 6. 비동기 작업 백로그

| 처리 항목 | 담당 Issue | MVP 구현 기준 | 후속 확장 |
|---|---|---|---|
| 작업 실행 레코드 저장 | BL-006 | 유형·대상·입력 버전·상태·재시도·시각 저장 | 범용 Workflow 엔진은 도입하지 않음 |
| QUEUED 작업 조회 | BL-007 | 단일 인스턴스 polling, batch·정렬 설정 | 우선순위 큐 필요 시 재검토 |
| 원자적 PROCESSING 선점 | BL-007 | 조건부 상태 갱신으로 1회 선점 | 다중 인스턴스에서 `SKIP LOCKED` 검토 |
| Worker 실행 | BL-007 | 제한된 실행 풀 또는 동기 Worker 호출, DB가 기준 | 브로커 소비자로 교체 가능 |
| 성공·실패 저장 | BL-007 | 짧은 트랜잭션으로 상태·failureCode 저장 | 세부 호출 이력 모델은 후순위 |
| 장시간 PROCESSING 복구 | BL-008 | 선점 시각 기준 stale 발견, 상한 내 재큐잉 | heartbeat는 필요 시 도입 |
| 재실행 | BL-008 | 새 실행 또는 retry count 증가 정책을 작업 유형별 명시 | 단계별 부분 재실행은 Should |
| 작업 유형별 Handler | BL-007 및 BL-019,026,031,036 | 문서 추출·후보 추출·색인·공고 구조화·분석 Handler 연결 | 플러그인형 동적 등록 불필요 |

공통 테스트는 앱 종료 직전 QUEUED 생성, 재시작 후 발견, 두 Dispatcher 동시 호출 시 단일 선점, 재시도 상한, stale 복구를 포함한다. 다중 인스턴스는 실제 두 인스턴스 배포 필요가 생기거나 중복 선점이 관측될 때만 검토한다.

## 7. AI 기능 구현 순서

| 순서 | 기능 | Issue | Prompt 상세 설계 시점 |
|---|---|---|---|
| 1 | 공통 LLM Provider Port와 Fake Adapter | BL-005 | 아직 작성하지 않고 호출 계약만 정한다. |
| 2 | Structured Output Schema 검증 | BL-021 | schema·오류 분류를 정하며 Prompt 공통 규칙만 결정한다. |
| 3 | 경력 후보 추출 | BL-022 | 구현 직전에 후보 필드·근거 제약·골든 샘플과 함께 작성한다. |
| 4 | 공고 구조화 | BL-032 | 회사·직무·업무·조건 schema와 이상 징후 fixture 확정 후 작성한다. |
| 5 | 요구사항별 판정 | BL-038~039 | 네 상태의 증거 규칙과 NOT_SATISFIED 명시적 충돌 정의 후 작성한다. |
| 6 | 지원 판단 근거·준비 전략 | BL-040 | 판정 저장이 안정된 뒤, 금지 표현과 근거 연결 규칙을 포함해 작성한다. |
| 7 | Embedding Provider 연동 | BL-027~029 | Prompt는 불필요하며 검색 문서 조합·top-K·모델 버전 정책을 정한다. |
| 8 | 회사 정보 Search Provider 연동 | BL-046~049 | 검색 질의 생성에 LLM이 필요할 때만 검색용 Prompt를 작성한다. |

구체 Provider 제품 선택은 각 실제 Adapter Issue 착수 직전에 한다. 하나의 AI Issue가 실제 모델 연동과 품질 Prompt 조정을 모두 장기간 끌면 Adapter 연결 PR과 품질 조정 PR로 분할한다.

## 8. 테스트 백로그 추적

| 필수 테스트 | 포함 Issue | 핵심 검증 |
|---|---|---|
| 도메인 불변 조건 단위 테스트 | BL-006,010,024,026,034,038~039,051 | 상태 전이·출처·판정·현재 상태 규칙 |
| 사용자 데이터 격리 통합 테스트 | BL-017,028,050~051 | ID 직접 접근, 작업 조회, Vector 검색 포함 |
| PDF 추출 실패 테스트 | BL-019~020 | 실패 상태와 대체 입력 복구 |
| Structured Output 검증 실패 테스트 | BL-021,022,032,038 | 잘못된 결과 업무 저장 금지·제한 재시도 |
| 비동기 작업 재발견 테스트 | BL-008 | 재시작 뒤 QUEUED와 stale PROCESSING 복구 |
| 경력 색인 재실행 테스트 | BL-029 | 멱등성·기존 active 유지·성공 후 전환 |
| 사용자 필터 Vector 검색 테스트 | BL-028,037 | 타 사용자·미확정·비활성 버전 제외 |
| 근거 없는 SATISFIED 저장 차단 | BL-015,038 | Evidence 없는 저장 거절 |
| 근거 없는 NOT_SATISFIED 저장 차단 | BL-015,039 | 명시적 반대 사실 없는 저장 거절 |
| 회사 검색 실패 후 분석 계속 | BL-048~049 | 판정 유지, COMPLETED+Limitation |
| 분석 결과 자동 저장 | BL-041~042 | 저장 전 완료 노출 금지, 부분 완료 경계 |
| 외부 호출 중 DB 트랜잭션 미유지 | BL-044 | LLM·Embedding·Search 지연 중 열린 tx 없음 |
| 핵심 사용자 흐름 E2E | BL-055 | 등록부터 지원 상태까지 정상·복구 경로 |

## 9. 기술 부채와 후순위 목록

| 항목 | MVP 처리 | 재검토 조건 |
|---|---|---|
| 모든 채용 사이트 URL 수집 | 본문 붙여넣기만 Must | 테스트 사용자의 반복 불편 1순위이고 사이트 정책을 준수할 수 있을 때 |
| DOCX와 이미지 OCR | 제외 | 입력 실패의 20% 이상을 차지할 때 |
| PDF 좌표 하이라이트 | 페이지·발췌로 대체 | 근거 위치를 못 찾는 사용자 오류가 반복될 때 |
| 유사 경력 자동 병합 | 후보 표시만 Should(BL-058) | 문서 중복이 검토 시간의 핵심 병목일 때 |
| 정교한 부분 재실행 | 전체 재실행 | 외부 비용·시간이 실사용 장벽이 될 때 |
| 자유형 Multi-Agent | 제외 | 통제 Workflow로 해결 불가한 명확한 과업과 평가 기준이 생길 때 |
| MCP 외부 연동 | 제외 | GitHub·Calendar 등 사용자가 승인한 외부 도구 연동을 실제 구현할 때 |
| 메시지 브로커 | DB Dispatcher/Worker | 지속 backlog, 독립 확장, 전달량·격리 요구가 DB 방식을 넘을 때 |
| 다중 인스턴스 Worker | 단일 인스턴스 원자 갱신 | 두 인스턴스 배포 필요 또는 중복 선점 위험이 생길 때 |
| Kubernetes | Compose·단일 배포 | 운영 규모·가용성 요구가 단일 호스트를 넘을 때 |
| Gmail·Calendar | 제외 | 핵심 분석 사용성이 검증되고 명시적 연동 수요가 확인될 때 |
| 자동 자소서 완성 및 제출 | 근거·준비 전략만 제공 | 사용자 검토·승인·사이트 정책·오류 책임을 설계한 뒤 |
| 합격 가능성 점수 | 영구 금지에 가까운 MVP 비목표 | 검증 가능한 라벨 데이터와 오해 방지 근거가 생겨도 별도 제품 결정 필요 |
| 사용자 판정 직접 수정 | AI 판정은 읽기 전용 | 별도 ‘사용자 자기판정’ 모델 가치가 검증될 때; AI 값 덮어쓰기는 금지 |
| 지원 상태 변경 이력·사유·전형 | 현재 상태만 저장 | 3인 테스트에서 추적 필요가 반복될 때 |
| 회사 조사 단독 재실행 | Should BL-060 | 검색 실패가 잦고 전체 분석 재실행 비용이 클 때 |

## 10. 릴리스 기준

### 내부 테스트 기준

- 첨부된 이력서·경력기술서·포트폴리오에서 핵심 경험 후보를 추출하고 사용자가 확정할 수 있다.
- 첨부된 DB Inc와 한미그룹 채용공고를 각각 구조화하여 READY로 만들 수 있다.
- 원문에 없는 경험을 후보·판정·전략에서 보유 사실로 생성하지 않는다. 발견 시 건수와 원인을 기록하고 릴리스 차단 여부를 판단한다.
- 요구사항별 근거가 문서명·페이지·발췌와 함께 표시된다. USER_DIRECT는 직접 입력 출처로 명확히 구분된다.
- 회사 공식 정보에는 URL과 발행일 또는 확인일이 있고, 실패 시 제3자 정보로 메우지 않는다.
- 재시작·Provider 실패·색인 실패에서도 저장된 입력과 기존 결과가 유실되지 않는다.

### 실사용 테스트 기준

- 최소 3명의 테스트 사용자, 사용자별 경력 문서 1개 이상, 채용공고 2개 이상을 사용한다.
- 공고 1건당 기존 수작업 시간과 플랫폼 사용 시간을 같은 범위로 비교한다.
- 잘못 연결된 경험, 누락된 관련 경험, 잘못된 상태 판정을 요구사항 단위로 기록한다.
- 회사 출처가 실제 공식 도메인이고 링크·날짜가 유효한지 확인한다.
- 두 번째 공고에서 경력 재입력·복사 작업이 첫 번째보다 감소했는지 기록한다.
- 치명적 사용자 격리·허위 경험·근거 무결성 오류가 0건이어야 한다.
- 나머지 품질 문제는 빈도·영향과 함께 다음 백로그로 전환하고 MVP Go/보완 결정을 남긴다.

## 11. 백로그 실행 순서

### MVP 핵심 경로

`병렬`은 선행 작업이 끝난 뒤 다른 표시 Issue와 동시에 진행 가능하다는 뜻이다. 개인 개발 기준 실제로는 한 번에 한 Issue만 진행하는 것을 권장한다.

| 순번 | Issue | 선행 작업 | 병렬 가능 | 핵심 경로 |
|---:|---|---|---|---|
| 1 | BL-001 | 없음 | 아니오 | 예 |
| 2 | BL-002 | BL-001 | BL-003 | 예 |
| 3 | BL-003 | BL-001 | BL-002 | 예 |
| 4 | BL-004 | BL-001~002 | BL-005 | 아니오 |
| 5 | BL-005 | BL-001 | BL-004 | 예 |
| 6 | BL-006 | BL-002 | BL-005 | 예 |
| 7 | BL-007 | BL-006 | 아니오 | 예 |
| 8 | BL-008 | BL-007 | BL-009 | 예 |
| 9 | BL-009 | BL-001 | BL-008 | 예 |
| 10 | BL-010 | BL-009 | 아니오 | 예 |
| 11 | BL-011 | BL-005,007,010 | BL-012 | 예 |
| 12 | BL-012 | BL-009 | BL-011 | 예 |
| 13 | BL-013 | BL-005,012 | 아니오 | 예 |
| 14 | BL-014 | BL-011,013 | 아니오 | 예 |
| 15 | BL-015 | BL-014 | 아니오 | 예 — 첫 Slice 완료 |
| 16 | BL-016 | BL-009 | BL-021 | 예 |
| 17 | BL-017 | BL-016 | BL-018 | 예 |
| 18 | BL-018 | BL-016 | BL-021 | 예 |
| 19 | BL-019 | BL-007,018 | 아니오 | 예 |
| 20 | BL-020 | BL-019 | BL-021 | 예 |
| 21 | BL-021 | BL-005,008 | BL-016~020 | 예 |
| 22 | BL-022 | BL-019~021 | 아니오 | 예 |
| 23 | BL-023 | BL-022 | 아니오 | 예 |
| 24 | BL-024 | BL-023 | 아니오 | 예 |
| 25 | BL-025 | BL-008,022 | BL-026 | 예 |
| 26 | BL-026 | BL-007,024 | BL-025 | 예 |
| 27 | BL-027 | BL-021,026 | 아니오 | 예 |
| 28 | BL-028 | BL-027 | 아니오 | 예 |
| 29 | BL-029 | BL-027~028 | 아니오 | 예 |
| 30 | BL-030 | BL-029 | BL-031 | 예 |
| 31 | BL-031 | BL-007,012 | BL-030 | 예 |
| 32 | BL-032 | BL-021,031 | 아니오 | 예 |
| 33 | BL-033 | BL-032 | 아니오 | 예 |
| 34 | BL-034 | BL-033 | 아니오 | 예 |
| 35 | BL-035 | BL-008,034 | BL-036 | 아니오 |
| 36 | BL-036 | BL-030,034 | 아니오 | 예 |
| 37 | BL-037 | BL-028,036 | 아니오 | 예 |
| 38 | BL-038 | BL-021,037 | 아니오 | 예 |
| 39 | BL-039 | BL-038 | BL-040 준비 | 예 |
| 40 | BL-040 | BL-038~039 | 아니오 | 예 |
| 41 | BL-041 | BL-040 | 아니오 | 예 |
| 42 | BL-042 | BL-038,041 | BL-044 | 예 |
| 43 | BL-043 | BL-041~042 | BL-044 | 예 |
| 44 | BL-044 | BL-027,032,038,040 | BL-042~043 | 예 |
| 45 | BL-045 | BL-034,036 | 아니오 | 예 |
| 46 | BL-046 | BL-005,021,045 | 아니오 | 예 |
| 47 | BL-047 | BL-046 | 아니오 | 예 |
| 48 | BL-048 | BL-045~047 | 아니오 | 예 |
| 49 | BL-049 | BL-040,047~048 | 아니오 | 예 |
| 50 | BL-050 | BL-043,049 | 아니오 | 예 |
| 51 | BL-051 | BL-012,050 | BL-052 | 예 |
| 52 | BL-052 | BL-029,043,050 | BL-051 | 예 |
| 53 | BL-053 | BL-021,049 | BL-054 | 예 |
| 54 | BL-054 | BL-017,050~051 | BL-053 | 예 |
| 55 | BL-055 | BL-018~054 관련 핵심 | 아니오 | 예 |
| 56 | BL-056 | BL-055 | 아니오 | 예 |
| 57 | BL-057 | BL-056 | 아니오 | 예 |

### 후순위 실행 순서

| 순번 | Issue | 선행 작업 | 병렬 가능 | 핵심 경로 |
|---:|---|---|---|---|
| 58 | BL-058 | BL-024 | BL-059~061 | 아니오 |
| 59 | BL-059 | BL-034 | BL-058,060~061 | 아니오 |
| 60 | BL-060 | BL-048 | BL-058~059,061 | 아니오 |
| 61 | BL-061 | BL-050 | BL-058~060 | 아니오 |
| 62 | BL-062 | BL-051,053 | 가능 | 아니오 |

## 12. 개발 시작 체크리스트

- [ ] 저장소를 생성하고 기본 branch 보호 정책을 정한다.
- [ ] Java 21과 사용할 Spring Boot 안정 버전을 결정한다.
- [ ] Gradle Wrapper, formatter, 테스트 task를 설정한다.
- [ ] PostgreSQL+pgvector Docker Compose와 데이터 Volume을 준비한다.
- [ ] 개발·테스트·운영 환경변수, 예시 파일, 비밀정보 커밋 금지 정책을 정한다.
- [ ] `identity/career/job/company/analysis/application/ai/common` 모듈 골격과 의존 검사를 정한다.
- [ ] DB Migration 도구를 선택하고 baseline 실행을 확인한다.
- [ ] 인증 MVP 방식을 결정한다. 첫 Slice는 테스트 사용자, Phase 2부터 실제 최소 인증을 사용한다.
- [ ] 개발 FileStorage Adapter를 로컬 파일+명시적 루트로 결정하고 Docker Volume 경계를 정한다.
- [ ] LLM·Embedding·Search Fake Adapter의 정상·timeout·invalid 결과 fixture를 준비한다.
- [ ] DB Dispatcher polling 주기·batch·Worker 동시성·stale 기준·재시도 상한의 초기값을 정한다.
- [ ] 요청 ID·실행 ID·failureCode와 민감 로그 금지 기준을 정한다.
- [ ] CI에서 빌드·단위·통합·모듈 의존 테스트를 실행한다.
- [ ] GitHub Issue 템플릿, PR 템플릿, Conventional Commit, 1 Issue–1 PR 원칙을 정한다.
- [ ] README에 목표, 범위, 로컬 실행, 테스트, 핵심 아키텍처 결정을 기록한다.

## 13. 별도 설계가 필요한 시점

새 대형 선행 문서는 만들지 않는다. 다음 내용은 해당 Issue 착수 시 짧은 결정 기록 또는 Issue 하위 설계로 확정한다.

- BL-016 전: 인증 MVP 방식과 로그아웃 경계.
- BL-022 전: 경력 후보 Structured Output schema와 Prompt·버전·골든 샘플.
- BL-027 전: Embedding Provider, 검색 문서 조합, vector 차원, 재임베딩 정책.
- BL-032 전: 공고 구조화 schema, READY 최소 기준, 이상 징후 판정 입력.
- BL-038 전: top-K, 검색 질의, 네 상태 증거 규칙, 미충족의 명시적 충돌 정의.
- BL-040 전: 지원 판단·준비 전략 Prompt와 금지 표현 검증 규칙.
- BL-046 전: Search Provider, 공식 도메인 판별, 최근 기준, 최대 5건 선별 규칙.
- BL-054 전: 완전 삭제 순서·실패 복구·감사 범위.
- BL-057 전: 실사용 평가 양식과 Go/보완 임계 기준.

## 14. 기존 문서 충돌 및 해석 기준

1. **요구사항 명세의 PDF 상한 미결정 표현:** FR-CAREER-002 비고에는 상한 미결정이 남아 있으나 도메인·아키텍처 v0.2에서 10MB·50페이지, 암호화·손상 거절이 확정됐다. 본 백로그는 최신 확정 정책을 따른다.
2. **문서 분석 상태 표현:** FR-CAREER-003의 ‘검토 필요·완료’ 표현과 달리 `CareerDocumentAnalysis`는 QUEUED/PROCESSING/SUCCEEDED/FAILED만 사용한다. SUCCEEDED는 후보 저장 성공이며 후보 검토 상태는 `CareerExtractionCandidate`가 소유한다.
3. **분석 시작 시 확정 Snapshot 표현:** 일부 요구사항은 ‘확정 경력 스냅샷’이라고 표현하지만 도메인 v0.2에 따라 전체 경력을 복사하지 않는다. 검색 후보 ID·점수·순위와 최종 채택 경력 표시 Snapshot만 보존한다.
4. **관측성 단계 상태:** NFR-OBS-001은 단계별 상태·시각을 요구하지만 `AnalysisStep` 엔티티는 제외됐다. JobAnalysis의 currentStep·상태·시각과 구조화 로그로 충족한다.
5. **공고 구조화와 분석:** JobPostingAnalysis는 job 모듈의 선행 Workflow이며 JobAnalysis는 READY 결과만 입력받는다. 분석 중 사용자 확인 대기 상태를 만들지 않는다.
6. **회사 확인 요청:** FR-COMPANY-001의 확인 요청은 READY 전 공고 구조화 단계에서만 처리한다. 분석 중 식별이 불확실하면 제한사항을 저장하고 경력 판정을 계속한다.
7. **AI·RAG 세부 정책:** top-K, Prompt, 재시도 수, Provider는 아직 확정되지 않았으므로 본 백로그는 결정 시점만 표시한다. 이는 충돌이 아니라 의도적 지연 결정이다.

새로운 구조적 충돌은 확인되지 않았다.

## 15. 백로그 요약

- **전체 Epic 수:** 13개
- **전체 Issue 수:** 62개
- **MVP 핵심 Issue 수:** 57개(BL-001~057)
- **후순위 Should Issue 수:** 5개(BL-058~062)
- **Phase별 핵심 Issue 수:** Phase 0 8개, Phase 1 7개, Phase 2 10개, Phase 3 5개, Phase 4 5개, Phase 5 9개, Phase 6 5개, Phase 7 8개
- **MVP 핵심 경로 Issue 수:** 실행 순서 표 기준 53개. BL-004, BL-025, BL-035, BL-053은 릴리스 필수이지만 사용자 가치 생성의 직접 선행 경로에서는 분리 가능하다.
- **첫 번째로 구현할 Issue:** BL-001 `[Foundation] Java 21 Spring Boot 모듈형 모놀리스 초기화`
- **첫 Vertical Slice 완료 예상 지점:** BL-015 완료 시점
- **개발 중 별도 설계가 필요한 시점:** BL-016,022,027,032,038,040,046,054,057 착수 직전의 짧은 Issue/ADR 결정
- **범위가 과도해 보이는 항목:** 실제 PDF 세 종류의 안정적 후보 추출, 공고 구조화 품질, 네 상태 판정 증거 규칙, 공식 회사 정보 검색을 모두 Must로 구현하는 부분이다. 기능 삭제보다는 첫 Slice 이후 품질 기준을 단계적으로 높이고, 각 L Issue가 5일을 넘으면 Adapter 연결과 품질 개선을 별도 PR로 분할한다.

이 백로그가 승인되면 추가 선행 문서를 만들지 않고 BL-001부터 Spring Boot 프로젝트 생성과 구현을 시작한다.
