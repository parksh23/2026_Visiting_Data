import json
import hmac
import math
import os
import re
import uuid
import secrets
import smtplib
import string
import threading
import time
from datetime import UTC, datetime
from email.message import EmailMessage
from io import BytesIO
from pathlib import Path
from typing import List, Literal, Optional
from urllib.parse import urlparse

import httpx
from fastapi import (
    APIRouter,
    Depends,
    File,
    Header,
    HTTPException,
    Query,
    Request,
    UploadFile,
    status,
)
from PIL import Image, UnidentifiedImageError
from google import genai
from google.genai import types as genai_types
from pydantic import BaseModel, Field
from sqlalchemy import func, or_
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from auth_utils import (
    create_access_token,
    get_current_user_email,
    hash_password,
    verify_password,
)
from database import get_db
from models import (
    AppUser,
    District,
    Friendship,
    Mission,
    SavedMission,
    UserAgreement,
    UserMission,
    utc_now,
)
from tourism_scoring import refresh_tourism_scores

GEMINI_API_KEY = os.getenv("GEMINI_API_KEY")
gemini_client = genai.Client(api_key=GEMINI_API_KEY) if GEMINI_API_KEY else None

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
MISSION_TYPE_ALIASES = {
    "PHOTO": "PHOTO",
    "IMAGE": "PHOTO",
    "IMAGE_LOCATION": "PHOTO",
    "PHOTO_LOCATION": "PHOTO",
    "CURRENT_LOCATION": "CURRENT_LOCATION",
    "LOCATION": "CURRENT_LOCATION",
    "GPS": "CURRENT_LOCATION",
    "GPS_LOCATION": "CURRENT_LOCATION",
    "RECEIPT": "RECEIPT",
    "RECEIPT_IMAGE": "RECEIPT",
}

UPLOAD_DIR = Path(__file__).resolve().parents[2] / "uploads"
MAX_UPLOAD_BYTES = int(os.getenv("MAX_UPLOAD_BYTES", str(5 * 1024 * 1024)))
PASSWORD_RESET_COOLDOWN_SECONDS = 5 * 60
_password_reset_attempts: dict[str, float] = {}
_password_reset_lock = threading.Lock()


class LoginRequest(BaseModel):
    email: str = Field(min_length=3, max_length=320)
    password: str = Field(min_length=1, max_length=100)

class AgreementRequest(BaseModel):
    doc: Literal["terms", "privacy", "location"]
    version: str = Field(min_length=1, max_length=20)
    agreed: bool
    agreed_at: datetime


class SignupRequest(BaseModel):
    email: str = Field(min_length=3, max_length=320)
    password: str = Field(min_length=8, max_length=100)
    nickname: str = Field(min_length=1, max_length=50)
    agreements: List[AgreementRequest] = Field(default_factory=list, max_length=3)

class KakaoLoginRequest(BaseModel):
    access_token: str
    agreements: List[AgreementRequest] = Field(default_factory=list, max_length=3)

class FindIdRequest(BaseModel):
    nickname: str

class FindPasswordRequest(BaseModel):
    email: str = Field(min_length=3, max_length=320)

class ChangePasswordRequest(BaseModel):
    old_password: str
    new_password: str

class TokenResponse(BaseModel):
    access_token: str
    token_type: str = "bearer"
    token: Optional[str] = None

class UserProfile(BaseModel):
    name: str
    points: str
    completed_missions: int
    saved_missions: int

class NicknameUpdateRequest(BaseModel):
    nickname: str

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
    target_text: Optional[str] = ""
    is_saved: bool = False

class MissionSavedResponse(BaseModel):
    mission_id: int
    is_saved: bool

class MissionProgressResponse(BaseModel):
    mission_id: int
    status: Literal["not_started", "in_progress", "completed"]

class MissionVerifyRequestDto(BaseModel):
    mission_id: int
    mission_type: str = Field(min_length=1, max_length=50)
    photo_url: Optional[str] = Field(default=None, max_length=1000)
    latitude: Optional[float] = Field(default=None, ge=-90, le=90)
    longitude: Optional[float] = Field(default=None, ge=-180, le=180)
    location_accuracy_m: Optional[float] = Field(default=None, ge=0, le=10_000)
    receipt_image_url: Optional[str] = Field(default=None, max_length=1000)

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
    userId: str

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

class TourismRefreshRequest(BaseModel):
    base_ym: Optional[str] = None


def _next_user_code(db: Session) -> str:
    # 순차 번호 계산은 동시 가입 시 충돌할 수 있으므로 짧은 UUID를 사용한다.
    while True:
        candidate = f"U{uuid.uuid4().hex[:12].upper()}"
        exists = db.query(AppUser.user_code).filter(AppUser.user_code == candidate).first()
        if not exists:
            return candidate


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


def _profile_dict(user: AppUser, db: Session) -> dict:
    saved_count = (
        db.query(func.count(SavedMission.mission_id))
        .filter(SavedMission.user_code == user.user_code)
        .scalar()
        or 0
    )
    return {
        "name": user.nickname,
        "points": f"{user.total_points:,}P",
        "completed_missions": user.completed_missions,
        "saved_missions": int(saved_count),
    }


def _validate_nickname(nickname: str) -> None:
    if not 2 <= len(nickname) <= 12:
        raise HTTPException(
            status_code=400, detail="닉네임은 2자 이상 12자 이하로 입력해주세요."
        )
    if any(character.isspace() for character in nickname):
        raise HTTPException(status_code=400, detail="닉네임에는 공백을 사용할 수 없습니다.")


def _available_social_nickname(db: Session, desired: str, kakao_id: str) -> str:
    normalized = "".join(desired.split()).strip() or "카카오사용자"
    normalized = normalized[:12]
    if not db.query(AppUser.user_code).filter(AppUser.nickname == normalized).first():
        return normalized
    suffix = kakao_id[-4:] or secrets.token_hex(2)
    candidate = f"{normalized[:7]}{suffix}"[:12]
    if not db.query(AppUser.user_code).filter(AppUser.nickname == candidate).first():
        return candidate
    return f"카카오{secrets.token_hex(3)}"[:12]


def _validated_agreements(agreements: List[AgreementRequest]) -> List[AgreementRequest]:
    accepted = {agreement.doc for agreement in agreements if agreement.agreed}
    missing = {"terms", "privacy", "location"} - accepted
    if missing:
        raise HTTPException(status_code=400, detail="필수 약관에 모두 동의해 주세요.")
    if len(accepted) != len(agreements):
        raise HTTPException(status_code=400, detail="약관 동의 정보가 올바르지 않습니다.")
    return agreements


def _store_agreements(
    db: Session, user_code: str, agreements: List[AgreementRequest]
) -> None:
    for agreement in _validated_agreements(agreements):
        agreed_at = agreement.agreed_at
        if agreed_at.tzinfo is not None:
            agreed_at = agreed_at.astimezone(UTC).replace(tzinfo=None)
        db.add(
            UserAgreement(
                user_code=user_code,
                document_slug=agreement.doc,
                version=agreement.version,
                agreed_at=agreed_at,
            )
        )


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


def _normalize_mission_type(value: Optional[str]) -> Optional[str]:
    """운영 DB의 구형 표기와 현재 API 표기를 하나의 인증 타입으로 통일한다."""
    if not value:
        return None
    return MISSION_TYPE_ALIASES.get(value.strip().upper())


def _mission_dict(
    mission: Mission,
    status_by_id: dict[int, str],
    saved_ids: Optional[set[int]] = None,
) -> dict:
    mission_status = status_by_id.get(mission.mission_id, "not_started")
    completed = mission_status == "completed"
    return {
        "mission_id": mission.mission_id,
        "title": getattr(mission, "title", ""),
        "district": getattr(mission, "district_name", ""),
        "location": getattr(mission, "location", ""),
        "latitude": mission.latitude,
        "longitude": mission.longitude,
        "reward_points": getattr(mission, "reward_points", 0),
        "progress_current": 1 if completed else 0,
        "progress_total": 1,
        "status": mission_status if mission_status in {"in_progress", "completed"} else "not_started",
        # 앱에는 항상 현재 API의 표준 타입을 내려준다. 알 수 없는 값만 원문을 유지해
        # 잘못된 운영 데이터를 화면 로딩 단계에서 확인할 수 있게 한다.
        "mission_type": _normalize_mission_type(mission.mission_type)
        or mission.mission_type,
        "image_url": getattr(mission, "image_url", None),
        "is_saved": mission.mission_id in (saved_ids or set()),
    }


def _uploaded_image_exists(image_url: Optional[str]) -> bool:
    if not image_url:
        return False
    parsed = urlparse(image_url)
    if parsed.scheme not in {"http", "https"} or not parsed.netloc:
        return False
    if not parsed.path.startswith("/uploads/"):
        return False
    filename = Path(parsed.path).name
    if not re.fullmatch(r"[0-9a-f]{32}\.jpg", filename):
        return False
    return (UPLOAD_DIR / filename).is_file()


def _send_temporary_password(recipient: str, temporary_password: str) -> None:
    host = os.getenv("SMTP_HOST")
    username = os.getenv("SMTP_USERNAME")
    password = os.getenv("SMTP_PASSWORD")
    sender = os.getenv("SMTP_FROM") or username
    if not all((host, username, password, sender)):
        raise RuntimeError("비밀번호 복구 메일 설정이 완료되지 않았습니다.")

    message = EmailMessage()
    message["Subject"] = "[Busan Quest] 임시 비밀번호 안내"
    message["From"] = sender
    message["To"] = recipient
    message.set_content(
        "Busan Quest 임시 비밀번호입니다.\n\n"
        f"{temporary_password}\n\n"
        "로그인 후 내 정보 > 계정 설정에서 비밀번호를 변경해 주세요.\n"
        "본인이 요청하지 않았다면 고객지원으로 문의해 주세요."
    )

    port = int(os.getenv("SMTP_PORT", "587"))
    with smtplib.SMTP(host, port, timeout=10) as smtp:
        smtp.starttls()
        smtp.login(username, password)
        smtp.send_message(message)


@router.post("/auth/login", response_model=TokenResponse)
def login(req: LoginRequest, db: Session = Depends(get_db)):
    user = db.query(AppUser).filter(AppUser.email == req.email.strip().lower()).first()
    password_too_long = len(req.password.encode("utf-8")) > 72
    if password_too_long or user is None or not verify_password(req.password, user.password_hash):
        raise HTTPException(status_code=401, detail="이메일 또는 비밀번호가 올바르지 않습니다.")
    if user.account_status != "ACTIVE":
        raise HTTPException(status_code=403, detail="사용할 수 없는 계정입니다.")

    token = _token_for(user)
    return {"access_token": token, "token_type": "bearer", "token": token}


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
    nickname = req.nickname.strip()
    if not nickname:
        raise HTTPException(status_code=400, detail="닉네임을 입력해주세요.")
    _validate_nickname(nickname)
    if db.query(AppUser).filter(AppUser.email == email).first():
        raise HTTPException(status_code=409, detail="이미 가입된 이메일입니다.")
    if db.query(AppUser).filter(AppUser.nickname == nickname).first():
        raise HTTPException(status_code=409, detail="이미 사용 중인 닉네임입니다.")

    user = AppUser(
        user_code=_next_user_code(db),
        login_id=email,
        email=email,
        password_hash=hash_password(req.password),
        nickname=nickname,
        account_status="ACTIVE",
    )
    db.add(user)
    try:
        db.flush()
        _store_agreements(db, user.user_code, req.agreements)
        db.commit()
    except IntegrityError as exc:
        db.rollback()
        raise HTTPException(status_code=409, detail="이미 사용 중인 계정 정보입니다.") from exc
    db.refresh(user)

    token = _token_for(user)
    return {"access_token": token, "token_type": "bearer", "token": token}


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
            nickname=_available_social_nickname(db, nickname, kakao_id),
            account_status="ACTIVE",
        )
        db.add(user)
        db.flush()
        _store_agreements(db, user.user_code, req.agreements)
    elif not user.kakao_id:
        user.kakao_id = kakao_id
    try:
        db.commit()
    except IntegrityError as exc:
        db.rollback()
        raise HTTPException(status_code=409, detail="카카오 계정을 연결할 수 없습니다.") from exc
    db.refresh(user)

    token = _token_for(user)
    return {"access_token": token, "token_type": "bearer", "token": token}


# --- 아이디/비밀번호 찾기 ---
@router.post("/auth/find-id")
def find_id(req: FindIdRequest, db: Session = Depends(get_db)):
    user = db.query(AppUser).filter(AppUser.nickname == req.nickname.strip()).first()
    if not user or not user.email:
        raise HTTPException(status_code=404, detail="해당 닉네임으로 가입된 계정을 찾을 수 없습니다.")

    email_parts = user.email.split("@")
    if len(email_parts) == 2:
        id_part, domain = email_parts
        if len(id_part) > 2:
            masked_id = id_part[:2] + "*" * (len(id_part) - 2)
        else:
            masked_id = id_part[:1] + "*"
        masked_email = f"{masked_id}@{domain}"
    else:
        masked_email = user.email

    return {"message": "아이디를 찾았습니다.", "masked_email": masked_email}


@router.post("/auth/find-password")
def find_password(req: FindPasswordRequest, db: Session = Depends(get_db)):
    email = req.email.strip().lower()
    now = time.monotonic()
    with _password_reset_lock:
        if len(_password_reset_attempts) > 1_000:
            cutoff = now - PASSWORD_RESET_COOLDOWN_SECONDS
            stale = [key for key, attempted_at in _password_reset_attempts.items() if attempted_at < cutoff]
            for key in stale:
                _password_reset_attempts.pop(key, None)
        previous = _password_reset_attempts.get(email, 0.0)
        if now - previous < PASSWORD_RESET_COOLDOWN_SECONDS:
            return {
                "success": True,
                "message": "가입된 계정이라면 입력한 이메일로 임시 비밀번호를 보냈습니다.",
            }
        _password_reset_attempts[email] = now

    user = db.query(AppUser).filter(AppUser.email == email, AppUser.account_status == "ACTIVE").first()
    # 계정 존재 여부가 응답으로 노출되지 않도록 항상 같은 성공 문구를 사용한다.
    if user and user.password_hash:
        chars = string.ascii_letters + string.digits + "!@#"
        temporary_password = "".join(secrets.choice(chars) for _ in range(12))
        try:
            _send_temporary_password(email, temporary_password)
        except (OSError, smtplib.SMTPException, RuntimeError) as exc:
            with _password_reset_lock:
                _password_reset_attempts.pop(email, None)
            raise HTTPException(
                status_code=503,
                detail="현재 비밀번호 복구 메일을 보낼 수 없습니다. 잠시 후 다시 시도해주세요.",
            ) from exc
        user.password_hash = hash_password(temporary_password)
        db.commit()

    return {
        "success": True,
        "message": "가입된 계정이라면 입력한 이메일로 임시 비밀번호를 보냈습니다.",
    }
# ---------------------------------------------


# --- 로그아웃 ---
@router.post("/auth/logout")
def logout(subject: str = Depends(get_current_user_email)):
    return {"success": True, "message": "성공적으로 로그아웃 되었습니다."}
# ---------------------------------


@router.get("/users/me", response_model=UserProfile)
def get_my_profile(
    subject: str = Depends(get_current_user_email), db: Session = Depends(get_db)
):
    user = _get_user(db, subject)
    return _profile_dict(user, db)


@router.patch("/users/me/nickname", response_model=UserProfile)
def update_my_nickname(
    req: NicknameUpdateRequest,
    subject: str = Depends(get_current_user_email),
    db: Session = Depends(get_db),
):
    user = _get_user(db, subject)
    nickname = req.nickname
    _validate_nickname(nickname)
    if nickname == user.nickname:
        raise HTTPException(status_code=400, detail="현재 닉네임과 동일합니다.")
    duplicate = (
        db.query(AppUser.user_code)
        .filter(
            AppUser.nickname == nickname,
            AppUser.user_code != user.user_code,
        )
        .first()
    )
    if duplicate:
        raise HTTPException(status_code=409, detail="이미 사용 중인 닉네임입니다.")

    user.nickname = nickname
    try:
        db.commit()
    except IntegrityError as exc:
        db.rollback()
        raise HTTPException(
            status_code=409, detail="이미 사용 중인 닉네임입니다."
        ) from exc
    db.refresh(user)
    return _profile_dict(user, db)


# --- 비밀번호 변경 및 회원탈퇴 ---
@router.patch("/users/me/password")
def change_password(
    req: ChangePasswordRequest,
    subject: str = Depends(get_current_user_email),
    db: Session = Depends(get_db)
):
    user = _get_user(db, subject)

    if not verify_password(req.old_password, user.password_hash):
        raise HTTPException(status_code=400, detail="현재 비밀번호가 일치하지 않습니다.")

    password_bytes = req.new_password.encode("utf-8")
    if len(req.new_password) < 8 or len(password_bytes) > 72:
        raise HTTPException(
            status_code=400,
            detail="새 비밀번호는 8자 이상, 72바이트 이하여야 합니다.",
        )

    user.password_hash = hash_password(req.new_password)
    db.commit()

    return {"success": True, "message": "비밀번호가 성공적으로 변경되었습니다."}


@router.delete("/users/me")
def withdraw_account(
    subject: str = Depends(get_current_user_email),
    db: Session = Depends(get_db)
):
    user = _get_user(db, subject)

    # 1. 외래키 오류 방지를 위해 연관 데이터 삭제 (Hard Delete)
    db.query(UserMission).filter(UserMission.user_code == user.user_code).delete()
    db.query(SavedMission).filter(SavedMission.user_code == user.user_code).delete()
    db.query(UserAgreement).filter(UserAgreement.user_code == user.user_code).delete()
    db.query(Friendship).filter(
        or_(Friendship.user_code == user.user_code, Friendship.friend_user_code == user.user_code)
    ).delete()

    # 2. 유저 정보 실제 삭제
    db.delete(user)
    db.commit()

    return {"success": True, "message": "회원 탈퇴가 완료되었습니다."}
# --------------------------------------------------


@router.get("/missions", response_model=List[MissionDto])
def get_missions(
    subject: str = Depends(get_current_user_email), db: Session = Depends(get_db)
):
    user = _get_user(db, subject)
    status_by_id = dict(
        db.query(UserMission.mission_id, UserMission.status)
        .filter(UserMission.user_code == user.user_code)
        .all()
    )
    saved_ids = {
        mission_id
        for (mission_id,) in db.query(SavedMission.mission_id)
        .filter(SavedMission.user_code == user.user_code)
        .all()
    }

    return [
        _mission_dict(mission, status_by_id, saved_ids)
        for mission in db.query(Mission).order_by(Mission.mission_id).all()
    ]


@router.get("/missions/saved", response_model=List[MissionDto])
def get_saved_missions(
    subject: str = Depends(get_current_user_email), db: Session = Depends(get_db)
):
    user = _get_user(db, subject)
    status_by_id = dict(
        db.query(UserMission.mission_id, UserMission.status)
        .filter(UserMission.user_code == user.user_code)
        .all()
    )

    saved_ids_list = [
        mission_id
        for (mission_id,) in db.query(SavedMission.mission_id)
        .filter(SavedMission.user_code == user.user_code)
        .order_by(SavedMission.created_at.desc(), SavedMission.mission_id.desc())
        .all()
    ]
    saved_ids = set(saved_ids_list)

    if not saved_ids_list:
        return []

    missions_db = (
        db.query(Mission)
        .filter(Mission.mission_id.in_(saved_ids_list))
        .all()
    )

    mission_map = {m.mission_id: m for m in missions_db}
    missions = [mission_map[mid] for mid in saved_ids_list if mid in mission_map]

    return [_mission_dict(mission, status_by_id, saved_ids) for mission in missions]


@router.post(
    "/missions/{mission_id}/start",
    response_model=MissionProgressResponse,
)
def start_mission(
    mission_id: int,
    subject: str = Depends(get_current_user_email),
    db: Session = Depends(get_db),
):
    user = _get_user(db, subject)
    if db.query(Mission.mission_id).filter(Mission.mission_id == mission_id).first() is None:
        raise HTTPException(status_code=404, detail="미션을 찾을 수 없습니다.")

    progress = (
        db.query(UserMission)
        .filter(
            UserMission.user_code == user.user_code,
            UserMission.mission_id == mission_id,
        )
        .first()
    )
    if progress and progress.status == "completed":
        raise HTTPException(status_code=409, detail="이미 완료한 미션입니다.")
    if progress is None:
        db.add(
            UserMission(
                user_code=user.user_code,
                mission_id=mission_id,
                status="in_progress",
            )
        )
    else:
        progress.status = "in_progress"
    db.commit()
    return {"mission_id": mission_id, "status": "in_progress"}


@router.delete(
    "/missions/{mission_id}/start",
    response_model=MissionProgressResponse,
)
def cancel_mission(
    mission_id: int,
    subject: str = Depends(get_current_user_email),
    db: Session = Depends(get_db),
):
    user = _get_user(db, subject)
    progress = (
        db.query(UserMission)
        .filter(
            UserMission.user_code == user.user_code,
            UserMission.mission_id == mission_id,
        )
        .first()
    )
    if progress and progress.status == "completed":
        raise HTTPException(status_code=409, detail="완료한 미션은 취소할 수 없습니다.")
    if progress:
        db.delete(progress)
        db.commit()
    return {"mission_id": mission_id, "status": "not_started"}


@router.post(
    "/missions/{mission_id}/saved",
    response_model=MissionSavedResponse,
    status_code=201,
)
def save_mission(
    mission_id: int,
    subject: str = Depends(get_current_user_email),
    db: Session = Depends(get_db),
):
    user = _get_user(db, subject)
    if db.query(Mission.mission_id).filter(Mission.mission_id == mission_id).first() is None:
        raise HTTPException(status_code=404, detail="미션을 찾을 수 없습니다.")

    existing = (
        db.query(SavedMission.mission_id)
        .filter(
            SavedMission.user_code == user.user_code,
            SavedMission.mission_id == mission_id,
        )
        .first()
    )
    if not existing:
        db.add(SavedMission(user_code=user.user_code, mission_id=mission_id))
        try:
            db.commit()
        except IntegrityError:
            db.rollback()

    return {"mission_id": mission_id, "is_saved": True}


@router.delete("/missions/{mission_id}/saved", response_model=MissionSavedResponse)
def unsave_mission(
    mission_id: int,
    subject: str = Depends(get_current_user_email),
    db: Session = Depends(get_db),
):
    user = _get_user(db, subject)

    deleted = (
        db.query(SavedMission)
        .filter(
            SavedMission.user_code == user.user_code,
            SavedMission.mission_id == mission_id,
        )
        .delete(synchronize_session=False)
    )
    if deleted:
        db.commit()

    return {"mission_id": mission_id, "is_saved": False}


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

    requested_type = _normalize_mission_type(req.mission_type)
    stored_type = _normalize_mission_type(mission.mission_type)
    if requested_type is None or stored_type is None or requested_type != stored_type:
        return {"success": False, "message": "미션 인증 방식이 올바르지 않습니다."}
    progress = (
        db.query(UserMission)
        .filter(
            UserMission.user_code == user.user_code,
            UserMission.mission_id == mission.mission_id,
        )
        .first()
    )
    if progress and progress.status == "completed":
        return {"success": False, "message": "이미 완료한 미션입니다."}
    if requested_type == "PHOTO" and not _uploaded_image_exists(req.photo_url):
        return {
            "success": False,
            "message": "서버에 업로드된 인증 사진을 확인할 수 없습니다.",
        }
    if requested_type == "RECEIPT" and not _uploaded_image_exists(
        req.receipt_image_url
    ):
        return {
            "success": False,
            "message": "서버에 업로드된 영수증 이미지를 확인할 수 없습니다.",
        }

    if requested_type in {"PHOTO", "CURRENT_LOCATION"}:
        if req.latitude is None or req.longitude is None:
            return {"success": False, "message": "현재 위치 정보가 필요합니다."}
        if mission.latitude is None or mission.longitude is None:
            return {"success": False, "message": "미션 장소 좌표가 등록되지 않았습니다."}
        if req.location_accuracy_m is not None and req.location_accuracy_m > 100:
            return {
                "success": False,
                "message": "현재 위치 정확도가 낮습니다. 야외에서 잠시 기다린 뒤 다시 시도해주세요.",
            }
        distance = _haversine_m(
            req.latitude, req.longitude, mission.latitude, mission.longitude
        )

        mission_radius = getattr(mission, "radius_m", 300)

        accuracy_margin = min(req.location_accuracy_m or 0, 50)
        if max(0, distance - accuracy_margin) > mission_radius:
            return {
                "success": False,
                "message": f"미션 장소에서 허용 반경 {mission_radius}m 이상 떨어져 있어요.",
            }

    ai_extracted_text = ""
    if requested_type in {"PHOTO", "RECEIPT"}:
        target_url = req.photo_url if requested_type == "PHOTO" else req.receipt_image_url
        filename = Path(urlparse(target_url).path).name
        local_image_path = UPLOAD_DIR / filename
        if gemini_client is None:
            local_image_path.unlink(missing_ok=True)
            raise HTTPException(status_code=503, detail="사진 인증 기능이 설정되지 않았습니다.")

        try:
            mission_title = getattr(mission, "title", "알 수 없는 미션")

            if requested_type == "RECEIPT":
                prompt = f"""
                당신은 영수증 인증 심사관입니다.
                유저가 수행한 미션의 제목은 '{mission_title}' 입니다.
                제출된 이미지가 영수증이 맞는지, 그리고 영수증의 상호명이나 결제 내역이 미션 제목('{mission_title}')을 성공적으로 완수했음을 증명하는지 확인하세요.
                반드시 아래 JSON 형식으로만 대답하세요.
                {{"is_success": true/false, "extracted_text": "승인/거절 이유와 결제 금액 등 핵심 정보 1줄 요약"}}
                """
            else:
                prompt = f"""
                당신은 미션 인증 심사관입니다.
                유저가 수행한 미션의 제목은 '{mission_title}' 입니다.
                제출된 사진이 미션 제목('{mission_title}')이 요구하는 장소, 사물, 혹은 상황을 명확하게 보여주고 미션을 성공적으로 완수했는지 객관적으로 판별하세요.
                반드시 아래 JSON 형식으로만 대답하세요.
                {{"is_success": true/false, "extracted_text": "판별 성공/실패 이유 1줄 요약"}}
                """

            response = gemini_client.models.generate_content(
                model="gemini-2.5-flash-lite",
                contents=[
                    prompt,
                    genai_types.Part.from_bytes(
                        data=local_image_path.read_bytes(),
                        mime_type="image/jpeg",
                    ),
                ],
                config=genai_types.GenerateContentConfig(
                    response_mime_type="application/json",
                    temperature=0.1
                ),
            )

            result_data = json.loads(response.text)
            is_success = result_data.get("is_success") is True
            ai_extracted_text = result_data.get("extracted_text", "")

            if not is_success:
                return {
                    "success": False,
                    "message": ai_extracted_text if ai_extracted_text else "사진이 미션 조건과 일치하지 않습니다."
                }

        except json.JSONDecodeError:
            raise HTTPException(status_code=502, detail="AI 응답을 해석할 수 없습니다.")
        except Exception as exc:
            raise HTTPException(status_code=502, detail="AI 사진 분석에 실패했습니다.") from exc
        finally:
            local_image_path.unlink(missing_ok=True)

    reward = getattr(mission, "reward_points", 0)
    if progress is None:
        db.add(
            UserMission(
                user_code=user.user_code,
                mission_id=mission.mission_id,
                status="completed",
            )
        )
    else:
        progress.status = "completed"
        progress.verified_at = utc_now()

    user.total_points += reward
    user.completed_missions += 1
    try:
        db.commit()
    except IntegrityError:
        db.rollback()
        return {"success": False, "message": "이미 완료한 미션입니다."}
    return {
        "success": True,
        "message": f"미션 인증이 완료되어 {reward}P가 적립됐습니다.",
    }


@router.post("/admin/tourism-scores/refresh")
def refresh_tourism_scores_endpoint(
    req: TourismRefreshRequest,
    x_admin_key: Optional[str] = Header(default=None, alias="X-Admin-Key"),
    db: Session = Depends(get_db),
):
    configured_key = os.getenv("TOURISM_ADMIN_KEY")
    if not configured_key:
        raise HTTPException(
            status_code=503,
            detail="관광지수 관리자 API가 설정되지 않았습니다.",
        )
    if not x_admin_key or not hmac.compare_digest(x_admin_key, configured_key):
        raise HTTPException(status_code=403, detail="관리자 인증에 실패했습니다.")
    try:
        return refresh_tourism_scores(db, req.base_ym)
    except (RuntimeError, ValueError) as exc:
        db.rollback()
        raise HTTPException(status_code=400, detail=str(exc)) from exc
    except Exception as exc:
        db.rollback()
        raise HTTPException(
            status_code=502,
            detail="관광지수 갱신에 실패해 기존 점수를 유지합니다.",
        ) from exc


@router.get("/districts/progress", response_model=List[DistrictStatusDto])
def get_district_progress(
    subject: str = Depends(get_current_user_email), db: Session = Depends(get_db)
):
    user = _get_user(db, subject)
    totals = dict(
        db.query(getattr(Mission, "district_name", Mission.mission_id), func.count(Mission.mission_id))
        .group_by(getattr(Mission, "district_name", Mission.mission_id))
        .all()
    )
    completed = dict(
        db.query(getattr(Mission, "district_name", Mission.mission_id), func.count(UserMission.id))
        .join(UserMission, UserMission.mission_id == Mission.mission_id)
        .filter(
            UserMission.user_code == user.user_code,
            UserMission.status == "completed",
        )
        .group_by(getattr(Mission, "district_name", Mission.mission_id))
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
    district: Optional[str] = Query(None, description="지역 랭킹 조회 시 타겟 지역구"),
    subject: str = Depends(get_current_user_email),
    db: Session = Depends(get_db),
):
    user = _get_user(db, subject)
    query = db.query(AppUser).filter(AppUser.account_status == "ACTIVE")

    def _apply_tie_ranks(data_list: List[dict]) -> List[dict]:
        current_rank = 1
        for i, item in enumerate(data_list):
            if i > 0 and item["score"] < data_list[i - 1]["score"]:
                current_rank = i + 1
            item["rank"] = current_rank
        return data_list

    if type == "region":
        target_district = district or user.district_name
        if not target_district:
            return {
                "myRank": {
                    "rank": 0,
                    "topPercent": 0,
                    "point": 0,
                    "userId": user.user_code,
                },
                "rankings": [],
            }

        mission_counts = (
            db.query(
                UserMission.user_code,
                func.count(UserMission.mission_id).label("mission_count")
            )
            .join(Mission, UserMission.mission_id == Mission.mission_id)
            .filter(
                UserMission.status == "completed",
                Mission.district_name == target_district
            )
            .group_by(UserMission.user_code)
            .subquery()
        )

        users_with_counts = (
            db.query(AppUser, func.coalesce(mission_counts.c.mission_count, 0).label("count"))
            .outerjoin(mission_counts, AppUser.user_code == mission_counts.c.user_code)
            .filter(AppUser.account_status == "ACTIVE")
            .all()
        )

        raw_data = [
            {
                "userId": u.user_code,
                "name": u.nickname,
                "score": count,
                "_tie_score": u.total_points
            }
            for u, count in users_with_counts
        ]
        raw_data.sort(key=lambda x: (-x["score"], -x["_tie_score"], x["userId"]))

        ranked_data = _apply_tie_ranks(raw_data)
        all_ranked_data = ranked_data

    else:
        if type == "friend":
            friend_codes = [
                f_code
                for (f_code,) in db.query(Friendship.friend_user_code)
                .filter(Friendship.user_code == user.user_code)
                .all()
            ]
            ranked_users = query.filter(AppUser.user_code.in_([user.user_code, *friend_codes])).all()
        else:
            ranked_users = query.all()

        raw_data = [
            {"userId": u.user_code, "name": u.nickname, "score": u.total_points}
            for u in ranked_users
        ]
        raw_data.sort(key=lambda x: (-x["score"], x["userId"]))

        ranked_data = _apply_tie_ranks(raw_data)

        all_users = query.all()
        all_raw_data = [
            {"userId": u.user_code, "name": u.nickname, "score": u.total_points}
            for u in all_users
        ]
        all_raw_data.sort(key=lambda x: (-x["score"], x["userId"]))

        all_ranked_data = _apply_tie_ranks(all_raw_data)

    my_item = next((r for r in all_ranked_data if r["userId"] == user.user_code), None)
    my_rank = my_item["rank"] if my_item else len(all_ranked_data) + 1
    my_score = my_item["score"] if my_item else 0
    top_percent = max(1, math.ceil(my_rank / max(len(all_ranked_data), 1) * 100))

    final_rankings = [
        {
            "rank": r["rank"],
            "userId": r["userId"],
            "name": r["name"],
            "score": r["score"],
        }
        for r in ranked_data
    ]

    return {
        "myRank": {
            "rank": my_rank,
            "topPercent": top_percent,
            "point": my_score,
            "userId": user.user_code,
        },
        "rankings": final_rankings
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
    if not content:
        raise HTTPException(status_code=400, detail="빈 파일은 업로드할 수 없습니다.")

    try:
        with Image.open(BytesIO(content)) as image:
            image.verify()
            if image.format != "JPEG":
                raise ValueError("not jpeg")
    except (UnidentifiedImageError, OSError, ValueError) as exc:
        raise HTTPException(status_code=400, detail="올바른 JPG 이미지가 아닙니다.") from exc

    UPLOAD_DIR.mkdir(parents=True, exist_ok=True)
    filename = f"{uuid.uuid4().hex}{allowed[file.content_type]}"
    (UPLOAD_DIR / filename).write_bytes(content)
    return {"url": str(request.base_url).rstrip("/") + f"/uploads/{filename}"}
