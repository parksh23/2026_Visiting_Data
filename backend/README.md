# 🌊 부산 로컬 도장깨기 (Busan Quest) - Backend

프론트엔드 앱에 데이터를 제공하고, 유저 점수와 랭킹을 관리하는 API 서버입니다.

## 🛠️ 기술 스택 (Tech Stack)
* **Language:** Python 3.9+
* **Framework:** FastAPI
* **Database:** PostgreSQL (개발 시 Docker 활용 권장)
* **Server:** Uvicorn

## 🚀 로컬 개발 환경 세팅 (Getting Started)

안전한 서버 관리를 위해 반드시 **가상환경(Virtual Environment)**을 세팅한 후 작업해 주세요!

### 1. 프로젝트 클론 및 가상환경 생성
터미널을 열고 아래 명령어를 순서대로 실행합니다.

```bash
# 1. 저장소 클론
git clone [https://github.com/BusanQuest/busanquest-backend.git](https://github.com/BusanQuest/busanquest-backend.git)
cd busanquest-backend

# 2. 가상환경(venv) 생성 (파이썬 패키지가 꼬이지 않게 독립된 방을 만듭니다)
python -m venv venv

# 3. 가상환경 켜기
# Windows 사용자인 경우:
venv\Scripts\activate
# Mac/Linux 사용자인 경우:
source venv/bin/activate

# (가상환경이 켜지면 터미널 줄 맨 앞에 (venv) 라는 글자가 생깁니다!)'''

2. 패키지 설치 및 환경 변수 세팅
Bash
# 필수 라이브러리(FastAPI 등) 설치
pip install -r requirements.txt
주의: 데이터베이스 비밀번호나 API 키는 깃허브에 올리면 안 됩니다!

최상단 폴더에 .env.example 파일을 복사하여 .env 파일을 하나 만들고, 리더에게 전달받은 DB 접속 정보와 비밀번호를 채워 넣어주세요.

3. 서버 실행
Bash
uvicorn app.main:app --reload
서버가 켜지면 브라우저에서 http://127.0.0.1:8000/docs 에 접속해 보세요. FastAPI가 자동으로 만들어준 멋진 API 테스트 화면(Swagger UI)을 볼 수 있습니다!

📂 폴더 구조 (Folder Structure)
app/api/ : URL 주소별 라우터 모음 (장소 API, 유저 API 등)

app/models/ : 데이터베이스(PostgreSQL) 테이블 구조 설계도

app/schemas/ : 프론트와 주고받을 데이터 규격(Pydantic 모델)

app/services/ : 핵심 비즈니스 로직 (AI 이미지 분석, 점수 계산 등)

🚨 Git 협업 규칙 및 주의사항
새로운 라이브러리를 설치했다면 반드시 pip freeze > requirements.txt 명령어로 목록을 갱신해서 커밋해 주세요.

venv 폴더와 .env 파일은 절대 깃허브에 커밋(Commit)하면 안 됩니다! (실수로 올리면 서버 다 털립니다 😱)
