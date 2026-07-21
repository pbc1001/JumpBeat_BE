# JumpBeat API 명세

## 1. 공통 규칙

- Base URL: `/api/v1`
- Content-Type: `application/json`
- 시간 단위: 밀리초(ms)
- 서버 시각: ISO 8601 UTC 문자열
- ID: UUID 문자열
- 인증: `Authorization: Bearer <accessToken>`
- 목록 조회: 커서 기반 페이지네이션
- API 문서는 springdoc-openapi로 생성하고 Swagger UI를 `/docs`에 제공한다.

성공 응답 예시:

```json
{
  "data": {
    "id": "b57fd47f-5138-4ea8-bb5f-d65f4a94fb65"
  }
}
```

오류 응답 예시:

```json
{
  "error": {
    "code": "SONG_NOT_FOUND",
    "message": "곡을 찾을 수 없습니다.",
    "details": null
  }
}
```

주요 상태 코드:

- `200`: 조회·수정 성공
- `201`: 생성 성공
- `204`: 삭제 성공
- `400`: 입력값 오류
- `401`: 인증 필요 또는 토큰 오류
- `403`: 권한 없음
- `404`: 리소스 없음
- `409`: 이메일·닉네임 중복 또는 상태 충돌
- `422`: 싱크 순서 등 도메인 규칙 위반
- `429`: 요청 횟수 제한

## 2. 인증 API

### `POST /auth/signup`

```json
{
  "email": "player@example.com",
  "nickname": "비트마스터",
  "password": "example-password"
}
```

응답 `201`:

```json
{
  "data": {
    "user": {
      "id": "uuid",
      "email": "player@example.com",
      "nickname": "비트마스터"
    },
    "accessToken": "jwt"
  }
}
```

### `POST /auth/login`

```json
{
  "email": "player@example.com",
  "password": "example-password"
}
```

응답은 회원가입과 동일한 사용자 및 access token을 반환한다. refresh token은 HttpOnly cookie로 전달한다.

### `POST /auth/refresh`

HttpOnly refresh cookie를 검증해 새 access token을 발급한다.

### `POST /auth/logout`

refresh token을 무효화하고 cookie를 제거한다.

### `GET /users/me`

로그인한 사용자 정보를 반환한다.

## 3. 곡 조회 API

### `GET /songs`

공개 곡 목록을 조회한다.

쿼리:

- `q`: 제목 또는 가수 검색어
- `language`: `KO`, `EN`, `JA`, `OTHER`
- `difficulty`: `EASY`, `NORMAL`, `HARD`
- `sort`: `LATEST`, `POPULAR`
- `cursor`: 다음 페이지 커서
- `limit`: 기본 20, 최대 50

응답 `200`:

```json
{
  "data": {
    "items": [
      {
        "id": "uuid",
        "title": "Electric Love",
        "artist": "Synth Wave Project",
        "youtubeVideoId": "videoId",
        "language": "EN",
        "difficulty": "NORMAL",
        "lyricLineCount": 42,
        "playCount": 128,
        "creator": {
          "id": "uuid",
          "nickname": "비트마스터"
        },
        "publishedAt": "2026-07-21T00:00:00.000Z"
      }
    ],
    "nextCursor": null
  }
}
```

### `GET /songs/:songId`

공개 곡의 플레이 데이터를 조회한다. 가사는 `lineOrder` 오름차순으로 반환한다.

```json
{
  "data": {
    "id": "uuid",
    "title": "Electric Love",
    "artist": "Synth Wave Project",
    "youtubeVideoId": "videoId",
    "language": "EN",
    "difficulty": "NORMAL",
    "durationMs": 185000,
    "lyrics": [
      {
        "id": "uuid",
        "lineOrder": 0,
        "text": "첫 번째 가사",
        "startTimeMs": 12500
      },
      {
        "id": "uuid",
        "lineOrder": 1,
        "text": "두 번째 가사",
        "startTimeMs": 15800
      }
    ]
  }
}
```

### `GET /songs/duplicates?title=...&artist=...`

정규화된 제목과 가수명을 기준으로 중복 후보를 반환한다. 결과가 있어도 등록을 차단하지 않는다.

```json
{
  "data": {
    "hasDuplicates": true,
    "items": [
      {
        "id": "uuid",
        "title": "Electric Love",
        "artist": "Synth Wave Project",
        "youtubeVideoId": "videoId"
      }
    ]
  }
}
```

라우팅 충돌을 피하기 위해 서버에서는 `duplicates` 정적 경로를 `:songId`보다 먼저 선언한다.

## 4. 곡 제작 API

모든 요청은 로그인이 필요하다. 초안은 작성자에게만 보이며 최종 공개 시 즉시 전체 목록에 노출된다.

### `POST /songs/drafts`

```json
{
  "title": "Electric Love",
  "artist": "Synth Wave Project",
  "youtubeUrl": "https://www.youtube.com/watch?v=videoId",
  "language": "EN",
  "difficulty": "NORMAL",
  "lyricsText": "첫 번째 가사\n두 번째 가사",
  "confirmedDuplicate": true
}
```

서버는 URL을 검증하고 `videoId`를 추출하며 빈 줄을 제외해 가사 줄을 만든다.

응답 `201`:

```json
{
  "data": {
    "id": "uuid",
    "status": "DRAFT",
    "youtubeVideoId": "videoId",
    "lyrics": [
      { "id": "uuid", "lineOrder": 0, "text": "첫 번째 가사", "startTimeMs": null },
      { "id": "uuid", "lineOrder": 1, "text": "두 번째 가사", "startTimeMs": null }
    ]
  }
}
```

중복 후보가 있는데 `confirmedDuplicate`가 `false`이면 `409 DUPLICATE_CONFIRMATION_REQUIRED`와 후보 목록을 반환한다.

### `GET /songs/drafts/:songId`

작성자가 자신의 초안을 조회한다.

### `PATCH /songs/drafts/:songId/sync`

가사 시작 시간을 일괄 저장한다.

```json
{
  "durationMs": 185000,
  "lyrics": [
    { "id": "uuid-1", "startTimeMs": 12500 },
    { "id": "uuid-2", "startTimeMs": 15800 }
  ]
}
```

서버 검증:

- 초안 작성자와 요청자가 같아야 한다.
- 모든 가사 ID가 해당 곡에 속해야 한다.
- 모든 시간이 0 이상이고 영상 길이보다 작아야 한다.
- `lineOrder` 순서대로 시간이 엄격히 증가해야 한다.

### `POST /songs/drafts/:songId/publish`

모든 곡 정보와 싱크를 재검증한 후 `PUBLISHED`로 변경하고 `publishedAt`을 기록한다.

### `PATCH /songs/:songId`

작성자가 공개 곡의 제목, 가수, 언어, 난이도, 영상 또는 가사를 수정한다. 영상이나 가사가 바뀌면 싱크를 다시 검증해야 한다.

### `DELETE /songs/:songId`

작성자 또는 관리자가 곡을 소프트 삭제한다. 응답은 `204`다.

## 5. 게임 결과 API

### `POST /game-results`

로그인이 필요하다. 프론트는 줄별 판정 기록을 보내고 서버는 현재 곡 가사를 기준으로 점수와 통계를 다시 계산한다.

```json
{
  "songId": "uuid",
  "mode": "CONTINUE",
  "playTimeMs": 172300,
  "judgements": [
    {
      "lyricLineId": "uuid-1",
      "judgement": "CORRECT",
      "submittedAtMs": 14820
    },
    {
      "lyricLineId": "uuid-2",
      "judgement": "TIMEOUT",
      "submittedAtMs": null
    }
  ]
}
```

응답 `201`:

```json
{
  "data": {
    "id": "uuid",
    "score": 100,
    "accuracy": 50.0,
    "correctCount": 1,
    "missCount": 1,
    "maxCombo": 1,
    "completionRate": 100.0,
    "playedAt": "2026-07-21T00:00:00.000Z"
  }
}
```

서버는 명백히 불가능한 시간, 중복 가사 판정, 잘못된 순서를 검증한다.

### `GET /users/me/game-results`

내 플레이 기록을 최신순으로 조회한다. `songId`, `mode`, `cursor`, `limit`을 지원한다.

## 6. 신고 API

### `POST /songs/:songId/reports`

```json
{
  "reason": "COPYRIGHT",
  "detail": "신고 상세 내용"
}
```

관리자 신고 목록과 처리 API는 관리자 기능 구현 단계에서 별도 확장한다.

## 7. 주요 오류 코드

| 코드 | HTTP | 의미 |
|---|---:|---|
| `VALIDATION_FAILED` | 400 | 입력값 검증 실패 |
| `INVALID_YOUTUBE_URL` | 400 | 지원하지 않는 YouTube URL |
| `EMAIL_ALREADY_EXISTS` | 409 | 이메일 중복 |
| `NICKNAME_ALREADY_EXISTS` | 409 | 닉네임 중복 |
| `DUPLICATE_CONFIRMATION_REQUIRED` | 409 | 중복 후보 확인 필요 |
| `INVALID_LYRIC_TIMELINE` | 422 | 가사 시간이 없거나 순서가 잘못됨 |
| `SONG_NOT_FOUND` | 404 | 곡이 없거나 접근할 수 없음 |
| `SONG_NOT_PLAYABLE` | 422 | 영상 또는 곡이 플레이 불가능한 상태 |
| `FORBIDDEN_SONG_ACCESS` | 403 | 곡 수정·삭제 권한 없음 |
| `INVALID_GAME_RESULT` | 422 | 판정 기록 검증 실패 |
