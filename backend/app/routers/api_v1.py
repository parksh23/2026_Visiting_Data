import math
import os
import uuid
from pathlib import Path
from typing import List, Optional

import httpx
from fastapi import (
    APIRouter,
    Depends,
    File,
    HTTPException,
    Query,
    Request,
    UploadFile,
    status,
)
from pydantic import BaseModel
from sqlalchemy import func
from sqlalchemy.orm import Session

from auth_utils import (
    create_access_token,
    get_current_user_email,
    hash_password,
    verify_password,
)
from database import get_db
from models import AppUser, District, Mission, UserMission

router = APIRouter(prefix="/api/v1", tags=["api_v1"])

BUSAN_DISTRICTS = [
    "강서구",
    "북구",
    "금정구",
    "기장군",
    "사상구",
    "부산진구",
    "동래구",
    "해운대구",
    "사하구",
    "서구",
    "연제구",
    "수영구",
    "중구",
    "동구",
    "남구",
    "영도구",
]
MISSION_TYPES = {"PHOTO", "CURRENT_LOCATION", "RECEIPT"}
UPLOAD_DIR = Path(__file__).resolve().parents[2] / "uploads"
MAX_UPLOAD_BYTES = int(os.getenv("MAX_UPLOAD_BYTES", str(5 * 1024 * 1024)))


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


class UserProfile(BaseModel):
    name: str
    points: str
    completed_missions: int
    saved_missions: int


class MissionDto(BaseModel):
    mission_id: int
    title: str
    district: str
    location: str
    latitude: Optional[float] = None
    longitude: Optional[float] = None
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


class DistrictStatusDto(BaseModel):
    district_name: str
    completed_count: int
    total_count: int
    status: str


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


class UploadResponse(BaseModel):
    url: str


def _next_user_code(db: Session) -> str:
    max_number = 0
    for (code,) in db.query(AppUser.user_code).all():
        text = str(code or "")
        if text.startswith("U") and text[1:].isdigit():
            max_number = max(max_number, int(text[1:]))
    return f"U{max_number + 1:03d}"


def _get_user(db: Session, subject: str) -> AppUser:
    user = (
        db.query(AppUser)
        .filter((AppUser.email == subject) | (AppUser.user_code == subject))
        .first()
    )
    if user is None:
        raise HTTPException(status_code=401, detail="인증된 사용자를 찾을 수 없습니다.")
    if user.account_status != "ACTIVE":
        raise HTTPException(status_code=403, detail="사용할 수 없는 계정입니다.")
    return user


def _token_for(user: AppUser) -> str:
    return create_access_token({"sub": user.user_code})


def _haversine_m(lat1: float, lon1: float, lat2: float, lon2: float) -> float:
    radius = 6_371_000
    phi1, phi2 = math.radians(lat1), math.radians(lat2)
    d_phi = math.radians(lat2 - lat1)
    d_lambda = math.radians(lon2 - lon1)
    a = (
        math.sin(d_phi / 2) ** 2
        + math.cos(phi1) * math.cos(phi2) * math.sin(d_lambda / 2) ** 2
    )
    return radius * 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))


def _mission_dict(mission: Mission, completed_ids: set[int]) -> dict:
    completed = mission.mission_id in completed_ids
    return {
        "mission_id": mission.mission_id,
        "title": mission.title,
        "district": mission.district_name,
        "location": mission.location,
        "latitude": mission.latitude,
        "longitude": mission.longitude,
        "reward_points": mission.reward_points,
        "progress_current": 1 if completed else 0,
        "progress_total": 1,
        "status": "completed" if completed else "ongoing",
        "mission_type": mission.mission_type,
        "image_url": mission.image_url,
    }


@router.post("/auth/login", response_model=TokenResponse)
def login(req: LoginRequest, db: Session = Depends(get_db)):
    user = db.query(AppUser).filter(AppUser.email == req.email.strip().lower()).first()
    if user is None or not verify_password(req.password, user.password_hash):
        raise HTTPException(status_code=401, detail="이메일 또는 비밀번호가 올바르지 않습니다.")
    if user.account_status != "ACTIVE":
        raise HTTPException(status_code=403, detail="사용할 수 없는 계정입니다.")
    return {"token": _token_for(user)}


@router.post("/auth/signup", response_model=TokenResponse, status_code=201)
def signup(req: SignupRequest, db: Session = Depends(get_db)):
    email = req.email.strip().lower()
    if "@" not in email:
        raise HTTPException(status_code=400, detail="이메일 형식이 올바르지 않습니다.")
    password_bytes = req.password.encode("utf-8")
    if len(req.password) < 8 or len(password_bytes) > 72:
        raise HTTPException(
            status_code=400, detail="비밀번호는 8자 이상, 72바이트 이하여야 합니다."
        )
    if db.query(AppUser).filter(AppUser.email == email).first():
        raise HTTPException(status_code=409, detail="이미 가입된 이메일입니다.")

    user = AppUser(
        user_code=_next_user_code(db),
        login_id=email,
        email=email,
        password_hash=hash_password(req.password),
        nickname=email.split("@")[0],
        account_status="ACTIVE",
    )
    db.add(user)
    db.commit()
    db.refresh(user)
    return {"token": _token_for(user)}


@router.post("/auth/kakao", response_model=TokenResponse)
def kakao_login(req: KakaoLoginRequest, db: Session = Depends(get_db)):
    try:
        response = httpx.get(
            "https://kapi.kakao.com/v2/user/me",
            headers={"Authorization": f"Bearer {req.access_token}"},
            timeout=10,
        )
    except httpx.HTTPError as exc:
        raise HTTPException(status_code=502, detail="카카오 인증 서버에 연결할 수 없습니다.") from exc
    if response.status_code != 200:
        raise HTTPException(status_code=401, detail="유효하지 않은 카카오 액세스 토큰입니다.")

    profile = response.json()
    kakao_id = str(profile.get("id") or "")
    if not kakao_id:
        raise HTTPException(status_code=401, detail="카카오 사용자 정보를 확인할 수 없습니다.")
    account = profile.get("kakao_account") or {}
    email = account.get("email")
    nickname = (account.get("profile") or {}).get("nickname") or f"카카오사용자{kakao_id[-4:]}"

    user = db.query(AppUser).filter(AppUser.kakao_id == kakao_id).first()
    if user is None and email:
        user = db.query(AppUser).filter(AppUser.email == email).first()
    if user is None:
        user = AppUser(
            user_code=_next_user_code(db),
            login_id=f"kakao:{kakao_id}",
            email=email,
            kakao_id=kakao_id,
            nickname=nickname,
            account_status="ACTIVE",
        )
        db.add(user)
    elif not user.kakao_id:
        user.kakao_id = kakao_id
    db.commit()
    db.refresh(user)
    return {"token": _token_for(user)}


@router.get("/users/me", response_model=UserProfile)
def get_my_profile(
    subject: str = Depends(get_current_user_email), db: Session = Depends(get_db)
):
    user = _get_user(db, subject)
    return {
        "name": user.nickname,
        "points": f"{user.total_points:,}P",
        "completed_missions": user.completed_missions,
        "saved_missions": user.saved_missions,
    }


@router.get("/missions", response_model=List[MissionDto])
def get_missions(
    subject: str = Depends(get_current_user_email), db: Session = Depends(get_db)
):
    user = _get_user(db, subject)
    completed_ids = {
        mission_id
        for (mission_id,) in db.query(UserMission.mission_id)
        .filter(
            UserMission.user_code == user.user_code,
            UserMission.status == "completed",
        )
        .all()
    }
    return [
        _mission_dict(mission, completed_ids)
        for mission in db.query(Mission).order_by(Mission.mission_id).all()
    ]


@router.get("/missions/ongoing", response_model=List[MissionDto])
def get_ongoing_missions(
    subject: str = Depends(get_current_user_email), db: Session = Depends(get_db)
):
    return [
        mission
        for mission in get_missions(subject, db)
        if mission["status"] == "ongoing"
    ]


@router.post("/missions/verify", response_model=MissionVerifyResponse)
def verify_mission(
    req: MissionVerifyRequestDto,
    subject: str = Depends(get_current_user_email),
    db: Session = Depends(get_db),
):
    user = _get_user(db, subject)
    mission = db.query(Mission).filter(Mission.mission_id == req.mission_id).first()
    if mission is None:
        raise HTTPException(status_code=404, detail="미션을 찾을 수 없습니다.")

    requested_type = req.mission_type.upper()
    if requested_type not in MISSION_TYPES or requested_type != mission.mission_type:
        return {"success": False, "message": "미션 인증 방식이 올바르지 않습니다."}
    duplicate = (
        db.query(UserMission)
        .filter(
            UserMission.user_code == user.user_code,
            UserMission.mission_id == mission.mission_id,
            UserMission.status == "completed",
        )
        .first()
    )
    if duplicate:
        return {"success": False, "message": "이미 완료한 미션입니다."}
    if requested_type == "PHOTO" and not req.photo_url:
        return {"success": False, "message": "인증 사진을 먼저 업로드해 주세요."}
    if requested_type == "RECEIPT" and not req.receipt_image_url:
        return {"success": False, "message": "영수증 이미지를 먼저 업로드해 주세요."}

    if requested_type in {"PHOTO", "CURRENT_LOCATION"}:
        if req.latitude is None or req.longitude is None:
            return {"success": False, "message": "현재 위치 정보가 필요합니다."}
        if mission.latitude is None or mission.longitude is None:
            return {"success": False, "message": "미션 장소 좌표가 등록되지 않았습니다."}
        distance = _haversine_m(
            req.latitude, req.longitude, mission.latitude, mission.longitude
        )
        if distance > mission.radius_m:
            return {
                "success": False,
                "message": f"미션 장소에서 허용 반경 {mission.radius_m}m 이상 떨어져 있어요.",
            }

    db.add(
        UserMission(
            user_code=user.user_code,
            mission_id=mission.mission_id,
            status="completed",
            photo_url=req.photo_url,
            receipt_image_url=req.receipt_image_url,
        )
    )
    user.total_points += mission.reward_points
    user.completed_missions += 1
    db.commit()
    return {
        "success": True,
        "message": f"미션 인증이 완료되어 {mission.reward_points}P가 적립됐습니다.",
    }


@router.get("/districts/progress", response_model=List[DistrictStatusDto])
def get_district_progress(
    subject: str = Depends(get_current_user_email), db: Session = Depends(get_db)
):
    user = _get_user(db, subject)
    totals = dict(
        db.query(Mission.district_name, func.count(Mission.mission_id))
        .group_by(Mission.district_name)
        .all()
    )
    completed = dict(
        db.query(Mission.district_name, func.count(UserMission.id))
        .join(UserMission, UserMission.mission_id == Mission.mission_id)
        .filter(
            UserMission.user_code == user.user_code,
            UserMission.status == "completed",
        )
        .group_by(Mission.district_name)
        .all()
    )
    result = []
    for district in BUSAN_DISTRICTS:
        total = int(totals.get(district, 0))
        done = int(completed.get(district, 0))
        state = "empty" if total == 0 else ("cleared" if done == total else "ongoing")
        result.append(
            {
                "district_name": district,
                "completed_count": done,
                "total_count": total,
                "status": state,
            }
        )
    return result


@router.get("/rankings", response_model=RankingResponse)
def get_rankings(
    type: str = Query("all", pattern="^(all|region|friend)$"),
    subject: str = Depends(get_current_user_email),
    db: Session = Depends(get_db),
):
    user = _get_user(db, subject)
    query = db.query(AppUser).filter(AppUser.account_status == "ACTIVE")
    if type == "region":
        if not user.district_name:
            ranked_users = []
        else:
            ranked_users = query.filter(
                AppUser.district_name == user.district_name
            ).all()
    elif type == "friend":
        ranked_users = []
    else:
        ranked_users = query.all()

    ranked_users.sort(key=lambda item: (-item.total_points, item.user_code))
    all_users = query.all()
    all_users.sort(key=lambda item: (-item.total_points, item.user_code))
    my_rank = next(
        (index for index, item in enumerate(all_users, 1) if item.user_code == user.user_code),
        len(all_users) + 1,
    )
    top_percent = max(1, math.ceil(my_rank / max(len(all_users), 1) * 100))
    return {
        "myRank": {
            "rank": my_rank,
            "topPercent": top_percent,
            "point": user.total_points,
        },
        "rankings": [
            {
                "rank": index,
                "userId": item.user_code,
                "name": item.nickname,
                "score": item.total_points,
            }
            for index, item in enumerate(ranked_users, 1)
        ],
    }


@router.post("/uploads", response_model=UploadResponse, status_code=201)
async def upload_image(
    request: Request,
    file: UploadFile = File(...),
    _: str = Depends(get_current_user_email),
):
    allowed = {"image/jpeg": ".jpg", "image/jpg": ".jpg"}
    if file.content_type not in allowed:
        raise HTTPException(status_code=400, detail="JPG 이미지만 업로드할 수 있습니다.")
    content = await file.read(MAX_UPLOAD_BYTES + 1)
    if len(content) > MAX_UPLOAD_BYTES:
        raise HTTPException(status_code=400, detail="이미지 크기는 5MB 이하여야 합니다.")

    UPLOAD_DIR.mkdir(parents=True, exist_ok=True)
    filename = f"{uuid.uuid4().hex}{allowed[file.content_type]}"
    (UPLOAD_DIR / filename).write_bytes(content)
    return {"url": str(request.base_url).rstrip("/") + f"/uploads/{filename}"}
