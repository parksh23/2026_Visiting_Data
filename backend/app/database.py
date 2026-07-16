import os
import base64
import zipfile
import tempfile
from dotenv import load_dotenv
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker, declarative_base

load_dotenv()

ORACLE_USER = os.getenv("ORACLE_USER")
ORACLE_PASSWORD = os.getenv("ORACLE_PASSWORD")
ORACLE_DSN = os.getenv("ORACLE_DSN")

# Render에 등록한 지갑 관련 변수 가져오기
WALLET_ZIP_BASE64 = os.getenv("WALLET_ZIP_BASE64")
ORACLE_WALLET_PASSWORD = os.getenv("ORACLE_WALLET_PASSWORD")
ORACLE_WALLET_PATH = os.getenv("ORACLE_WALLET_PATH")

# 🌟 클라우드(Render) 환경이라 BASE64 데이터가 존재한다면, 서버 부팅 시 압축 풀기
if WALLET_ZIP_BASE64 and not ORACLE_WALLET_PATH:
    # 서버 내부의 임시 폴더 경로 생성
    wallet_dir = os.path.join(tempfile.gettempdir(), "oracle_wallet")
    os.makedirs(wallet_dir, exist_ok=True)

    zip_path = os.path.join(wallet_dir, "wallet.zip")

    # 1. Base64 텍스트를 디코딩하여 zip 파일로 저장
    with open(zip_path, "wb") as f:
        f.write(base64.b64decode(WALLET_ZIP_BASE64))

    # 2. 압축 풀기
    with zipfile.ZipFile(zip_path, 'r') as zip_ref:
        zip_ref.extractall(wallet_dir)

    # 3. 압축 푼 폴더 내부를 뒤져서 tnsnames.ora 파일이 있는 '진짜 폴더 경로' 찾기
    found_wallet_path = wallet_dir
    for root, dirs, files in os.walk(wallet_dir):
        if "tnsnames.ora" in files:
            found_wallet_path = root
            break

    # 4. 오라클이 찾은 경로를 사용할 수 있도록 지정
    ORACLE_WALLET_PATH = found_wallet_path
    print(f"✅ 오라클 지갑 찐 위치 세팅 완료: {ORACLE_WALLET_PATH}")


DATABASE_URL = f"oracle+oracledb://{ORACLE_USER}:{ORACLE_PASSWORD}@{ORACLE_DSN}"

engine = create_engine(
    DATABASE_URL,
    echo=True,
    connect_args={
        "config_dir": ORACLE_WALLET_PATH,
        "wallet_location": ORACLE_WALLET_PATH,
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