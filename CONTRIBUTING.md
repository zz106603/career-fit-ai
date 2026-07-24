# 개발 기여 가이드

## Pull Request 검증

`main` 대상 Pull Request에는 `Backend CI`가 실행됩니다. CI는 Ubuntu 환경에서
Temurin Java 21과 저장소의 Gradle Wrapper를 사용해 다음 명령을 수행합니다.

```bash
cd backend
./gradlew clean build --no-daemon --stacktrace
```

이 명령은 단위 테스트, ArchUnit 모듈 의존성 테스트, Testcontainers 기반
PostgreSQL·pgvector 통합 테스트와 Flyway migration 검증을 모두 포함합니다.
GitHub Actions의 Ubuntu runner가 제공하는 Docker를 사용하므로 별도 DB Secret이나
고정 포트 설정은 필요하지 않습니다.

PR을 올리기 전에 로컬 Docker가 실행 중인 상태에서 같은 명령을 통과시켜야 합니다.
Windows에서는 다음 명령을 사용합니다.

```powershell
Set-Location backend
.\gradlew.bat clean build --no-daemon --stacktrace
```

## 모듈 경계

`ModuleDependencyTest`는 다음 금지 의존을 검사합니다.

- `identity` → 업무 모듈 또는 `ai`
- `career`, `job`, `company` → `analysis` 또는 `application`
- `ai` → 업무 모듈
- `common` → 업무 모듈 또는 `ai`

`ModuleDependencyViolationFixtureTest`는 의도적으로 `identity`에서 `ai` 방향의
금지 의존을 만든 fixture를 검사하여 ArchUnit 규칙이 실제 위반을 거부하는지 확인합니다.
fixture는 테스트 전용 패키지에만 두며 제품 코드에 포함하지 않습니다.

## CI 범위

현재 CI는 백엔드 build와 테스트만 담당합니다. 배포, 운영 환경 반영,
프론트엔드 build는 포함하지 않습니다.
