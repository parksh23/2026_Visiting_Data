import os
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import PlainTextResponse
from middleware import log_requests
from pydantic import BaseModel
from typing import List

# app. 접두어 제거 및 setup_logging 임포트 추가
from database import Base, engine
import models
from routers import text_files
from log_control import setup_logging

app = FastAPI()

# 로깅 시스템 가동
setup_logging()

app.middleware("http")(log_requests)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

Base.metadata.create_all(bind=engine)

app.include_router(text_files.router)


# --- Pydantic 모델 정의 (안드로이드의 data class와 동일) ---
class UserProfile(BaseModel):
    name: str
    points: str
    completed_missions: int
    saved_missions: int

class OngoingMission(BaseModel):
    title: str
    region: str
    reward: int
    current: int
    total: int

class DistrictProgress(BaseModel):
    name: str
    completed: int
    total: int

class RankEntry(BaseModel):
    rank: int
    name: str
    score: str
    is_me: bool = False


# 중복되던 루트(/) 경로를 하나로 깔끔하게 통합
@app.get("/")
def read_root():
    return {"message": "Busan Quest API Server is running successfully!"}

@app.get("/api/v1/users/me", response_model=UserProfile)
def get_my_profile():
    return UserProfile(
        name="부산갈매기",
        points="2,450P",
        completed_missions=86,
        saved_missions=28
    )

@app.get("/api/v1/missions/ongoing", response_model=List[OngoingMission])
def get_ongoing_missions():
    return [
        OngoingMission(
            title="오륙도 해안길 걷기",
            region="남구 용호동",
            reward=100,
            current=0,
            total=1
        )
    ]

@app.get("/api/v1/districts/progress", response_model=List[DistrictProgress])
def get_district_progress():
    return [
        DistrictProgress(name="중구", completed=3, total=3),
        DistrictProgress(name="동구", completed=2, total=2),
        DistrictProgress(name="해운대구", completed=2, total=2),
        DistrictProgress(name="북구", completed=1, total=2),
        DistrictProgress(name="동래구", completed=1, total=2),
        DistrictProgress(name="수영구", completed=1, total=2),
        DistrictProgress(name="남구", completed=0, total=2),
    ]

@app.get("/api/v1/rankings", response_model=List[RankEntry])
def get_rankings():
    return [
        RankEntry(rank=1, name="바다사랑이", score="5,620P"),
        RankEntry(rank=2, name="해운대모험가", score="4,320P"),
        RankEntry(rank=3, name="광안리러버", score="3,150P"),
        RankEntry(rank=12, name="부산갈매기 (나)", score="2,450P", is_me=True),
    ]

# 로그 확인 주소
@app.get("/logs", response_class=PlainTextResponse)
def read_server_logs():
    log_path = "./logs/server_logs.txt"

    if os.path.exists(log_path):
        with open(log_path, "r", encoding="utf-8") as f:
            content = f.read()
        return content

    return "아직 로그 파일이 생성되지 않았습니다."