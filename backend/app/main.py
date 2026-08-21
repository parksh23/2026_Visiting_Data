from fastapi import FastAPI, HTTPException, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from fastapi.staticfiles import StaticFiles
from fastapi.responses import PlainTextResponse
from pydantic import BaseModel
from typing import Optional, Dict, Any

from pathlib import Path
from datetime import datetime
import json
import os

import firebase_admin
from firebase_admin import credentials

from database import Base, SessionLocal, engine
from document_seed import seed_documents
import models
from routers import text_files
from routers import api_v1
from routers import internal_jobs
from log_control import setup_logging
from scheduler import shutdown_scheduler, start_scheduler

BASE_DIR = Path(__file__).resolve().parents[1]

app = FastAPI()

@app.on_event("startup")
def start_background_jobs():
    setup_logging()

    try:
        firebase_json_str = os.getenv("FIREBASE_CREDENTIALS_JSON")
        if firebase_json_str:
            cred = credentials.Certificate(json.loads(firebase_json_str))
            firebase_admin.initialize_app(cred)
        else:
            key_path = os.getenv("FIREBASE_KEY_PATH")
            if key_path and os.path.exists(key_path):
                cred = credentials.Certificate(key_path)
                firebase_admin.initialize_app(cred)
    except Exception as e:
        print(f"Firebase Init Error: {e}")

    db = SessionLocal()
    try:
        seed_documents(db)
    finally:
        db.close()
    start_scheduler()

@app.on_event("shutdown")
def stop_background_jobs():
    shutdown_scheduler()

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
app.include_router(internal_jobs.router)

UPLOAD_DIR = BASE_DIR / "uploads"
UPLOAD_DIR.mkdir(exist_ok=True)
app.mount("/uploads", StaticFiles(directory=UPLOAD_DIR), name="uploads")


@app.exception_handler(RequestValidationError)
async def validation_exception_handler(
    request: Request, exc: RequestValidationError
):
    return JSONResponse(
        status_code=400,
        content={"detail": "요청 형식이 올바르지 않습니다."},
    )


# =========================
# 서버 상태 확인용 루트 API
# =========================

@app.get("/")
def read_root():
    return {"message": "Busan Quest API Server is running successfully!"}


# =========================
# 로그 txt 저장 및 관리 설정
# =========================

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


# =========================
# 로그 조회 API
# =========================

@app.get("/admin/logs/server", response_class=PlainTextResponse)
def view_server_logs():
    """
    log_control.py에서 기록하는 서버 전체 로그(server_logs.txt)를 조회합니다.
    """
    server_log_file = LOG_DIR / "server_logs.txt"
    if not server_log_file.exists():
        raise HTTPException(status_code=404, detail="서버 로그 파일이 존재하지 않습니다.")

    with open(server_log_file, "r", encoding="utf-8") as f:
        return f.read()


@app.get("/admin/logs/signal", response_class=PlainTextResponse)
def view_signal_logs():
    """
    클라이언트에서 보내는 신호 로그(server_signals.txt)를 조회합니다.
    """
    if not LOG_FILE.exists():
        raise HTTPException(status_code=404, detail="신호 로그 파일이 존재하지 않습니다.")

    with open(LOG_FILE, "r", encoding="utf-8") as f:
        return f.read()