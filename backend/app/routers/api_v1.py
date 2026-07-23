import os
import io
import json
from PIL import Image
import google.generativeai as genai

from fastapi import APIRouter, HTTPException, status, Query, Depends, UploadFile, File, Form
from pydantic import BaseModel
from typing import List, Optional

from sqlalchemy.orm import Session
from sqlalchemy import func

from database import get_db
# 리더님의 프로젝트 구조에 맞게 임포트 (AppUser, Mission, UserMission 모두 필요)
from models import AppUser, Mission, UserMission
from auth_utils import (
    create_access_token,
    get_current_user_email,
    hash_password,
    verify_password
)

# =========================================================
# 0. Gemini API 초기화 (Render 환경변수 사용)
# =========================================================
# Render 대시보드의 [Environment] 탭에서 GEMINI_API_KEY 값을 설정해주시면 됩니다.
GEMINI_API_KEY = os.getenv("GEMINI_API_KEY", "여기에_발급받은_API_KEY_임시입력")
genai.configure(api_key=GEMINI_API_KEY)
# 구글의 최신 무료/초경량/고속 모델 적용
model = genai.GenerativeModel('gemini-2.5-flash-lite')

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
def login(req: LoginRequest, db: Session = Depends(get_db)):
    user = db.query(AppUser).filter(AppUser.email == req.email).first()

    if user is None:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="이메일 또는 비밀번호가 올바르지 않습니다."
        )

    if user.account_status != "ACTIVE":
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="사용할 수 없는 계정입니다."
        )

    if not verify_password(req.password, user.password_hash):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="이메일 또는 비밀번호가 올바르지 않습니다."
        )

    access_token = create_access_token(data={"sub": user.email})
    return {"token": access_token}


@router.post("/auth/signup", response_model=TokenResponse, status_code=status.HTTP_201_CREATED)
def signup(req: SignupRequest, db: Session = Depends(get_db)):
    if "@" not in req.email:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="이메일 형식이 올바르지 않습니다."
        )

    if len(req.password) < 8:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="비밀번호는 8자 이상이어야 합니다."
        )

    if len(req.password.encode("utf-8")) > 72:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="비밀번호는 72바이트 이하로 입력해주세요."
        )

    existing_user = db.query(AppUser).filter(AppUser.email == req.email).first()
    if existing_user is not None:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="이미 가입된 이메일입니다."
        )

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

    access_token = create_access_token(data={"sub": new_user.email})
    return {"token": access_token}


@router.post("/auth/kakao", response_model=TokenResponse)
def kakao_login(req: KakaoLoginRequest):
    kakao_user_email = "kakao_user@example.com"
    access_token = create_access_token(data={"sub": kakao_user_email})
    return {"token": access_token}

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

# =========================================================
# 8. Mission API
# =========================================================
@router.get("/missions", response_model=List[MissionDto])
def get_missions(
    current_user_email: str = Depends(get_current_user_email),
    db: Session = Depends(get_db)
):
    user = db.query(AppUser).filter(AppUser.email == current_user_email).first()
    
    results = db.query(Mission, UserMission.status)\
                .outerjoin(UserMission, (Mission.mission_id == UserMission.mission_id) & (UserMission.user_id == user.user_code))\
                .all()

    missions_response = []
    for mission, status in results:
        missions_response.append({
            "mission_id": mission.mission_id,
            "title": mission.mission_name,
            "location": mission.region_name,
            "reward_points": mission.base_score,
            "progress_current": 1 if status == "completed" else 0,
            "progress_total": 1,
            "status": status or "locked",
            "mission_type": mission.mission_type,
            "image_url": None
        })

    return missions_response


@router.get("/missions/ongoing", response_model=List[MissionDto])
def get_ongoing_missions(
    current_user_email: str = Depends(get_current_user_email),
    db: Session = Depends(get_db)
):
    user = db.query(AppUser).filter(AppUser.email == current_user_email).first()
    
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


@router.post("/missions/verify", response_model=MissionVerifyResponse)
async def verify_mission(
    file: UploadFile = File(...),
    mission_id: int = Form(...),
    mission_type: str = Form(...),
    target_text: str = Form(""), # 프론트에서 넘어올 판별 목표 (예: "광안리 해수욕장" 또는 "돼지국밥")
    current_user_email: str = Depends(get_current_user_email),
    db: Session = Depends(get_db)
):
    try:
        # 1. 파일 읽기 (메모리상에서 바로 처리)
        image_bytes = await file.read()
        image = Image.open(io.BytesIO(image_bytes))

        # 2. 미션 타입별 Gemini 프롬프트 분기
        if mission_type == "RECEIPT":
            prompt = f"""
            이 이미지는 영수증입니다. 다음 목표 정보가 포함되어 있는지 확인하세요: '{target_text}'
            반드시 아래 JSON 형식으로만 대답하세요.
            {{"is_success": true/false, "extracted_text": "결제 금액, 상호명 등 추출된 핵심 정보"}}
            """
        else:
            prompt = f"""
            이 사진이 다음 미션 장소나 사물을 명확하게 보여주는지 판별하세요: '{target_text}'
            반드시 아래 JSON 형식으로만 대답하세요.
            {{"is_success": true/false, "extracted_text": "판별 성공/실패 이유 1줄 요약"}}
            """

        # 3. Gemini 2.5 Flash API 호출 (JSON 강제 반환 설정)
        response = model.generate_content(
            [prompt, image],
            generation_config=genai.GenerationConfig(
                response_mime_type="application/json",
                temperature=0.1
            )
        )
        
        # 4. 결과 파싱
        result_data = json.loads(response.text)
        is_success = result_data.get("is_success", False)
        extracted_text = result_data.get("extracted_text", "")

        # TODO: 인증 성공 시 DB(UserMission)의 상태를 'completed'로 업데이트하고 포인트(AppUser) 지급 로직 추가 가능

        return {
            "success": is_success,
            "message": extracted_text if extracted_text else ("미션 성공!" if is_success else "사진이 미션 조건과 일치하지 않습니다.")
        }

    except Exception as e:
        raise HTTPException(status_code=500, detail=f"AI 분석 중 오류가 발생했습니다: {str(e)}")


# =========================================================
# 9. District API
# =========================================================
@router.get("/districts/progress", response_model=List[DistrictStatusDto])
def get_district_progress(
    current_user_email: str = Depends(get_current_user_email),
    db: Session = Depends(get_db)
):
    user = db.query(AppUser).filter(AppUser.email == current_user_email).first()

    total_counts_query = db.query(Mission.region_name, func.count(Mission.mission_id))\
                           .group_by(Mission.region_name).all()
    
    completed_counts_query = db.query(Mission.region_name, func.count(Mission.mission_id))\
                               .join(UserMission, Mission.mission_id == UserMission.mission_id)\
                               .filter(UserMission.user_id == user.user_code)\
                               .filter(UserMission.status == "completed")\
                               .group_by(Mission.region_name).all()

    completed_dict = {region: count for region, count in completed_counts_query}

    response = []
    for region, total in total_counts_query:
        completed = completed_dict.get(region, 0)
        
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
    current_user_email: str = Depends(get_current_user_email),
    db: Session = Depends(get_db)
):
    # AppUser 테이블에서 total_points 기준으로 내림차순 정렬하여 전체 조회
    all_users = db.query(AppUser).order_by(AppUser.total_points.desc()).all()
    
    rankings_list = []
    my_rank_data = {"rank": 0, "topPercent": 0, "point": 0}
    total_users = len(all_users)

    # 순회하면서 순위 계산 및 리스트 생성
    for idx, user in enumerate(all_users):
        current_rank = idx + 1
        
        # 요청한 사용자 본인의 랭킹 정보 기록
        if user.email == current_user_email:
            my_rank_data = {
                "rank": current_rank,
                "topPercent": int((current_rank / total_users) * 100) if total_users > 0 else 0,
                "point": user.total_points
            }
            
        # 프론트에 내려줄 랭킹은 상위 100명까지만 제한
        if current_rank <= 100:
            rankings_list.append({
                "rank": current_rank,
                "userId": user.user_code,
                "name": user.nickname,
                "score": user.total_points
            })

    return {
        "myRank": my_rank_data,
        "rankings": rankings_list
    }
