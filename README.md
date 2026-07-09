# 2026_Visiting_Data
```
📦 android (프론트엔드 레포지토리) - 임시(수정되는 대로 반영)
├── app/
│   ├── src/main/java/com/example/busanquest/
│   │   ├── ui/               # 🎨 화면(디자인) 코드만 모아두는 곳
│   │   │   ├── theme/        # 색상, 폰트 세팅 (Color.kt, Theme.kt)
│   │   │   ├── screens/      # 각 화면들 (HomeScreen.kt, MapScreen.kt 등)
│   │   │   └── components/   # 재사용할 레고 블록들 (버튼, 태그, 카드 등)
│   │   ├── data/             # 💾 데이터와 서버 통신을 담당하는 곳
│   │   │   ├── models/       # 데이터 모양 (SpotItem, User 등 데이터 클래스)
│   │   │   └── network/      # 백엔드 서버(FastAPI)와 통신하는 코드 (Retrofit 등)
│   │   └── MainActivity.kt   # 앱이 켜지는 진입점 (최상위 도화지)
│   └── build.gradle.kts      # 프론트엔드 라이브러리 목록
├── .gitignore                # 🚫 깃허브에 올리면 안 되는 파일 목록 (local.properties 등)
└── README.md                 # 안드로이드 파트 실행 방법 가이드

📦 busanquest-backend (백엔드 레포지토리) - 임시(수정되는 대로 반영)
├── app/
│   ├── api/                  # 🌐 프론트엔드가 호출할 API 주소(엔드포인트) 모음
│   │   ├── routes_spot.py    # 장소 관련 API (리스트 조회, 방문 인증 등)
│   │   └── routes_user.py    # 유저 관련 API (점수 갱신, 랭킹 조회 등)
│   ├── core/                 # ⚙️ 설정 파일 (DB 연결 정보, 보안/CORS 설정 등)
│   ├── models/               # 🗄️ 데이터베이스(PostgreSQL) 테이블 구조 설계도
│   ├── schemas/              # 📝 프론트와 주고받을 데이터 형식(Pydantic 모델)
│   ├── services/             # 🧠 핵심 비즈니스 로직 (AI 사진 분석, 점수 계산 등)
│   └── main.py               # 서버가 켜지는 진입점 (FastAPI 앱 실행)
├── requirements.txt          # 파이썬 필수 라이브러리 목록
├── .env.example              # 환경변수 템플릿 (비밀번호 제외한 틀만 공유)
├── .gitignore                # 🚫 깃허브에 올리면 안 되는 파일 목록 (venv, .env 등)
└── README.md                 # 백엔드 파트 실행 및 가상환경 세팅 가이드
```
USER_ID : 인덱스 역할
USER_CODE : 실질적 유저 ID 역할
LOGIN_ID : 사용자 ID
EMAIL : 사용자 이메일
PASSWORD_HASH : 비밀번호 암호화
ACCOUNT_STATUS : 사용자 상태
NICKNAME : 사용자 닉네임
LEVEL_NO : 사용자 LEVEL
TOTAL_POINTS : 사용자 포인트
COMPLETED_MISSIONS : 완료 미션
SAVED_MISSIONS : 저장 미션
CONQUERED_DISTRICTS : 진행중 미션
CREATED_AT : 계정 생성 시간