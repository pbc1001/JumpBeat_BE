# JumpBeat 백엔드 개발 계획

## 1. 목표 기술 스택

| 영역 | 선택 | 목적 |
|---|---|---|
| 언어 | Kotlin | 간결한 문법과 null 안정성을 갖춘 JVM 서버 개발 |
| 실행 환경 | Java 21 | 장기 지원되는 JVM 기준 |
| 서버 | Spring Boot + Spring MVC | 계층 구조, 검증, 보안, 운영 기능 지원 |
| DB | PostgreSQL | 관계형 데이터와 개인 플레이 기록 저장 |
| ORM | Spring Data JPA + Hibernate | 엔티티 관계와 저장소 계층 구현 |
| 마이그레이션 | Flyway | 버전이 기록되는 DB 변경 관리 |
| 인증 | Spring Security + JWT | API 인증과 로그인 유지 |
| 비밀번호 | BCryptPasswordEncoder | Spring Security 기반 단방향 해시 |
| 문서 | springdoc-openapi + Swagger UI | 실행 가능한 API 명세 |
| 테스트 | JUnit + H2 | 단위·통합 API 테스트 |
| 빌드 | Gradle Kotlin DSL | Kotlin 기반 빌드와 의존성 관리 |
| 로컬 DB | 파일형 H2 | 별도 설치 없이 유지되는 개발 DB |

정확한 패키지 버전은 프로젝트 생성 시 호환되는 안정 버전을 고정한다.

## 2. 예정 의존성

Spring Initializr 의존성:

- `Spring Web`: REST API와 Spring MVC
- `Spring Data JPA`: Repository와 Hibernate ORM
- `Validation`: 요청 DTO 검증
- `Spring Security`: 인증·인가와 비밀번호 해시
- `PostgreSQL Driver`: PostgreSQL JDBC 연결
- `Flyway Migration`: SQL 기반 DB 버전 관리
- `Spring Boot Actuator`: health API와 운영 상태 확인

추가 라이브러리:

- `org.jetbrains.kotlin:kotlin-reflect`: Spring의 Kotlin 리플렉션 지원
- `com.fasterxml.jackson.module:jackson-module-kotlin`: Kotlin JSON 변환
- `org.springdoc:springdoc-openapi-starter-webmvc-ui`: OpenAPI와 Swagger UI
- `io.jsonwebtoken:jjwt-api`, `jjwt-impl`, `jjwt-jackson`: JWT 생성·검증
- `io.mockk:mockk`: Kotlin 친화적 mocking
- `com.ninja-squad:springmockk`: Spring 테스트에서 MockK 사용

Gradle plugin:

- `org.springframework.boot`
- `io.spring.dependency-management`
- `org.jetbrains.kotlin.jvm`
- `org.jetbrains.kotlin.plugin.spring`
- `org.jetbrains.kotlin.plugin.jpa`

Kotlin 클래스는 기본적으로 final이므로 Spring proxy와 JPA를 위해 `kotlin-spring`, `kotlin-jpa` plugin을 사용한다. 라이브러리는 실제 구현 단계에서 필요한 시점에만 추가한다.

## 3. 환경 변수 초안

```dotenv
SPRING_PROFILES_ACTIVE=local
SERVER_PORT=8080
DB_URL=jdbc:postgresql://localhost:5432/jumpbeat
DB_USERNAME=jumpbeat
DB_PASSWORD=replace-with-local-password
FRONTEND_ORIGIN=http://localhost:5173
JWT_ACCESS_SECRET=replace-with-long-random-secret-at-least-32-bytes
JWT_ACCESS_EXPIRATION_MS=900000
JWT_REFRESH_EXPIRATION_MS=1209600000
AUTH_COOKIE_SECURE=false
```

실제 비밀값은 `.env`에 저장하고 Git에는 올리지 않는다. 저장소에는 키 이름만 포함한 `.env.example`을 둔다.

YouTube IFrame Player 재생에는 별도의 API 키가 필요하지 않다. YouTube 검색·상세 메타데이터를 Data API로 조회하는 기능을 추가할 때만 Google API 키를 검토한다.

## 4. 구현 단계

### 0단계: 프로젝트 기반

- Gradle Kotlin DSL 기반 Spring Boot 프로젝트 생성
- 환경 변수 검증
- 로컬 H2와 운영 PostgreSQL 프로필 분리
- Spring Data JPA 연결 및 Flyway 최초 마이그레이션
- 전역 요청 검증, 오류 응답 형식, CORS, 보안 헤더 설정
- springdoc-openapi Swagger UI `/docs` 구성
- Actuator를 이용한 상태 확인 `GET /api/v1/health` 구성

완료 기준:

- 새 개발자가 안내 문서만 보고 서버와 DB를 실행할 수 있다.
- health API와 Swagger 화면을 확인할 수 있다.
- Gradle 테스트와 빌드가 성공한다.

### 1단계: 회원과 인증

- 회원가입
- 로그인
- access/refresh token 갱신
- 로그아웃
- 내 정보 조회
- 이메일·닉네임 중복 검증
- 비밀번호 해시와 인증 테스트

완료 기준:

- 프론트 회원가입·로그인 화면이 서버와 연결된다.
- 인증이 필요한 API를 토큰 없이 호출하면 차단된다.

### 2단계: 공개 곡 조회

- 곡·가사 JPA entity와 repository 구현
- 곡 목록, 검색, 필터, 정렬
- 곡 상세와 플레이용 가사 조회
- 중복 후보 검색
- 커서 페이지네이션

완료 기준:

- 프론트의 더미 곡 목록을 서버 데이터로 교체할 수 있다.
- 동일 제목·가수의 후보가 중복 확인 모달에 표시된다.

### 3단계: 곡 등록과 싱크

- YouTube URL 파싱과 videoId 검증
- 곡 초안 및 줄바꿈 기반 가사 생성
- 싱크 일괄 저장
- 시간 증가·영상 길이 검증
- 최종 공개
- 작성자 수정 및 소프트 삭제

완료 기준:

- `곡 정보 입력 → 싱크 기록 → 미리보기 → 공개` 흐름이 끝까지 동작한다.
- 공개되지 않은 초안은 작성자 외에는 조회할 수 없다.

### 4단계: 프론트 YouTube Player와 게임

이 단계의 중심은 프론트엔드지만 백엔드 API와 함께 검증한다.

- 공식 YouTube IFrame Player 연결
- `getCurrentTime()` 기반 싱크 기록
- 현재·이전·다음 가사 표시
- Enter 제출과 문자열 정규화
- Continue/Survival 모드
- Miss, 점수, 콤보 표시
- 버퍼링·일시정지·영상 탐색 처리

완료 기준:

- 실제 등록한 YouTube 영상으로 두 모드를 플레이할 수 있다.
- 영상 시간과 가사 전환이 일치한다.

### 5단계: 결과와 개인 기록

- 줄별 판정 및 결과 저장
- 서버 통계 재계산
- 모드별 개인 기록 조회

완료 기준:

- 결과 화면에 서버가 확정한 통계가 표시된다.
- 같은 곡을 여러 번 플레이해 자신의 과거 기록을 조회할 수 있다.

### 6단계: 운영 안전장치

- 콘텐츠 신고
- 관리자 숨김 처리
- 요청 횟수 제한
- 입력 크기 제한 및 로그 정책
- 재생 불가 곡 상태 처리
- 이용약관·저작권 안내 연결

완료 기준:

- 신고된 곡을 관리자가 숨길 수 있다.
- 악성·과도한 요청과 비정상 입력이 차단된다.

## 5. 테스트 우선순위

반드시 자동 테스트할 규칙:

- 이메일 및 닉네임 중복
- 작성자만 초안·곡 수정 가능
- 중복 곡 확인 후에도 등록 가능
- 모든 가사 시작 시간이 순서대로 증가
- 공개되지 않은 초안이 목록에 노출되지 않음
- Enter 정답, 오답, 시간 초과의 점수 계산
- Survival 모드 첫 Miss 이후 판정 거부
- 삭제·숨김 곡의 신규 플레이 차단

## 6. 첫 구현 작업

다음 작업에서는 0단계를 수행한다.

1. `JumpBeat_BE`에 Gradle Kotlin DSL 기반 Spring Boot 프로젝트를 생성한다.
2. 로컬 H2와 운영 PostgreSQL·Flyway를 연결한다.
3. `.env.example`과 실행 프로필을 작성한다.
4. 공통 오류 형식과 요청 검증을 설정한다.
5. Actuator health API와 springdoc Swagger UI를 구성한다.
6. 빌드, 린트, 테스트로 초기 상태를 검증한다.

그 후 회원가입과 로그인부터 기능 단위로 진행한다.
