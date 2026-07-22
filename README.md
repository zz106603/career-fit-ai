# career-fit-ai

AI 취업 지원 실행 플랫폼의 Java 21·Spring Boot 기반 모듈형 모놀리스입니다.

## 요구 환경

- Java 21
- Gradle Wrapper 사용

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
