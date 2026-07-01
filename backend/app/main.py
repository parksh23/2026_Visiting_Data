from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from typing import List
from fastapi.middleware.cors import CORSMiddleware
import os
import httpx
import jwt

app = FastAPI(title="Busan Quest API Server")

# JWT 서명용 비밀키 (실서비스에서는 환경변수로 관리하세요)
JWT_SECRET = os.getenv("JWT_SECRET", "busan-quest-dev-secret-change-me")
JWT_ALGORITHM = "HS256"

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
