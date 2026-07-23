from fastapi import APIRouter, HTTPException, status, Query, Depends
from pydantic import BaseModel
from typing import List, Optional

from sqlalchemy.orm import Session

from database import get_db
from models import AppUser, Mission, UserMission
from auth_utils import (
    create_access_token,
    get_current_user_email,
    hash_password,
    verify_password
)



router = APIRouter(prefix="/api/v1", tags=["api_v1"])


# =========================================================
# 1. Auth DTO
# =========================================================

class LoginRequest(BaseModel):
    email: str
    password: str


class SignupRequest(BaseModel):
    email: str
    password: str


class KakaoLoginRequest(BaseModel):
    access_token: str


class TokenResponse(BaseModel):
    token: str


# =========================================================
# 2. User DTO
# =========================================================

class UserProfile(BaseModel):
    name: str
    points: str
    completed_missions: int
    saved_missions: int


# =========================================================
# 3. Mission DTO
# =========================================================

class MissionDto(BaseModel):
    mission_id: int
    title: str
    location: str
    reward_points: int
    progress_current: int
    progress_total: int
    status: str
    mission_type: str
    image_url: Optional[str] = None


class MissionVerifyRequestDto(BaseModel):
    mission_id: int
    mission_type: str
    photo_url: Optional[str] = None
    latitude: Optional[float] = None
    longitude: Optional[float] = None
    receipt_image_url: Optional[str] = None


class MissionVerifyResponse(BaseModel):
    success: bool
    message: str


# =========================================================
# 4. District DTO
# =========================================================

class DistrictStatusDto(BaseModel):
    district_name: str
    completed_count: int
    total_count: int
    status: str


# =========================================================
# 5. Ranking DTO
# =========================================================

class MyRank(BaseModel):
    rank: int
    topPercent: int
    point: int


class RankingItem(BaseModel):
    rank: int
    userId: str
    name: str
    score: int


class RankingResponse(BaseModel):
    myRank: MyRank
    rankings: List[RankingItem]


# =========================================================
# 6. Auth API
# =========================================================

@router.post("/auth/login", response_model=TokenResponse)
def login(
    req: LoginRequest,
    db: Session = Depends(get_db)
):
    # 이메일로 사용자 조회
    user = db.query(AppUser).filter(AppUser.email == req.email).first()

    # 사용자가 없으면 로그인 실패
    if user is None:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="이메일 또는 비밀번호가 올바르지 않습니다."
        )

    # 정지 계정이면 로그인 차단
    if user.account_status != "ACTIVE":
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="사용할 수 없는 계정입니다."
        )

    # 비밀번호 검증
    if not verify_password(req.password, user.password_hash):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="이메일 또는 비밀번호가 올바르지 않습니다."
        )

    # 로그인 성공 시 JWT 발급
    access_token = create_access_token(
        data={"sub": user.email}
    )

    return {
        "token": access_token
    }

@router.post(
    "/auth/signup",
    response_model=TokenResponse,
    status_code=status.HTTP_201_CREATED
)
@router.post(
    "/auth/signup",
    response_model=TokenResponse,
    status_code=status.HTTP_201_CREATED
)
def signup(
    req: SignupRequest,
    db: Session = Depends(get_db)
):
    # 이메일 형식 검사
    if "@" not in req.email:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="이메일 형식이 올바르지 않습니다."
        )

    # 비밀번호 길이 검사
    if len(req.password) < 8:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="비밀번호는 8자 이상이어야 합니다."
        )

    # bcrypt는 72바이트까지만 처리 가능
    if len(req.password.encode("utf-8")) > 72:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="비밀번호는 72바이트 이하로 입력해주세요."
        )

    # 이메일 중복 확인
    existing_user = db.query(AppUser).filter(AppUser.email == req.email).first()

    if existing_user is not None:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="이미 가입된 이메일입니다."
        )

    # 기존 USER_CODE 중 가장 큰 번호를 찾아서 다음 코드 생성
    user_codes = db.query(AppUser.user_code).all()

    max_number = 0

    for row in user_codes:
        code = row[0]

        if code is None:
            continue

        code = str(code)

        if code.startswith("U"):
            number_part = code[1:]

            if number_part.isdigit():
                max_number = max(max_number, int(number_part))

    next_user_code = f"U{max_number + 1:03d}"

    default_name = req.email.split("@")[0]

    new_user = AppUser(
        user_code=next_user_code,
        login_id=req.email,
        email=req.email,
        password_hash=hash_password(req.password),
        account_status="ACTIVE",
        nickname=default_name,
        level_no=1,
        total_points=0,
        completed_missions=0,
        saved_missions=0,
        conquered_districts=0
    )

    db.add(new_user)
    db.commit()
    db.refresh(new_user)

    access_token = create_access_token(
        data={"sub": new_user.email}
    )

    return {
        "token": access_token
    }


@router.post("/auth/kakao", response_model=TokenResponse)
def kakao_login(req: KakaoLoginRequest):
    # 실제 구현에서는 req.access_token으로 카카오 API 검증
    # 지금은 mock으로 카카오 사용자라고 가정
    kakao_user_email = "kakao_user@example.com"

    access_token = create_access_token(
        data={"sub": kakao_user_email}
    )

    return {
        "token": access_token
    }

# =========================================================
# 7. User API
# =========================================================

@router.get("/users/me", response_model=UserProfile)
def get_my_profile(
    current_user_email: str = Depends(get_current_user_email),
    db: Session = Depends(get_db)
):
    user = db.query(AppUser).filter(AppUser.email == current_user_email).first()

    if user is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="사용자를 찾을 수 없습니다."
        )

    return {
        "name": user.nickname,
        "points": f"{user.total_points:,}P",
        "completed_missions": user.completed_missions,
        "saved_missions": user.saved_missions
    }

from sqlalchemy import func
# from models import AppUser, Mission, UserMission (경로에 맞게 임포트 하세요)

# =========================================================
# 8. Mission API
# =========================================================

@router.get("/missions", response_model=List[MissionDto])
def get_missions(
    current_user_email: str = Depends(get_current_user_email),
    db: Session = Depends(get_db)
):
    user = db.query(AppUser).filter(AppUser.email == current_user_email).first()
    
    # 미션 전체 목록과 현재 유저의 상태를 Outer Join으로 가져옴
    results = db.query(Mission, UserMission.status)\
                .outerjoin(UserMission, (Mission.mission_id == UserMission.mission_id) & (UserMission.user_id == user.user_code))\
                .all()

    missions_response = []
    for mission, status in results:
        missions_response.append({
            "mission_id": mission.mission_id,
            "title": mission.mission_name,
            "location": mission.region_name, # 상세 장소가 없으므로 구/군 이름 활용
            "reward_points": mission.base_score,
            "progress_current": 1 if status == "completed" else 0,
            "progress_total": 1,
            "status": status or "locked",
            "mission_type": mission.mission_type,
            "image_url": None # DB에 컬럼이 없으므로 임시로 None 처리
        })

    return missions_response


@router.get("/missions/ongoing", response_model=List[MissionDto])
def get_ongoing_missions(
    current_user_email: str = Depends(get_current_user_email),
    db: Session = Depends(get_db)
):
    user = db.query(AppUser).filter(AppUser.email == current_user_email).first()
    
    # UserMission 테이블에서 상태가 ongoing인 미션만 Join하여 가져옴
    results = db.query(Mission, UserMission.status)\
                .join(UserMission, Mission.mission_id == UserMission.mission_id)\
                .filter(UserMission.user_id == user.user_code)\
                .filter(UserMission.status == "ongoing")\
                .all()

    return [
        {
            "mission_id": mission.mission_id,
            "title": mission.mission_name,
            "location": mission.region_name,
            "reward_points": mission.base_score,
            "progress_current": 0,
            "progress_total": 1,
            "status": status,
            "mission_type": mission.mission_type,
            "image_url": None
        } for mission, status in results
    ]

# ... verify_mission 엔드포인트는 기존 코드 유지 ...


# =========================================================
# 9. District API
# =========================================================

@router.get("/districts/progress", response_model=List[DistrictStatusDto])
def get_district_progress(
    current_user_email: str = Depends(get_current_user_email),
    db: Session = Depends(get_db)
):
    user = db.query(AppUser).filter(AppUser.email == current_user_email).first()

    # 1. 각 구/군별 '전체' 미션 개수 조회
    total_counts_query = db.query(Mission.region_name, func.count(Mission.mission_id))\
                           .group_by(Mission.region_name).all()
    
    # 2. 각 구/군별 해당 유저가 '완료한(completed)' 미션 개수 조회
    completed_counts_query = db.query(Mission.region_name, func.count(Mission.mission_id))\
                               .join(UserMission, Mission.mission_id == UserMission.mission_id)\
                               .filter(UserMission.user_id == user.user_code)\
                               .filter(UserMission.status == "completed")\
                               .group_by(Mission.region_name).all()

    # 결과를 찾기 쉽게 딕셔너리로 변환 (예: {"해운대구": 2, "수영구": 1})
    completed_dict = {region: count for region, count in completed_counts_query}

    response = []
    for region, total in total_counts_query:
        completed = completed_dict.get(region, 0)
        
        # 상태 판별: 다 깼으면 cleared, 1개라도 깼으면 ongoing, 아니면 locked
        status = "locked"
        if completed == total and total > 0:
            status = "cleared"
        elif completed > 0:
            status = "ongoing"

        response.append({
            "district_name": region,
            "completed_count": completed,
            "total_count": total,
            "status": status
        })

    return response

# =========================================================
# 10. Ranking API
# =========================================================

@router.get("/rankings", response_model=RankingResponse)
def get_rankings(
    type: str = Query("all"),
    current_user_email: str = Depends(get_current_user_email)
):
    print("랭킹 요청 사용자:", current_user_email)
    print("랭킹 타입:", type)

    return {
        "myRank": {
            "rank": 12,
            "topPercent": 15,
            "point": 2450
        },
        "rankings": [
            {
                "rank": 1,
                "userId": "u001",
                "name": "바다사랑이",
                "score": 5620
            },
            {
                "rank": 2,
                "userId": "u002",
                "name": "해운대모험가",
                "score": 4320
            },
            {
                "rank": 3,
                "userId": "u003",
                "name": "로컬탐험대",
                "score": 3980
            }
        ]
    }
