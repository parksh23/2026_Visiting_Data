### ⚙️ 2. 백엔드 (Backend) `README.md`

```markdown
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

# (가상환경이 켜지면 터미널 줄 맨 앞에 (venv) 라는 글자가 생깁니다!)
