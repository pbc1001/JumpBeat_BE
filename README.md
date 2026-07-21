# JumpBeat Backend

노래를 들으며 가사를 입력하는 리듬형 타자 연습 서비스 **JumpBeat**의 백엔드입니다.

## 설계 문서

- [요구사항](./REQUIREMENTS.md)
- [데이터베이스 구조](./ERD.md)
- [API 명세](./API_SPEC.md)
- [개발 계획](./DEVELOPMENT_PLAN.md)

## 기술 스택

- Kotlin + Java 21
- Spring Boot + Spring MVC
- Spring Data JPA + Hibernate
- 로컬 개발: 파일형 H2 Database
- 운영 환경: PostgreSQL + Flyway
- Spring Security + JWT 인증
- springdoc-openapi + Swagger UI
- JUnit
- Gradle Kotlin DSL

## 데이터베이스 정책

- 로컬에서는 별도 DB 설치 없이 H2를 사용합니다.
- 로컬 데이터는 `data/` 폴더에 파일로 저장되며 서버를 껐다 켜도 유지됩니다.
- `data/`는 Git에 포함하지 않습니다.
- 실제 배포 환경에서는 `prod` 프로필과 PostgreSQL을 사용합니다.
- 운영 DB 변경은 `src/main/resources/db/migration`의 Flyway SQL로 관리합니다.

## 로컬 실행

필요한 프로그램은 Java 21 이상뿐입니다.

```powershell
.\gradlew.bat bootRun
```

첫 실행 시 H2 DB와 필요한 테이블이 자동 생성됩니다.

실행 후 확인할 주소:

- 서버 상태: `http://localhost:8080/api/v1/health`
- Swagger UI: `http://localhost:8080/docs`
- OpenAPI JSON: `http://localhost:8080/api-docs`

테스트와 빌드:

```powershell
.\gradlew.bat test
.\gradlew.bat build
```

## 운영 환경

배포할 때 다음 환경 변수를 설정합니다.

```dotenv
SPRING_PROFILES_ACTIVE=prod
DB_URL=jdbc:postgresql://host:5432/jumpbeat
DB_USERNAME=jumpbeat
DB_PASSWORD=change-me
JWT_ACCESS_SECRET=replace-with-a-secure-random-secret
FRONTEND_ORIGIN=https://example.com
```

## 핵심 원칙

- YouTube 영상은 다운로드하지 않고 공식 IFrame Player로 재생합니다.
- 서버에는 YouTube URL 전체가 아닌 `videoId`를 저장합니다.
- 곡은 싱크 편집 중에는 비공개 초안이며 최종 등록 시 즉시 공개됩니다.
- 게임의 실시간 판정은 프론트엔드에서 수행하고 서버는 결과를 검증·저장합니다.
