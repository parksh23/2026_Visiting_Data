import os
import base64
import zipfile
import shutil
from pathlib import Path
from dotenv import load_dotenv
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker, declarative_base

# 💡 1. .env 파일의 절대 경로 지정 (현재 파일 위치 기준)
BASE_DIR = Path(__file__).resolve().parent
load_dotenv(dotenv_path=BASE_DIR / ".env")

ORACLE_USER = os.getenv("ORACLE_USER")
ORACLE_PASSWORD = os.getenv("ORACLE_PASSWORD")
ORACLE_DSN = os.getenv("ORACLE_DSN")
ORACLE_WALLET_PASSWORD = os.getenv("ORACLE_WALLET_PASSWORD")

# 💡 2. 월렛 폴더의 절대 경로 지정 (backend/wallet)
WALLET_DIR = BASE_DIR.parent / "wallet"
WALLET_B64 = os.getenv("WALLET_ZIP_BASE64")

# 현재 실행 환경이 Render 클라우드인지 확인 (Render는 자동으로 RENDER=true 변수를 주입함)
IS_RENDER_ENV = os.getenv("RENDER") == "true"

if IS_RENDER_ENV:
    # 1. 클라우드 서버 환경: Base64 텍스트가 반드시 있어야 함
    if not WALLET_B64:
        raise ValueError("🚨 [서버 에러] Render 환경변수 'WALLET_ZIP_BASE64'가 비어있습니다! 대시보드를 확인해주세요.")
else:
    # 2. 로컬(내 컴퓨터) 환경: Base64 텍스트 대신 실제 wallet 폴더가 있어야 함
    if not WALLET_DIR.exists():
        raise ValueError(f"🚨 [로컬 에러] 로컬 지갑 폴더를 찾을 수 없습니다! ({WALLET_DIR} 경로에 오라클 지갑 압축을 풀어주세요.)")

# 서버 내부에 tnsnames.ora가 없다면 환경변수 텍스트를 풀어 지갑을 자동으로 만듭니다.
if not (WALLET_DIR / "tnsnames.ora").exists():
    WALLET_DIR.mkdir(parents=True, exist_ok=True)
    zip_path = WALLET_DIR / "wallet.zip"

    # 1. 텍스트를 다시 zip 파일로 복원
    with open(zip_path, "wb") as f:
        f.write(base64.b64decode(WALLET_B64))

    # 2. 압축 해제
    with zipfile.ZipFile(zip_path, 'r') as zip_ref:
        zip_ref.extractall(WALLET_DIR)

    # 3. 2중 폴더 방어 코드 (압축 해제 시 하위 폴더가 한 겹 더 생기면 파일을 밖으로 꺼냄)
    for item in WALLET_DIR.iterdir():
        if item.is_dir():
            for sub_item in item.iterdir():
                shutil.move(str(sub_item), str(WALLET_DIR))

DATABASE_URL = f"oracle+oracledb://{ORACLE_USER}:{ORACLE_PASSWORD}@{ORACLE_DSN}"

engine = create_engine(
    DATABASE_URL,
    echo=True,
    connect_args={
        # 오라클 드라이버가 인식할 수 있도록 문자열(str)로 변환하여 주입합니다.
        "config_dir": str(WALLET_DIR),
        "wallet_location": str(WALLET_DIR),
        "wallet_password": ORACLE_WALLET_PASSWORD,
    }
)

SessionLocal = sessionmaker(
    autocommit=False,
    autoflush=False,
    bind=engine
)

Base = declarative_base()

def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()