import os
from pathlib import Path
import oracledb
from dotenv import load_dotenv
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker, declarative_base

# .env 파일에서 환경변수 로드
# 실행 위치(cwd)에 상관없이 이 database.py 파일과 같은 폴더의 .env를 찾도록 경로를 명시합니다.
ENV_PATH = Path(__file__).resolve().parent / ".env"
load_dotenv(dotenv_path=ENV_PATH)

# --- 1. Oracle 접속 정보 (.env 파일에서 불러옴) ---
ORACLE_USER = os.getenv("ORACLE_USER")
ORACLE_PASSWORD = os.getenv("ORACLE_PASSWORD")

# [NOTE] ORACLE_DSN: Wallet 없이 TCPS(단방향 SSL)로 접속하는 전체 Connection Descriptor.
# 예)
# (description=
#   (retry_count=20)(retry_delay=3)
#   (address=(protocol=tcps)(port=1522)(host=adb.ap-osaka-1.oraclecloud.com))
#   (connect_data=(service_name=gf42e2bf77db727_visiting2026_medium.adb.oraclecloud.com))
#   (security=(ssl_server_dn_match=yes))
# )
# .env 파일에는 줄바꿈 없이 한 줄로 이어서 넣으세요.
ORACLE_DSN = os.getenv("ORACLE_DSN")

if not all([ORACLE_USER, ORACLE_PASSWORD, ORACLE_DSN]):
    raise RuntimeError(
        "환경변수가 누락되었습니다. .env 파일에 "
        "ORACLE_USER / ORACLE_PASSWORD / ORACLE_DSN 을 설정하세요."
    )


# --- 2. SQLAlchemy 엔진 생성 ---
# DSN 문자열에 괄호/콜론 등 특수문자가 많아서 URL로 조립하면 파싱이 깨지기 쉽습니다.
# 그래서 creator 콜백을 통해 oracledb.connect()를 직접 호출하는 방식을 씁니다.
# (python-oracledb의 Thin 모드이므로 Instant Client 설치도 필요 없습니다.)
def _creator():
    return oracledb.connect(
        user=ORACLE_USER,
        password=ORACLE_PASSWORD,
        dsn=ORACLE_DSN,
    )


engine = create_engine(
    "oracle+oracledb://",
    creator=_creator,
    pool_pre_ping=True,   # 연결이 끊겼는지 매 요청 전 확인 (idle timeout 대비)
    pool_recycle=1800,    # 30분마다 커넥션 재생성
)

SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
Base = declarative_base()


# --- 3. FastAPI 의존성 주입용 DB 세션 ---
def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()