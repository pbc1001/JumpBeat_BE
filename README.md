# JumpBeat Backend

노래를 들으며 가사를 입력하는 리듬형 타자 연습 서비스 **JumpBeat**의 백엔드입니다.

현재 Kotlin Spring Boot 기반과 최초 데이터베이스 스키마가 구성되어 있으며, 아래 문서를 기준으로 기능을 개발합니다.

## 설계 문서

- [요구사항](./REQUIREMENTS.md)
- [데이터베이스 구조](./ERD.md)
- [API 명세](./API_SPEC.md)
- [개발 계획](./DEVELOPMENT_PLAN.md)

## 예정 기술 스택

- Kotlin + Java 21
- Spring Boot + Spring MVC
- PostgreSQL
- Spring Data JPA + Hibernate
- Flyway
- Spring Security + JWT 인증
- springdoc-openapi + Swagger UI
- JUnit + MockK
- Gradle Kotlin DSL
- Docker Compose

## 핵심 원칙

- YouTube 영상은 다운로드하지 않고 공식 IFrame Player로 재생합니다.
- 서버에는 YouTube URL 전체가 아닌 `videoId`를 저장합니다.
- 곡은 싱크 편집 중에는 비공개 초안이며, 최종 등록 시 즉시 공개됩니다.
- 게임 진행과 실시간 판정은 프론트엔드에서 수행하고 서버는 결과를 검증·저장합니다.
- 이 폴더에 서버가 생성된 뒤 실행 방법과 환경 변수 설명을 추가합니다.

## 로컬 실행 준비

필요한 프로그램:

- Java 21 이상
- Docker Desktop 또는 별도로 설치한 PostgreSQL

환경 변수 예시는 `.env.example`, 로컬 PostgreSQL 구성은 `compose.yml`에 있습니다.

```powershell
docker compose up -d
.\gradlew.bat bootRun
```

실행 후 확인할 주소:

- 서버 상태: `http://localhost:8080/api/v1/health`
- Swagger UI: `http://localhost:8080/docs`
- OpenAPI JSON: `http://localhost:8080/api-docs`

테스트와 빌드:

```powershell
.\gradlew.bat test
.\gradlew.bat build
```
