# PDF 텍스트 추출 운영 가이드 v0.1

## 처리 흐름

1. 인증 사용자가 `POST /api/career-documents/{documentId}/extractions`를 호출한다.
2. 서버는 문서 소유권을 확인하고 `CareerDocumentAnalysis`와 `JobExecution`을 `QUEUED`로 생성한다.
3. Worker가 저장소에서 원본 PDF를 읽고 PDFBox로 페이지별 텍스트를 추출한다.
4. 페이지 텍스트를 한 트랜잭션으로 저장하고 `JobExecution`은 `SUCCEEDED`로 종료한다.
5. `CareerDocumentAnalysis`는 후속 분석이 이어질 수 있도록 `PROCESSING`을 유지한다.

동일 사용자·문서·입력 버전에 활성 분석이 있으면 새 작업을 만들지 않고 기존 분석과 작업 ID를 반환한다.

## 저장 및 보안

- 원본 PDF는 로컬 파일 저장소에, 추출 텍스트는 PostgreSQL `career_document_page.page_text`에 저장한다.
- 모든 조회와 저장은 `user_id`와 문서 소유권을 기준으로 격리한다.
- 원본 PDF 내용과 추출 텍스트는 로그에 기록하지 않는다.
- 줄바꿈만 LF로 정규화하고, 공백 제거·문장 보정·OCR은 수행하지 않는다.

## 실패 코드

- `CAREER_DOCUMENT_NOT_FOUND`: 문서가 삭제됐거나 소유 문서가 아님
- `FILE_STORAGE_READ_FAILED`: 저장된 원본을 읽지 못함
- `PDF_PARSE_FAILED`: PDF 구조 또는 페이지 정보 파싱 실패
- `PDF_ENCRYPTED`: 암호화된 PDF
- `PDF_TEXT_EMPTY`: 모든 페이지의 추출 텍스트가 비어 있음
- `PAGE_TEXT_SAVE_FAILED`: 페이지 텍스트 DB 저장 실패

실패하면 `JobExecution`과 `CareerDocumentAnalysis`를 모두 `FAILED`로 기록한다. 원인을 제거한 뒤 다시 요청하면 새 입력 작업을 생성할 수 있다.

## 범위 제외

OCR, 표·레이아웃 복원, 경력 후보 생성, 임베딩과 Vector 검색은 후속 Issue 범위다.
