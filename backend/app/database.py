import os
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
# .env의 '../wallet'에 의존하지 않고, 코드가 직접 부모 폴더(backend) 안의 wallet 폴더를 계산합니다.
WALLET_DIR = str(BASE_DIR.parent / "wallet")

DATABASE_URL = f"oracle+oracledb://{ORACLE_USER}:{ORACLE_PASSWORD}@{ORACLE_DSN}"

engine = create_engine(
    DATABASE_URL,
    echo=True,
    connect_args={
        "config_dir": WALLET_DIR,
        "wallet_location": WALLET_DIR,
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