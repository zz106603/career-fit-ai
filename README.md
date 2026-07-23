# career-fit-ai

AI 취업 지원 실행 플랫폼의 Java 21·Spring Boot 기반 모듈형 모놀리스입니다.

## 요구 환경

- Java 21
- Gradle Wrapper 사용
- Docker Desktop 또는 Docker Engine과 Docker Compose

## 개발 DB

PostgreSQL 17과 pgvector를 기동합니다.

```powershell
docker compose up -d --wait postgres
```

기본 접속 정보는 다음과 같으며 로컬 개발 용도로만 사용합니다.

- URL: `jdbc:postgresql://localhost:5432/career_fit`
- 사용자명: `career_fit`
- 비밀번호: `career_fit`

포트나 접속 정보를 바꾸려면 Compose에는 `POSTGRES_DB`, `POSTGRES_USER`,
`POSTGRES_PASSWORD`, `POSTGRES_PORT`를, 애플리케이션에는 `DB_URL`,
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
.\gradlew.bat bootRun --args='--spring.profiles.active=local'
```

macOS/Linux:

```bash
cd backend
./gradlew bootRun --args='--spring.profiles.active=local'
```

## 테스트

통합 테스트가 pgvector 이미지를 Testcontainers로 실행하므로 Docker가 실행 중이어야
합니다. 빈 DB 적용, 기존 DB baseline, migration 재실행, `vector` 타입 사용을 검증합니다.

```powershell
Set-Location backend
.\gradlew.bat clean test
```

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
