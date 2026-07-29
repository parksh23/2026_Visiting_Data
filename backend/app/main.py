from fastapi import FastAPI
from fastapi.responses import Response
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import Optional, Dict, Any

from pathlib import Path
from datetime import datetime
import json

from database import Base, engine
import models
from routers import text_files
from routers import api_v1
from routers import rankings


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
app.include_router(rankings.router)


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

# =========================
# 404 에러 해결을 위한 추가 라우터
# =========================

@app.get("/favicon.ico", include_in_schema=False)
def favicon():
    """브라우저의 자동 favicon 요청으로 인한 404 에러 방지용"""
    return Response(content="", media_type="image/x-icon")

@app.get("/logs")
def read_logs():
    """저장된 서버 신호 로그(txt)를 브라우저에서 확인할 수 있게 반환"""
    if not LOG_FILE.exists():
        return {"message": "아직 저장된 로그 파일이 없습니다.", "logs": []}

    logs_list = []
    with open(LOG_FILE, "r", encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if line:
                logs_list.append(json.loads(line))

    return {"total_logs": len(logs_list), "logs": logs_list}