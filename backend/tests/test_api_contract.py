import os
import sys
from pathlib import Path

os.environ["DATABASE_URL"] = "sqlite:///:memory:"
os.environ["JWT_SECRET_KEY"] = "test-secret"

APP_DIR = Path(__file__).resolve().parents[1] / "app"
TEST_DEPS = Path(__file__).resolve().parents[1] / ".testdeps"
sys.path.insert(0, str(TEST_DEPS))
sys.path.insert(0, str(APP_DIR))

from auth_utils import get_current_user_email
from database import Base, SessionLocal, engine
from fastapi import HTTPException
from models import District, Mission
from routers.api_v1 import (
    BUSAN_DISTRICTS,
    MissionVerifyRequestDto,
    SignupRequest,
    get_district_progress,
    get_missions,
    get_my_profile,
    get_rankings,
    signup,
    verify_mission,
)


def _seed_minimum():
    Base.metadata.create_all(bind=engine)
    db = SessionLocal()
    try:
        db.add_all([District(name=name) for name in BUSAN_DISTRICTS])
        db.add(
            Mission(
                mission_id=1,
                title="테스트 위치 미션",
                district_name="중구",
                location="중구 테스트동",
                latitude=35.1,
                longitude=129.03,
                radius_m=300,
                reward_points=100,
                mission_type="CURRENT_LOCATION",
            )
        )
        db.commit()
    finally:
        db.close()


def test_frontend_contract():
    _seed_minimum()
    try:
        get_current_user_email(None)
        raise AssertionError("토큰 없는 요청은 401이어야 합니다.")
    except HTTPException as exc:
        assert exc.status_code == 401

    db = SessionLocal()
    try:
        signup_response = signup(
            SignupRequest(email="tester@example.com", password="testpass123"), db
        )
        subject = get_current_user_email(f"Bearer {signup_response['token']}")

        rankings = get_rankings("all", subject, db)
        assert set(rankings) == {"myRank", "rankings"}

        progress = get_district_progress(subject, db)
        assert len(progress) == 16
        assert {item["status"] for item in progress} <= {
            "empty",
            "ongoing",
            "cleared",
        }

        missions = get_missions(subject, db)
        assert missions[0]["district"] == "중구"

        verify = verify_mission(
            MissionVerifyRequestDto(
                mission_id=1,
                mission_type="CURRENT_LOCATION",
                latitude=35.1,
                longitude=129.03,
            ),
            subject,
            db,
        )
        assert verify["success"] is True

        duplicate = verify_mission(
            MissionVerifyRequestDto(
                mission_id=1,
                mission_type="CURRENT_LOCATION",
                latitude=35.1,
                longitude=129.03,
            ),
            subject,
            db,
        )
        assert duplicate["success"] is False

        profile = get_my_profile(subject, db)
        assert profile["points"] == "100P"
    finally:
        db.close()


if __name__ == "__main__":
    test_frontend_contract()
    print("API contract test passed")
