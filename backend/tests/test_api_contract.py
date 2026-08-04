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
from fastapi.security import HTTPAuthorizationCredentials
from models import AppUser, District, Friendship, Mission, UserMission
from routers.api_v1 import (
    BUSAN_DISTRICTS,
    MissionVerifyRequestDto,
    SignupRequest,
    UPLOAD_DIR,
    get_district_progress,
    get_missions,
    get_my_profile,
    get_rankings,
    signup,
    verify_mission,
)
from tourism_scoring import (
    DistrictScore,
    INDICATORS,
    calculate_district_scores,
    calculate_reward_points,
    update_mission_rewards,
)


def _seed_minimum():
    Base.metadata.create_all(bind=engine)
    db = SessionLocal()
    try:
        db.add_all([District(name=name) for name in BUSAN_DISTRICTS])
        db.add_all(
            [
                Mission(
                    mission_id=1,
                    title="테스트 위치 미션",
                    district_name="중구",
                    latitude=35.1,
                    longitude=129.03,
                    reward_points=100,
                    mission_type="CURRENT_LOCATION",
                    mission_category="관광지방문",
                    image_url="https://example.com/images/junggu.jpg",
                ),
                Mission(
                    mission_id=2,
                    title="테스트 사진 미션",
                    district_name="중구",
                    latitude=35.1,
                    longitude=129.03,
                    reward_points=120,
                    mission_type="PHOTO",
                    mission_category="관광지방문",
                ),
            ]
        )
        db.commit()
    finally:
        db.close()


def test_frontend_contract():
    _seed_minimum()
    try:
        get_current_user_email(
            HTTPAuthorizationCredentials(scheme="Bearer", credentials="invalid")
        )
        raise AssertionError("유효하지 않은 토큰은 401이어야 합니다.")
    except HTTPException as exc:
        assert exc.status_code == 401

    db = SessionLocal()
    try:
        signup_response = signup(
            SignupRequest(
                email="tester@example.com",
                password="testpass123",
                nickname=" 부산탐험가 ",
            ),
            db,
        )
        subject = get_current_user_email(
            HTTPAuthorizationCredentials(
                scheme="Bearer", credentials=signup_response["token"]
            )
        )

        rankings = get_rankings("all", subject, db)
        assert set(rankings) == {"myRank", "rankings"}

        progress = get_district_progress(subject, db)
        assert len(progress) == 16
        assert {item["district_name"] for item in progress} == set(BUSAN_DISTRICTS)
        assert next(
            item for item in progress if item["district_name"] == "중구"
        ) == {
            "district_name": "중구",
            "completed_count": 0,
            "total_count": 2,
            "status": "ongoing",
        }
        assert {item["status"] for item in progress} <= {
            "empty",
            "ongoing",
            "cleared",
        }

        missions = get_missions(subject, db)
        assert missions[0]["district"] == "중구"
        assert missions[0]["latitude"] == 35.1
        assert missions[0]["longitude"] == 129.03
        assert missions[0]["image_url"] == "https://example.com/images/junggu.jpg"

        friend = AppUser(
            user_code="U999",
            login_id="friend@example.com",
            email="friend@example.com",
            nickname="친구탐험가",
            account_status="ACTIVE",
            total_points=500,
        )
        db.add(friend)
        db.flush()
        db.add(Friendship(user_code=subject, friend_user_code=friend.user_code))
        db.commit()

        friend_rankings = get_rankings("friend", subject, db)
        assert [item["userId"] for item in friend_rankings["rankings"]] == [
            "U999",
            subject,
        ]

        user = db.query(AppUser).filter(AppUser.user_code == subject).one()
        user.district_name = "중구"
        friend.district_name = "중구"
        db.commit()
        region_rankings = get_rankings("region", subject, db)
        assert {item["userId"] for item in region_rankings["rankings"]} == {
            "U999",
            subject,
        }

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
        completion = (
            db.query(UserMission)
            .filter(UserMission.user_code == subject, UserMission.mission_id == 1)
            .one()
        )
        assert completion.status == "completed"

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

        local_uri = verify_mission(
            MissionVerifyRequestDto(
                mission_id=2,
                mission_type="PHOTO",
                photo_url="content://media/picker/photo/1",
                latitude=35.1,
                longitude=129.03,
            ),
            subject,
            db,
        )
        assert local_uri == {
            "success": False,
            "message": "서버에 업로드된 인증 사진을 확인할 수 없습니다.",
        }

        upload_name = f"{'a' * 32}.jpg"
        upload_path = UPLOAD_DIR / upload_name
        UPLOAD_DIR.mkdir(parents=True, exist_ok=True)
        upload_path.write_bytes(b"test-jpeg")
        try:
            uploaded_photo = verify_mission(
                MissionVerifyRequestDto(
                    mission_id=2,
                    mission_type="PHOTO",
                    photo_url=f"https://testserver/uploads/{upload_name}",
                    latitude=35.1,
                    longitude=129.03,
                ),
                subject,
                db,
            )
            assert uploaded_photo["success"] is True
        finally:
            upload_path.unlink(missing_ok=True)

        profile = get_my_profile(subject, db)
        assert profile["name"] == "부산탐험가"
        assert profile["points"] == "220P"

        try:
            signup(
                SignupRequest(
                    email="another@example.com",
                    password="testpass123",
                    nickname="부산탐험가",
                ),
                db,
            )
            raise AssertionError("중복 닉네임 요청은 409여야 합니다.")
        except HTTPException as exc:
            assert exc.status_code == 409
            assert exc.detail == "이미 사용 중인 닉네임입니다."

        try:
            signup(
                SignupRequest(
                    email="blank@example.com",
                    password="testpass123",
                    nickname="   ",
                ),
                db,
            )
            raise AssertionError("빈 닉네임 요청은 400이어야 합니다.")
        except HTTPException as exc:
            assert exc.status_code == 400
            assert exc.detail == "닉네임을 입력해주세요."
    finally:
        db.close()


def test_tourism_score_formula():
    raw_values = {
        "중구": {indicator: 0.0 for indicator in INDICATORS},
        "해운대구": {indicator: 100.0 for indicator in INDICATORS},
    }
    scores = calculate_district_scores(raw_values)

    assert scores["중구"].activity_score == 0.0
    assert scores["중구"].difficulty_score == 1.0
    assert scores["중구"].difficulty_level == "어려움"
    assert scores["해운대구"].activity_score == 1.0
    assert scores["해운대구"].difficulty_score == 0.0
    assert scores["해운대구"].difficulty_level == "쉬움"

    assert calculate_reward_points("식당방문", 1.0) == 150
    assert calculate_reward_points("관광지방문", 0.0) == 120
    assert calculate_reward_points("식당방문", 0.25) == 113


def test_monthly_update_preserves_existing_user_total():
    db = SessionLocal()
    try:
        result = update_mission_rewards(
            db,
            {
                "중구": DistrictScore(
                    district_name="중구",
                    spending_total=10.0,
                    search_total=20.0,
                    activity_score=0.0,
                    difficulty_score=1.0,
                    difficulty_level="어려움",
                )
            },
        )
        assert result == {"updated_missions": 2, "skipped_missions": 0}
        assert db.get(Mission, 1).reward_points == 180
        completion = db.query(UserMission).filter(UserMission.mission_id == 1).one()
        user = db.query(AppUser).filter(AppUser.user_code == completion.user_code).one()
        assert user.total_points == 220
    finally:
        db.close()


if __name__ == "__main__":
    test_frontend_contract()
    test_tourism_score_formula()
    test_monthly_update_preserves_existing_user_total()
    print("API contract test passed")
