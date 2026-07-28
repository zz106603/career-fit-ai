# ADR-001: JPA 중심 영속성 전략

- 상태: 채택
- 결정일: 2026-07-28

## 배경

초기 Vertical Slice는 SQL과 데이터 제약을 빠르게 검증하기 위해 모든 Repository를
`JdbcClient`로 구현했다. M1부터 Aggregate와 연관 데이터가 늘어나므로 일반 저장·조회까지
수동 SQL과 RowMapper로 유지하면 변경 비용이 커진다. 동시에 pgvector 검색과 비동기 작업
선점은 PostgreSQL 전용 SQL과 영향 행 수 제어가 중요하다.

## 결정

- 일반 Aggregate 저장·조회는 Spring Data JPA를 기본으로 사용한다.
- 도메인 모델은 JPA Entity와 분리하고 infrastructure Adapter에서 변환한다.
- 사용자 소유 데이터 조회에는 `userId` 조건을 Repository 계약과 쿼리에 포함한다.
- 스키마 생성과 제약 관리는 Hibernate가 아니라 Flyway가 담당한다.
- Hibernate는 시작 시 `validate`만 수행하고 Open Session in View는 사용하지 않는다.
- pgvector 저장·유사도 검색, 부분 인덱스 기반 중복 방지, 조건부 상태 전이와 원자 선점은
  native SQL 또는 `JdbcClient`를 제한적으로 사용한다.
- 한 Repository 안에서 JPA와 JDBC를 임의로 혼합하지 않고 SQL 사용 이유가 드러나는
  별도 Adapter 경계를 둔다.

## 적용 범위

JPA 전환 대상:

- 경력과 경력 버전
- 채용공고
- 공고 구조화 결과와 요구사항
- 경력 후보 검색 Snapshot
- 공고 분석 결과와 판정·근거 Snapshot

SQL 유지 대상:

- pgvector 검색 문서 저장과 상태 갱신
- pgvector 유사도 검색
- 후속 비동기 작업의 원자 선점·부분 유니크 인덱스 연계 쿼리

## 결과

일반 영속성에서는 Entity 매핑, 영속성 컨텍스트, Spring Data JPA 쿼리와 트랜잭션 역량을
활용한다. PostgreSQL 고유 기능은 억지로 JPQL로 감싸지 않고 SQL의 원자성과 실행 계획을
명시적으로 관리한다.
