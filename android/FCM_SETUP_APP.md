# 앱 FCM 연동 안내 (프론트)

작성일: 2026-08-20
서버 쪽 설정은 `backend/FCM_SETUP.md` 를 본다. 이 문서는 **앱에서 해야 할 일**만 다룬다.

---

## 지금 상태 (2026-08-21 갱신)

**`app/google-services.json` 이 들어왔고, FCM 이 켜졌다.**

| 항목 | 값 |
|---|---|
| Firebase 프로젝트 | `busanquest` (project number `949382330358`) |
| 등록된 패키지명 | `kr.co.busanquest` ✅ `applicationId` 와 일치 |
| Mobile SDK App ID | `1:949382330358:android:ef75d32852795ca1a839d7` |

`app/build.gradle.kts` 의 조건부 분기가 이제 참이 되어 google-services 플러그인이
자동으로 적용된다. 다음 빌드부터 `PushRegistrar` 가 실제 FCM 토큰을 받아
로그인 시 서버 `PUSH_TOKENS` 에 등록한다.

> `google-services.json` 은 **의도적으로 커밋한다** (`.gitignore` 에서 제외했다).
> 빠져 있으면 다른 팀원 PC 나 CI 빌드에서 FCM 이 조용히 꺼진 채로 나오기 때문이다.
> 이 파일에는 비공개 키가 없다 — 프로젝트 ID 와, 패키지명·인증서로 제한된 Android API 키뿐이다.
>
> 반면 서버가 쓰는 **Firebase Admin SDK 서비스 계정 JSON 은 진짜 개인키라 절대 커밋 금지**다.
> (`firebase-service-account*.json`, `*-firebase-adminsdk-*.json`, `serviceAccountKey.json` 로 차단해 뒀다)

---

## Firebase 콘솔에서 할 일 (1~4 완료, 5번만 남음)

1. Firebase 콘솔에서 프로젝트를 만든다.
2. **Android 앱을 패키지명 `kr.co.busanquest` 로 등록한다.**

   > ⚠️ `backend/FCM_SETUP.md` 에는 `com.example.busasnquest` 로 적혀 있는데 **낡은 값**이다.
   > 지금 `applicationId` 는 `kr.co.busanquest` 이고, 다른 값으로 등록하면 푸시가 오지 않는다.

3. `google-services.json` 을 받아 **`android/app/google-services.json`** 에 넣는다.
4. Android Studio 에서 다시 빌드한다. 플러그인이 자동으로 켜진다.
5. 백엔드 담당자에게 **Firebase Admin SDK 서비스 계정 JSON** 을 전달한다
   (서버가 `FIREBASE_CREDENTIALS_JSON` 으로 쓴다).

> 서비스 계정 JSON은 **Git 에 커밋하지 않는다.** (`google-services.json` 은 커밋 대상)

---

## 앱에 들어간 것

| 파일 | 역할 |
|---|---|
| `util/PushRegistrar.kt` | FCM 토큰을 서버에 등록(`POST`)·해제(`DELETE`) |
| `util/BusanQuestMessagingService.kt` | 푸시 수신. 토큰 갱신(`onNewToken`) 재등록 |
| `util/Notifier.showRemote()` | 앱이 떠 있을 때 받은 푸시를 알림으로 표시 |
| `data/repository/NotificationSettingsRepository.kt` | 알림 설정 서버 동기화 |
| `AndroidManifest.xml` | 서비스 등록 + 기본 채널/아이콘/색 meta-data |

### 동작 흐름

- **로그인 직후** (`BusanQuestApp` 의 `LaunchedEffect(status)`)
  → 토큰 등록 + 서버 알림 설정 내려받기. 자동 로그인도 같은 경로를 탄다.
- **로그아웃** (`ProfileViewModel.submitLogout`)
  → 로컬 JWT 를 지우기 **전에** 토큰 해제. 순서를 바꾸면 인증이 안 돼서
  서버에 토큰이 남고, 로그아웃한 기기로 계속 푸시가 간다.
- **회원 탈퇴** → 서버가 `PUSH_TOKENS` 를 같이 지우므로 앱에서 따로 안 부른다.
- **앱이 백그라운드/종료** → 시스템이 알림을 자동으로 띄운다 (`onMessageReceived` 안 불림).
- **앱이 화면에 떠 있음** → `onMessageReceived` 에서 `Notifier.showRemote()` 로 직접 띄운다.

`data["type"]` 은 `NEW_MISSION` | `RANKING_CHANGE` 이고,
서버 `push_notifications.CHANNELS` 및 `Notifier` 의 채널 ID 와 같은 값이다.

---

## 알림 설정이 서버로 옮겨졌다

전에는 기기 DataStore 에만 저장해서, **스위치를 꺼도 서버는 푸시를 계속 보냈다.**
이제 `GET/PATCH /api/v1/users/me/notifications` 로 서버(USER_SETTINGS)에 저장한다.

- 서버가 원본, 기기 DataStore 는 캐시
- 캐시를 남긴 이유: `Notifier` 가 알림을 띄우기 직전에 스위치와 야간 방해 금지를
  확인해야 하는데, 그 시점에 네트워크를 탈 수 없다
- 토글은 낙관적 갱신 — 화면이 먼저 바뀌고, 서버 저장이 실패하면 되돌아온다

---

## 중복 알림 정리

새 미션·랭킹은 **서버 푸시로 일원화**했다.
`HomeViewModel` 의 `Notifier.checkNewMissions`, `RankingViewModel` 의
`Notifier.checkRankChange` 호출을 제거했다.
(함수 자체는 `Notifier` 에 남겨 뒀다 — 푸시 없이 도는 로컬 전용 빌드로 되돌릴 때 필요하다)

미션 인증 결과는 그대로 로컬 알림이다. 동기 API 응답으로 즉시 알 수 있어
서버가 FCM 으로 보내지 않는다.

---

## 확인 방법

1. `google-services.json` 을 넣고 빌드 → 로그인
2. Logcat 에서 `POST /api/v1/users/me/push-token` 요청 확인 (디버그 빌드에서만 보인다)
3. DB `PUSH_TOKENS` 에 행이 생겼는지 확인
4. Firebase 콘솔 > Messaging 에서 테스트 메시지 전송
   - 앱을 **끈 상태**로 받으면 시스템 알림
   - 앱을 **켠 상태**로 받으면 `onMessageReceived` → `Notifier.showRemote`
     (이때 `data` 에 `type` 을 넣어야 뜬다)
5. 알림 설정에서 스위치를 끄고 DB `USER_SETTINGS` 값이 바뀌는지 확인

## 아직 안 한 것

- 알림 탭 시 특정 화면으로 보내는 딥링크. 지금은 전부 `MainActivity` 로만 연다.
  서버가 `data` 에 `mission_id` 를 넣어 주면 미션 상세로 보낼 수 있다.
- 실제 기기에서의 수신 검증 (이 문서 작성 시점엔 Firebase 프로젝트가 없어 미실행)
