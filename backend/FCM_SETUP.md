# FCM 서버 푸시 배포 설정

## 1. DB 마이그레이션

Oracle 운영 DB에 다음 순서로 적용한다.

```text
008_user_agreements.sql
009_user_settings_and_push.sql
010_fcm_notifications.sql
```

이미 적용한 파일은 다시 실행하지 않는다.

## 2. Firebase 프로젝트

1. Firebase 콘솔에서 프로젝트를 생성한다.
2. Android 앱을 패키지명 `com.example.busasnquest`로 등록한다.
3. 프론트에 `google-services.json`을 전달한다.
4. Firebase Admin SDK 서비스 계정 JSON을 발급한다.

서비스 계정 JSON과 `google-services.json`은 Git에 커밋하지 않는다.

## 3. 서버 환경변수

서비스 계정 JSON 전체를 환경변수로 넣는 방식:

```env
FIREBASE_CREDENTIALS_JSON={"type":"service_account", ...}
INTERNAL_JOB_KEY=충분히-긴-임의의-비밀값
```

서버의 안전한 파일 경로를 사용하는 방식:

```env
GOOGLE_APPLICATION_CREDENTIALS=/absolute/path/firebase-service-account.json
INTERNAL_JOB_KEY=충분히-긴-임의의-비밀값
```

두 Firebase 자격 증명 방식 중 하나만 사용한다.

## 4. 외부 크론

Render 무료 인스턴스가 잠들어도 일일 작업이 실행되도록 외부 크론에서 다음 API를 호출한다.

모든 요청에 다음 헤더가 필요하다.

```http
X-Job-Key: INTERNAL_JOB_KEY와 동일한 값
```

### 새 미션 알림

```http
POST /internal/jobs/new-mission-notifications
```

실행 시각: 매일 `18:00 Asia/Seoul`

### 랭킹 갱신 및 상승 알림

```http
POST /internal/jobs/ranking-notifications
```

실행 시각: 매일 `18:10 Asia/Seoul`

### 야간 보류 푸시

```http
POST /internal/jobs/pending-pushes
```

권장 실행 간격: 1분. 외부 크론 서비스가 1분 간격을 지원하지 않으면 가능한 가장 짧은 간격을 사용한다.

내부 `BackgroundScheduler`에도 같은 작업이 등록되어 있다. 두 실행이 겹쳐도 멱등 키와 DB 선점 로그로 같은 논리 알림의 중복 발송을 방지한다.

## 5. 앱 연동 API

### 토큰 등록

```http
POST /api/v1/users/me/push-token
Authorization: Bearer <access-token>
Content-Type: application/json
```

```json
{
  "token": "FCM token",
  "platform": "android"
}
```

### 로그아웃 시 토큰 해제

```http
DELETE /api/v1/users/me/push-token
Authorization: Bearer <access-token>
Content-Type: application/json
```

```json
{
  "token": "FCM token"
}
```

## 6. 알림 정책

- 새 미션은 지역 설정 사용자에게 같은 구·군 미션만 발송한다.
- 지역 미설정 사용자에게는 부산 전체 신규 미션을 발송한다.
- 신규 미션 여러 개는 하루 한 번 묶어 발송한다.
- 랭킹은 상승한 경우에만 발송한다.
- `night_mute` 사용자는 21:00~08:00 발송을 보류한다.
- 미션 인증 결과는 동기 API 응답과 프론트 로컬 알림을 사용하며 FCM으로 보내지 않는다.
- FCM이 `UNREGISTERED` 또는 `INVALID_ARGUMENT`를 반환한 토큰은 삭제한다.
