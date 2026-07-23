import base64
import os
import tempfile
import zipfile
from pathlib import Path

from dotenv import load_dotenv
from sqlalchemy import create_engine
from sqlalchemy.orm import declarative_base, sessionmaker

load_dotenv()


def _oracle_database_url() -> str:
    user = os.getenv("ORACLE_USER")
    password = os.getenv("ORACLE_PASSWORD")
    dsn = os.getenv("ORACLE_DSN")
    if not all((user, password, dsn)):
        raise RuntimeError(
            "DATABASE_URL 또는 ORACLE_USER/ORACLE_PASSWORD/ORACLE_DSN을 설정해 주세요."
        )
    return f"oracle+oracledb://{user}:{password}@{dsn}"


def _prepare_wallet() -> str | None:
    wallet_path = os.getenv("ORACLE_WALLET_PATH")
    wallet_base64 = os.getenv("WALLET_ZIP_BASE64")
    if wallet_path or not wallet_base64:
        return wallet_path

    wallet_dir = Path(tempfile.gettempdir()) / "busan_quest_oracle_wallet"
    wallet_dir.mkdir(parents=True, exist_ok=True)
    zip_path = wallet_dir / "wallet.zip"
    zip_path.write_bytes(base64.b64decode(wallet_base64))
    with zipfile.ZipFile(zip_path) as archive:
        archive.extractall(wallet_dir)

    for root, _, files in os.walk(wallet_dir):
        if "tnsnames.ora" in files:
            return root
    return str(wallet_dir)


DATABASE_URL = os.getenv("DATABASE_URL") or _oracle_database_url()
engine_options: dict = {"pool_pre_ping": True}

if DATABASE_URL.startswith("sqlite"):
    engine_options["connect_args"] = {"check_same_thread": False}
elif DATABASE_URL.startswith("oracle"):
    wallet_path = _prepare_wallet()
    if wallet_path:
        engine_options["connect_args"] = {
            "config_dir": wallet_path,
            "wallet_location": wallet_path,
            "wallet_password": os.getenv("ORACLE_WALLET_PASSWORD"),
        }

engine = create_engine(DATABASE_URL, **engine_options)
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
Base = declarative_base()


def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
