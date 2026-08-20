# BusasnQuest

부산 16개 구·군의 장소를 미션으로 탐험하고 인증 사진·현재 위치·영수증으로 완료하는 공모전용 Android 앱입니다.

## 프로젝트 구성

```text
android/                   Jetpack Compose Android 앱
  app/src/main/java/.../
    ui/                    화면, 공통 컴포넌트, ViewModel
    data/model/            앱 내부 모델
    data/remote/           Retrofit API와 DTO
    data/repository/       미션·사용자·랭킹 상태 및 서버 연동
backend/
  app/
    main.py                FastAPI 진입점
    routers/api_v1.py      v1 API
    models.py              Oracle SQLAlchemy 모델
    tourism_scoring.py     관광지수 기반 보상 계산
    scheduler.py           월별 점수 갱신 작업
  migrations/              Oracle 스키마 변경 이력
  tests/                   API 계약 및 점수 계산 테스트
render.yaml                Render 배포 Blueprint
```

## Android 실행

1. Android Studio에서 `android/`를 엽니다.
2. JDK 17 이상과 Android SDK 36을 설정합니다.
3. 필요하면 `~/.gradle/gradle.properties`에 아래 값을 덮어씁니다.

```properties
BUSANQUEST_API_BASE_URL=https://your-api.example.com/
BUSANQUEST_KAKAO_NATIVE_APP_KEY=your-kakao-native-app-key
# 아래 네 값은 Play 업로드 키 생성 후에만 설정합니다.
BUSANQUEST_STORE_FILE=C:/secure/busanquest-upload.jks
BUSANQUEST_STORE_PASSWORD=your-store-password
BUSANQUEST_KEY_ALIAS=upload
BUSANQUEST_KEY_PASSWORD=your-key-password
```

4. `app` 구성을 실행합니다. 릴리스 빌드에서는 HTTP 로그와 샘플 미션이 자동으로 비활성화됩니다.

## Backend 실행

```powershell
cd backend
python -m venv .venv
.venv\Scripts\activate
pip install -r requirements-dev.txt
Copy-Item app/.env.example app/.env
cd app
uvicorn main:app --reload
```

Oracle 또는 SQLite 연결 정보와 JWT, Gemini, SMTP 키는 `backend/app/.env`에 설정합니다. 비밀정보와 Oracle Wallet은 저장소에 추가하지 않습니다.

테스트는 저장소 루트에서 실행합니다.

```powershell
python -m pytest backend/tests -q
```

## 출시 전 외부 설정

- Kakao Developers에서 현재 Android 패키지명과 키 해시 등록
- Render 환경변수 및 Oracle 연결 설정
- Google Play Console 개인정보처리방침 URL과 데이터 보안 양식 등록
- 서명 키로 AAB 생성 후 내부 테스트 트랙 검증

서명 설정이 없더라도 `bundleRelease`는 검증용 미서명 AAB를 만들 수 있지만 Play Console에는 업로드할 수 없습니다. 업로드 키는 저장소 밖에 보관하고 위 Gradle 속성으로만 연결합니다.
