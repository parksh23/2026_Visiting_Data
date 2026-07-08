from fastapi import FastAPI, Depends, HTTPException
from pydantic import BaseModel
from typing import List
from fastapi.middleware.cors import CORSMiddleware
from starlette.middleware.base import BaseHTTPMiddleware
from sqlalchemy.orm import Session
from sqlalchemy import func

# 💡 앞서 설계한 database.py와 models.py에서 연동 도구들을 가져옵니다.
from database import get_db, engine
import models
from log_control import setup_logging
from middleware import log_requests

app = FastAPI(title="Busan Quest API Server")

# 로깅 시스템 가동
setup_logging()

# 안드로이드 에뮬레이터 및 외부 프론트엔드 접근을 위한 CORS 허용 설정
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 🚨 서버가 인스턴스화될 때 오라클 DB에 테이블이 없으면 자동으로 구조를 매핑해 줍니다.
models.Base.metadata.create_all(bind=engine)


# --- 1. Pydantic 모델 정의 (안드로이드 DTO와 1:1 대응) ---

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

# 💡 [새로 추가] 오라클 DB의 미션 데이터를 안드로이드에 안전하게 넘겨주기 위한 규격 명세
class MissionResponse(BaseModel):
    mission_id: int
    mission_type: str
    region_name: str
    mission_name: str
    difficulty: str | None
    base_score: int | None
    final_score: int | None

    class Config:
        from_attributes = True  # SQLAlchemy 모델 객체를 Pydantic이 자동으로 읽도록 설정


# --- 2. API 엔드포인트 라우터 (진짜 DB 연동 영역) ---

@app.get("/")
def read_root():
    return {"message": "Busan Quest API Server is running successfully with Oracle DB!"}

@app.get("/api/v1/users/me", response_model=UserProfile)
def get_my_profile():
    # 유저 정보 테이블 연동 전까지 유지되는 온전한 가짜 데이터
    return UserProfile(
        name="부산갈매기",
        points="2,450P",
        completed_missions=86,
        saved_missions=28
    )

# 🎯 [새로 추가] DBeaver로 넣은 34개의 부산 미션 전체 리스트를 DB에서 긁어와 반환합니다.
@app.get("/api/v1/missions", response_model=List[MissionResponse])
def get_all_missions(db: Session = Depends(get_db)):
    missions = db.query(models.Mission).all()
    return missions

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

# 🎯 [실시간 DB 연동으로 변경] 
# 기존 하드코딩을 걷어내고, 오라클 DB에 저장된 실제 데이터 기반으로 구군별 총 미션 수를 실시간 집계합니다.
@app.get("/api/v1/districts/progress", response_model=List[DistrictProgress])
def get_district_progress(db: Session = Depends(get_db)):
    # SQL 정석: SELECT region_name, COUNT(mission_id) FROM missions GROUP BY region_name;
    results = db.query(
        models.Mission.region_name, 
        func.count(models.Mission.mission_id).label("total_count")
    ).group_by(models.Mission.region_name).all()
    
    progress_list = []
    
    # DB에서 조회해 온 지역명과 카운트 값을 규격에 맞게 리스트로 변환
    for region_name, total_count in results:
        # 💡 아직 '유저가 미션을 완료했는지 기록하는 테이블'은 설계하기 전이므로,
        # 각 구군별 완료 개수(completed)는 우선 1개 또는 0개로 자연스럽게 가짜 처리하고,
        # 총 미션 수(total)는 실제 오라클 DB에 들어있는 갯수를 그대로 반영합니다!
        mock_completed = 1 if total_count > 2 else 0
        
        progress_list.append(
            DistrictProgress(
                name=region_name,
                completed=mock_completed,
                total=total_count
            )
        )
    
    # 안드로이드 UI에서 보기 좋게 총 미션 수가 많은 지역순(내림차순)으로 정렬하여 보냅니다.
    progress_list.sort(key=lambda x: x.total, reverse=True)
    return progress_list

@app.get("/api/v1/rankings", response_model=List[RankEntry])
def get_rankings():
    return [
        RankEntry(rank=1, name="바다사랑이", score="5,620P"),
        RankEntry(rank=2, name="해운대모험가", score="4,320P"),
        RankEntry(rank=3, name="광안리러버", score="3,150P"),
        RankEntry(rank=12, name="부산갈매기 (나)", score="2,450P", is_me=True),
    ]


# --- 3. 카카오 로그인 ---

class KakaoLoginRequest(BaseModel):
    access_token: str

class LoginResponse(BaseModel):
    token: str

@app.post("/api/v1/auth/kakao", response_model=LoginResponse)
async def kakao_login(req: KakaoLoginRequest):
    """
    앱이 카카오 SDK 로 받은 access_token 을 받아서
    1) 카카오 서버에 토큰을 검증하고 사용자 정보를 조회한 뒤
    2) 우리 서버의 JWT 를 발급해 돌려준다.
    """
    # 1) 카카오에 토큰 검증 + 사용자 정보 조회
    try:
        async with httpx.AsyncClient(timeout=10.0) as client:
            resp = await client.get(
                "https://kapi.kakao.com/v2/user/me",
                headers={"Authorization": f"Bearer {req.access_token}"},
            )
    except httpx.RequestError:
        raise HTTPException(status_code=502, detail="카카오 서버와 통신할 수 없습니다.")

    if resp.status_code != 200:
        raise HTTPException(status_code=401, detail="유효하지 않은 카카오 토큰입니다.")

    kakao_user = resp.json()
    kakao_id = kakao_user.get("id")
    if kakao_id is None:
        raise HTTPException(status_code=401, detail="카카오 사용자 정보를 가져올 수 없습니다.")

    # (참고) 닉네임/이메일이 필요하면 아래에서 꺼내 DB 저장/회원가입에 사용
    account = kakao_user.get("kakao_account", {})
    nickname = account.get("profile", {}).get("nickname")
    email = account.get("email")

    # 2) 여기서 DB 에 kakao_id 로 회원 조회/생성 (DB 연결 후 구현)
    #    지금은 DB 가 없으므로 kakao_id 를 그대로 사용자 식별자로 사용한다.

    # 3) 우리 서버 JWT 발급
    payload = {
        "sub": f"kakao:{kakao_id}",
        "provider": "kakao",
        "nickname": nickname,
        "email": email,
    }
    token = jwt.encode(payload, JWT_SECRET, algorithm=JWT_ALGORITHM)

    return LoginResponse(token=token)
