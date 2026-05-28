from fastapi import FastAPI
from pydantic import BaseModel
from typing import List
from fastapi.middleware.cors import CORSMiddleware

app = FastAPI(title="Busan Quest API Server")

# 안드로이드 에뮬레이터에서 접근할 수 있도록 CORS 설정 허용
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

# --- 1. Pydantic 모델 정의 (안드로이드의 data class와 동일) ---

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

# --- 2. API 엔드포인트 라우터 ---
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
        # 데이터베이스가 연결되면 DB에서 계산해서 가져올 부분입니다.
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
