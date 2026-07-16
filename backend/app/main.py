from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import Optional, Dict, Any

from pathlib import Path
from datetime import datetime
import json

from app.database import Base, engine
from app import models
from app.routers import text_files
from app.routers import api_v1


app = FastAPI()

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# DB 테이블 생성
Base.metadata.create_all(bind=engine)

# 라우터 등록
app.include_router(text_files.router)
app.include_router(api_v1.router)


# =========================
# 서버 상태 확인용 루트 API
# =========================

@app.get("/")
def read_root():
    return {"message": "Busan Quest API Server is running successfully!"}


# =========================
# 로그 txt 저장 설정
# =========================

BASE_DIR = Path(__file__).resolve().parents[1]
LOG_DIR = BASE_DIR / "logs"
LOG_FILE = LOG_DIR / "server_signals.txt"

LOG_DIR.mkdir(exist_ok=True)


class ServerSignal(BaseModel):
    signal_type: str
    user_id: Optional[str] = None
    district_name: Optional[str] = None
    message: Optional[str] = None
    data: Optional[Dict[str, Any]] = None


def save_signal_log(signal: ServerSignal):
    log_data = {
        "time": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
        "signal_type": signal.signal_type,
        "user_id": signal.user_id,
        "district_name": signal.district_name,
        "message": signal.message,
        "data": signal.data
    }

    with open(LOG_FILE, "a", encoding="utf-8") as f:
        f.write(json.dumps(log_data, ensure_ascii=False) + "\n")


@app.post("/logs/signal")
def receive_signal(signal: ServerSignal):
    save_signal_log(signal)

    return {
        "message": "신호 로그 저장 완료",
        "saved_to": str(LOG_FILE)
    }
