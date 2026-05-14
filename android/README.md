# 🌊 부산 로컬 도장깨기 (Busan Quest) - Android

부산의 숨겨진 명소, 노포, 둘레길을 탐험하며 도장을 모으는 위치 기반 체크리스트 앱입니다.

## 🛠️ 기술 스택 (Tech Stack)
* **Language:** Kotlin
* **UI Framework:** Jetpack Compose (Material 3)
* **IDE:** Android Studio (최신 버전 권장)

## 🚀 로컬 개발 환경 세팅 (Getting Started)

팀원 여러분, 환영합니다! 아래 순서대로 안드로이드 개발 환경을 세팅해 주세요.

### 1. 필수 프로그램 설치
* [Android Studio](https://developer.android.com/studio)를 설치해 주세요. (설치 시 Android SDK 필수 체크)
* 윈도우 사용자의 경우 **프로젝트 폴더명과 윈도우 계정명에 한글이 없어야 합니다!** (반드시 C드라이브 바로 아래 영어 폴더에 클론하세요. 예: `C:\AndroidProjects`)

### 2. 프로젝트 클론 및 실행
1. 터미널(Git Bash 등)을 열고 프로젝트를 다운로드합니다.
   ```bash
   git clone [https://github.com/BusanQuest/busanquest-android.git](https://github.com/BusanQuest/busanquest-android.git)

2. Android Studio를 켜고 Open을 눌러 다운받은 busanquest-android 폴더를 엽니다.

   우측 하단에 Gradle Sync(코끼리 아이콘 로딩)가 끝날 때까지 기다립니다.

3. Device Manager에서 가상 스마트폰(Emulator)을 생성합니다.

   ⚠️ 주의: 에뮬레이터 충돌 방지를 위해 Pixel 6 (API Level 30) 버전을 권장합니다!

4. 상단의 재생 버튼(▶️ Run 'app')을 눌러 앱을 실행합니다.

📂 폴더 구조 (Folder Structure)
코드가 꼬이지 않도록 각자의 담당 구역을 확인해 주세요.

   ui/screens/ : 메인 화면, 지도, 리스트, 마이페이지 등 각 화면 파일

   ui/components/ : 버튼, 태그, 카드 등 재사용 가능한 UI 블록 모음

   data/ : 서버 API 통신 및 데이터 모델 관리

🚨 Git 협업 규칙 (필독)
메인 브랜치(main)에 직접 푸시(Push)하지 마세요!

   작업 시작 전 항상 git pull을 받아 최신 상태를 유지하세요.

기능을 만들 때마다 본인 이름이나 기능명으로 브랜치를 파서 작업하세요. (예: feature/map-screen)

local.properties 파일이나 API 키가 적힌 파일은 절대 깃허브에 올리지 마세요! (이미 .gitignore에 설정되어 있습니다.)
