# Career Fit AI Frontend

M1 Backend API를 브라우저 사용자 흐름으로 연결하는 React Web Client입니다.

## 요구 환경

- Node.js 24
- pnpm 10
- 로컬 Spring Boot API: 기본 `http://localhost:8080`

## 실행

```powershell
Set-Location frontend
pnpm install
pnpm dev
```

브라우저에서 `http://localhost:5173`으로 접속합니다. 개발 서버는 `/api` 요청을
Spring Boot로 proxy하므로 브라우저 코드에서 Backend 주소를 직접 조합하지 않습니다.
대상 주소를 변경할 때만 `.env.local`에 다음 값을 설정합니다.

```text
VITE_API_PROXY_TARGET=http://localhost:8080
```

`.env.local`은 Git에 커밋하지 않습니다.

## 검증 명령

```powershell
pnpm lint
pnpm format:check
pnpm typecheck
pnpm test:run
pnpm build
```

Playwright E2E 시나리오는 M1 화면 연결이 끝나는 FE-008에서 추가합니다.

## 디렉터리 원칙

- `src/app`: 애플리케이션 Provider와 Router 구성
- `src/routes`: URL 단위 화면
- `src/features`: 업무 기능별 UI·상태·API 조합
- `src/shared`: 여러 기능에서 재사용하는 작은 UI·기술 요소
- `src/test`: 단위·컴포넌트 테스트 공통 설정과 MSW 서버
- `e2e`: 실제 Frontend·Backend를 연결하는 Playwright 시나리오
