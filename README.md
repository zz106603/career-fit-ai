# career-fit-ai

AI 취업 지원 실행 플랫폼의 Java 21·Spring Boot 기반 모듈형 모놀리스입니다.

[![Backend CI](https://github.com/zz106603/career-fit-ai/actions/workflows/backend-ci.yml/badge.svg)](https://github.com/zz106603/career-fit-ai/actions/workflows/backend-ci.yml)

## 요구 환경

- Java 21
- Gradle Wrapper 사용
- Docker Desktop 또는 Docker Engine과 Docker Compose

## 개발 DB

PostgreSQL 17과 pgvector를 기동합니다.

먼저 예시 파일을 복사하고 로컬 전용 비밀번호를 설정합니다. `.env`는 Git에서
제외되며 Docker Compose가 자동으로 읽습니다.

```powershell
Copy-Item .env.example .env
```

```powershell
docker compose up -d --wait postgres
```

기본 접속 정보는 다음과 같으며 로컬 개발 용도로만 사용합니다.

- URL: `jdbc:postgresql://localhost:5432/career_fit`
- 사용자명: `career_fit`
- 비밀번호: `.env`의 `DB_PASSWORD`

포트나 접속 정보를 바꾸려면 Compose에는 `POSTGRES_DB`, `POSTGRES_USER`,
`DB_PASSWORD`, `POSTGRES_PORT`를, 애플리케이션에는 `DB_URL`,
`DB_USERNAME`, `DB_PASSWORD`를 설정합니다.

DB 컨테이너를 중지할 때는 데이터를 보존합니다.

```powershell
docker compose down
```

`local` 프로필로 애플리케이션을 실행하면 Flyway가 migration을 적용합니다.
관리되지 않은 기존 테이블이 있는 DB는 버전 `0`으로 baseline한 뒤 초기 migration을
적용하며, 이후 모든 스키마 변경은 `backend/src/main/resources/db/migration`에서
버전 migration으로 관리합니다.

## 실행

Windows:

```powershell
Set-Location backend
$env:DB_PASSWORD = '<.env에 설정한 값>'
.\gradlew.bat bootRun --args='--spring.profiles.active=local'
```

macOS/Linux:

```bash
cd backend
export DB_PASSWORD='<.env에 설정한 값>'
./gradlew bootRun --args='--spring.profiles.active=local'
```

`local` 프로필에서 `DB_PASSWORD`가 누락되면 애플리케이션은 기동에 실패합니다.
실제 비밀번호·토큰·API 키는 설정 파일이나 명령행 인자로 커밋하지 않습니다.

## 테스트

통합 테스트가 pgvector 이미지를 Testcontainers로 실행하므로 Docker가 실행 중이어야
합니다. 빈 DB 적용, 기존 DB baseline, migration 재실행, `vector` 타입 사용을 검증합니다.

```powershell
Set-Location backend
.\gradlew.bat clean test
```

PR CI와 동일한 전체 build는 다음 명령으로 재현합니다.

```powershell
Set-Location backend
.\gradlew.bat clean build --no-daemon --stacktrace
```

`main` 대상 Pull Request에는 Java 21 기반 `Backend CI`가 실행되며 단위 테스트,
모듈 의존성 검사, PostgreSQL·pgvector 통합 테스트를 함께 검증합니다. 자세한 내용은
[`CONTRIBUTING.md`](CONTRIBUTING.md)를 참고합니다.

## 모듈 경계

단일 Spring Boot 배포 단위 안에서 다음 최상위 패키지를 업무·기술 모듈 경계로 사용합니다.

- `identity`: 사용자 식별과 소유권 경계
- `career`: 경력 원본, 확정, 버전 및 검색 표현
- `job`: 채용공고 원문과 구조화 요구사항
- `company`: 회사 기준정보와 공식 출처 조사
- `analysis`: 검색, 판정, 근거 및 Snapshot 조정
- `application`: 지원 상태와 분석 결과 조회
- `ai`: Provider 중립적인 AI·검색 기술 지원
- `common`: 업무 규칙이 없는 기술 공통 요소

ArchUnit 테스트가 다음 원칙을 검증합니다.

- `identity`는 다른 업무 모듈이나 `ai`에 의존하지 않습니다.
- `career`, `job`, `company`는 조정 계층인 `analysis`, `application`에 의존하지 않습니다.
- `ai`와 `common`은 업무 모듈을 소유하거나 의존하지 않습니다.

## 실행 프로필

- `local`: 로컬 개발
- `test`: 자동화 테스트

프로필별 설정은 `application-local.yml`, `application-test.yml`로 분리합니다.
운영 환경은 배포 시 외부 설정으로 주입하며 운영 Secret 관리 제품은 현재 범위에
포함하지 않습니다.

## 개발용 사용자 컨텍스트

M0에서는 회원가입·로그인 구현 전에도 사용자별 데이터 격리를 검증할 수 있도록
`local`, `test` 프로필에서 고정 개발 사용자를 제공합니다. 보호 API 요청에는
다음 헤더 중 하나를 전달합니다.

```text
X-Development-User: user-a
X-Development-User: user-b
```

헤더가 없거나 등록되지 않은 값이면 요청은 `401 Unauthorized`로 거절됩니다.
업무 서비스는 이 헤더를 직접 사용하지 않고 `CurrentUserProvider`에서 확인된
`userId`를 받아야 합니다. 이 헤더는 개발·테스트 전용이며 실제 인증 기능으로
사용하지 않습니다.

## 민감 로그 정책

- 비밀번호, 토큰, Authorization, API 키와 secret 값은 콘솔 출력 전에 `***`로 마스킹합니다.
- 문서·공고 원문, LLM prompt, 전체 요청·응답은 로그 인자로 전달하지 않습니다.
- 구조화 로그에는 실행 ID, 데이터 크기, 모델 버전, 상태, 실패 유형처럼 업무 원문이 없는 메타데이터만 기록합니다.
- 예외 메시지에도 같은 마스킹을 적용하지만, 임의의 평문에서 개인정보를 완벽히 식별할 수는 없으므로 원문 로깅 금지가 우선 규칙입니다.
