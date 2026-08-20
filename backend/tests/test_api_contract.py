import os
import sys
import asyncio
from io import BytesIO
from pathlib import Path
from PIL import Image

os.environ["DATABASE_URL"] = "sqlite:///:memory:"
os.environ["JWT_SECRET_KEY"] = "test-secret"

APP_DIR = Path(__file__).resolve().parents[1] / "app"
TEST_DEPS = Path(__file__).resolve().parents[1] / ".testdeps"
sys.path.insert(0, str(TEST_DEPS))
sys.path.insert(0, str(APP_DIR))

from auth_utils import get_current_user_email, verify_password
from database import Base, SessionLocal, engine
from fastapi import HTTPException, UploadFile
from fastapi.testclient import TestClient
from fastapi.security import HTTPAuthorizationCredentials
from starlette.datastructures import Headers
from starlette.requests import Request
from models import AppUser, District, Friendship, Mission, SavedMission, UserAgreement, UserMission
import routers.api_v1 as api_v1_module
from main import app
from routers.api_v1 import (
    BUSAN_DISTRICTS,
    FindPasswordRequest,
    FindIdRequest,
    LoginRequest,
    ChangePasswordRequest,
    KakaoLoginRequest,
    MissionVerifyRequestDto,
    NicknameUpdateRequest,
    SignupRequest,
    UPLOAD_DIR,
    get_district_progress,
    get_missions,
    get_my_profile,
    get_rankings,
    get_saved_missions,
    find_password,
    find_id,
    login,
    change_password,
    withdraw_account,
    upload_image,
    kakao_login,
    signup,
    save_mission,
    start_mission,
    cancel_mission,
    unsave_mission,
    update_my_nickname,
    verify_mission,
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
                Mission(
                    mission_id=3,
                    title="테스트 영수증 미션",
                    district_name="중구",
                    latitude=35.1,
                    longitude=129.03,
                    reward_points=150,
                    mission_type="RECEIPT",
                    mission_category="식당방문",
                ),
            ]
        )
        db.commit()
    finally:
        db.close()


def _agreements():
    return [
        {
            "doc": doc,
            "version": "1.1",
            "agreed": True,
            "agreed_at": "2026-08-20T00:00:00Z",
        }
        for doc in ("terms", "privacy", "location")
    ]


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
                agreements=_agreements(),
            ),
            db,
        )
        subject = get_current_user_email(
            HTTPAuthorizationCredentials(
                scheme="Bearer", credentials=signup_response["token"]
            )
        )

        assert db.query(UserAgreement).filter(UserAgreement.user_code == subject).count() == 3
        rankings = get_rankings(type="all", district=None, subject=subject, db=db)
        assert set(rankings) == {"myRank", "rankings"}

        progress = get_district_progress(subject, db)
        assert len(progress) == 16
        assert {item["district_name"] for item in progress} == set(BUSAN_DISTRICTS)
        assert next(
            item for item in progress if item["district_name"] == "중구"
        ) == {
            "district_name": "중구",
            "completed_count": 0,
            "total_count": 3,
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
        assert missions[0]["is_saved"] is False

        saved = save_mission(1, subject, db)
        assert saved == {"mission_id": 1, "is_saved": True}
        assert save_mission(1, subject, db) == saved
        assert db.query(SavedMission).count() == 1
        assert [mission["mission_id"] for mission in get_saved_missions(subject, db)] == [1]
        assert get_missions(subject, db)[0]["is_saved"] is True
        assert get_my_profile(subject, db)["saved_missions"] == 1

        assert unsave_mission(1, subject, db) == {
            "mission_id": 1,
            "is_saved": False,
        }
        assert unsave_mission(1, subject, db)["is_saved"] is False
        assert get_saved_missions(subject, db) == []
        assert get_my_profile(subject, db)["saved_missions"] == 0

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

        friend_rankings = get_rankings(type="friend", district=None, subject=subject, db=db)
        assert [item["userId"] for item in friend_rankings["rankings"]] == [
            "U999",
            subject,
        ]

        user = db.query(AppUser).filter(AppUser.user_code == subject).one()
        user.district_name = "중구"
        friend.district_name = "중구"
        db.commit()
        region_rankings = get_rankings(type="region", district=None, subject=subject, db=db)
        assert {item["userId"] for item in region_rankings["rankings"]} == {
            "U999",
            subject,
        }

        started = start_mission(1, subject, db)
        assert started == {"mission_id": 1, "status": "in_progress"}
        assert next(item for item in get_missions(subject, db) if item["mission_id"] == 1)[
            "status"
        ] == "in_progress"

        cancelled = cancel_mission(1, subject, db)
        assert cancelled == {"mission_id": 1, "status": "not_started"}
        assert next(item for item in get_missions(subject, db) if item["mission_id"] == 1)[
            "status"
        ] == "not_started"

        start_mission(1, subject, db)

        inaccurate = verify_mission(
            MissionVerifyRequestDto(
                mission_id=1,
                mission_type="CURRENT_LOCATION",
                latitude=35.1,
                longitude=129.03,
                location_accuracy_m=150,
            ),
            subject,
            db,
        )
        assert inaccurate["success"] is False
        assert "정확도가 낮습니다" in inaccurate["message"]

        too_far = verify_mission(
            MissionVerifyRequestDto(
                mission_id=1,
                mission_type="CURRENT_LOCATION",
                latitude=36.1,
                longitude=129.03,
                location_accuracy_m=20,
            ),
            subject,
            db,
        )
        assert too_far["success"] is False
        assert "허용 반경" in too_far["message"]

        # 기존 운영 DB에 남아 있을 수 있는 LOCATION 표기도 현재 API 타입으로
        # 정규화되어 목록 조회와 인증 모두 정상 동작해야 한다.
        legacy_mission = db.query(Mission).filter(Mission.mission_id == 1).one()
        legacy_mission.mission_type = "LOCATION"
        db.commit()
        mission_from_api = next(
            item for item in get_missions(subject, db) if item["mission_id"] == 1
        )
        assert mission_from_api["mission_type"] == "CURRENT_LOCATION"



        verify = verify_mission(
            MissionVerifyRequestDto(
                mission_id=1,
                mission_type=" current_location ",
                latitude=35.1,
                longitude=129.03,
                location_accuracy_m=20,
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

        missing_receipt = verify_mission(
            MissionVerifyRequestDto(
                mission_id=3,
                mission_type="RECEIPT",
            ),
            subject,
            db,
        )
        assert missing_receipt == {
            "success": False,
            "message": "서버에 업로드된 영수증 이미지를 확인할 수 없습니다.",
        }

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

        class GeminiResponse:
            text = '{"is_success": true, "extracted_text": "테스트 승인"}'

        class GeminiModels:
            @staticmethod
            def generate_content(*args, **kwargs):
                return GeminiResponse()

        class GeminiClient:
            models = GeminiModels()

        original_client = api_v1_module.gemini_client
        api_v1_module.gemini_client = GeminiClient()
        upload_name = f"{'a' * 32}.jpg"
        upload_path = UPLOAD_DIR / upload_name
        UPLOAD_DIR.mkdir(parents=True, exist_ok=True)
        Image.new("RGB", (2, 2), "white").save(upload_path, format="JPEG")
        try:
            uploaded_photo = verify_mission(
                MissionVerifyRequestDto(
                    mission_id=2,
                    mission_type="PHOTO",
                    photo_url=f"https://testserver/uploads/{upload_name}",
                    latitude=35.1,
                    longitude=129.03,
                    location_accuracy_m=20,
                ),
                subject,
                db,
            )
            assert uploaded_photo["success"] is True

            receipt_name = f"{'b' * 32}.jpg"
            receipt_path = UPLOAD_DIR / receipt_name
            Image.new("RGB", (2, 2), "white").save(receipt_path, format="JPEG")
            uploaded_receipt = verify_mission(
                MissionVerifyRequestDto(
                    mission_id=3,
                    mission_type="RECEIPT",
                    receipt_image_url=f"https://testserver/uploads/{receipt_name}",
                ),
                subject,
                db,
            )
            assert uploaded_receipt["success"] is True
            assert not receipt_path.exists()
        finally:
            upload_path.unlink(missing_ok=True)
            if "receipt_path" in locals():
                receipt_path.unlink(missing_ok=True)
            api_v1_module.gemini_client = original_client

        profile = get_my_profile(subject, db)
        assert profile["name"] == "부산탐험가"
        assert profile["points"] == "370P"
        assert profile["completed_missions"] == 3

        try:
            signup(
                SignupRequest(
                    email="another@example.com",
                    password="testpass123",
                    nickname="부산탐험가",
                    agreements=_agreements(),
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
                    agreements=_agreements(),
                ),
                db,
            )
            raise AssertionError("빈 닉네임 요청은 400이어야 합니다.")
        except HTTPException as exc:
            assert exc.status_code == 400
            assert exc.detail == "닉네임을 입력해주세요."
    finally:
        db.close()


def test_kakao_signup_without_email():
    class KakaoResponse:
        status_code = 200

        @staticmethod
        def json():
            return {
                "id": 123456789,
                "kakao_account": {"profile": {"nickname": "카카오탐험가"}},
            }

    original_get = api_v1_module.httpx.get
    api_v1_module.httpx.get = lambda *args, **kwargs: KakaoResponse()
    db = SessionLocal()
    try:
        response = kakao_login(
            KakaoLoginRequest(access_token="valid-token", agreements=_agreements()), db
        )
        assert response["token"]
        user = db.query(AppUser).filter(AppUser.kakao_id == "123456789").one()
        assert user.email is None
        assert user.login_id == "kakao:123456789"
    finally:
        api_v1_module.httpx.get = original_get
        db.close()


def test_nickname_update_contract():
    db = SessionLocal()
    try:
        user = db.query(AppUser).filter(AppUser.email == "tester@example.com").one()
        updated = update_my_nickname(
            NicknameUpdateRequest(nickname="새닉네임"), user.user_code, db
        )
        assert updated == {
            "name": "새닉네임",
            "points": "370P",
            "completed_missions": 3,
            "saved_missions": 0,
        }

        try:
            update_my_nickname(
                NicknameUpdateRequest(nickname="친구탐험가"), user.user_code, db
            )
            raise AssertionError("중복 닉네임은 거절되어야 합니다.")
        except HTTPException as exc:
            assert exc.status_code == 409
            assert exc.detail == "이미 사용 중인 닉네임입니다."

        for invalid in ("한", "가나다라마바사아자차카타파", "공백 닉네임"):
            try:
                update_my_nickname(
                    NicknameUpdateRequest(nickname=invalid), user.user_code, db
                )
                raise AssertionError("형식이 잘못된 닉네임은 거절되어야 합니다.")
            except HTTPException as exc:
                assert exc.status_code == 400
    finally:
        db.close()


def test_account_lifecycle_contract():
    db = SessionLocal()
    try:
        signup_response = signup(
            SignupRequest(
                email="lifecycle@example.com",
                password="initialPass123",
                nickname="계정테스터",
                agreements=_agreements(),
            ),
            db,
        )
        subject = get_current_user_email(
            HTTPAuthorizationCredentials(
                scheme="Bearer", credentials=signup_response["token"]
            )
        )

        assert login(
            LoginRequest(email=" LIFECYCLE@example.com ", password="initialPass123"),
            db,
        )["token"]
        assert find_id(FindIdRequest(nickname="계정테스터"), db)["masked_email"] == (
            "li*******@example.com"
        )

        try:
            change_password(
                ChangePasswordRequest(old_password="wrong", new_password="changedPass123"),
                subject,
                db,
            )
            raise AssertionError("현재 비밀번호가 다르면 변경이 거절되어야 합니다.")
        except HTTPException as exc:
            assert exc.status_code == 400

        assert change_password(
            ChangePasswordRequest(
                old_password="initialPass123",
                new_password="changedPass123",
            ),
            subject,
            db,
        )["success"] is True
        assert login(
            LoginRequest(email="lifecycle@example.com", password="changedPass123"),
            db,
        )["token"]

        assert withdraw_account(subject, db)["success"] is True
        assert db.query(AppUser).filter(AppUser.user_code == subject).first() is None
        assert db.query(UserAgreement).filter(UserAgreement.user_code == subject).count() == 0
    finally:
        db.close()


def test_upload_contract_rejects_non_jpeg_and_accepts_real_jpeg():
    request = Request(
        {
            "type": "http",
            "scheme": "https",
            "server": ("testserver", 443),
            "path": "/api/v1/uploads",
            "root_path": "",
            "query_string": b"",
            "headers": [],
        }
    )

    invalid = UploadFile(
        BytesIO(b"not-an-image"),
        filename="fake.png",
        headers=Headers({"content-type": "image/png"}),
    )
    try:
        asyncio.run(upload_image(request, invalid, "test-user"))
        raise AssertionError("PNG 업로드는 거절되어야 합니다.")
    except HTTPException as exc:
        assert exc.status_code == 400

    image_bytes = BytesIO()
    Image.new("RGB", (4, 4), "blue").save(image_bytes, format="JPEG")
    image_bytes.seek(0)
    valid = UploadFile(
        image_bytes,
        filename="mission.jpg",
        headers=Headers({"content-type": "image/jpeg"}),
    )
    response = asyncio.run(upload_image(request, valid, "test-user"))
    filename = Path(response["url"]).name
    uploaded = UPLOAD_DIR / filename
    try:
        assert uploaded.is_file()
        assert filename.endswith(".jpg")
    finally:
        uploaded.unlink(missing_ok=True)


def test_health_endpoint_and_lifespan():
    with TestClient(app) as client:
        response = client.get("/")
    assert response.status_code == 200
    assert response.json()["version"] == "1.0.0"


def test_password_recovery_never_exposes_temporary_password():
    sent: dict[str, str] = {}
    original_sender = api_v1_module._send_temporary_password
    api_v1_module._password_reset_attempts.clear()
    api_v1_module._send_temporary_password = (
        lambda recipient, temporary_password: sent.update(
            recipient=recipient,
            temporary_password=temporary_password,
        )
    )
    db = SessionLocal()
    try:
        response = find_password(FindPasswordRequest(email="tester@example.com"), db)
        serialized = str(response).lower()
        assert "temp_password" not in response
        assert sent["recipient"] == "tester@example.com"
        assert sent["temporary_password"] not in serialized

        user = db.query(AppUser).filter(AppUser.email == "tester@example.com").one()
        assert verify_password(sent["temporary_password"], user.password_hash)
    finally:
        api_v1_module._send_temporary_password = original_sender
        api_v1_module._password_reset_attempts.clear()
        db.close()


if __name__ == "__main__":
    test_frontend_contract()
    test_kakao_signup_without_email()
    test_nickname_update_contract()
    test_password_recovery_never_exposes_temporary_password()
    print("API contract test passed")
