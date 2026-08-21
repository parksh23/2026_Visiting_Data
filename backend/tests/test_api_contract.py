import os
import sys
from datetime import datetime
from pathlib import Path
from types import SimpleNamespace
from zoneinfo import ZoneInfo

os.environ["DATABASE_URL"] = "sqlite:///:memory:"
os.environ["JWT_SECRET_KEY"] = "test-secret"

APP_DIR = Path(__file__).resolve().parents[1] / "app"
TEST_DEPS = Path(__file__).resolve().parents[1] / ".testdeps"
sys.path.insert(0, str(TEST_DEPS))
sys.path.insert(0, str(APP_DIR))

from auth_utils import get_current_user_email
from database import Base, SessionLocal, engine
from document_seed import seed_documents
from fastapi import HTTPException
from fastapi.security import HTTPAuthorizationCredentials
from PIL import Image
from models import (
    AppRanking,
    AppUser,
    District,
    Friendship,
    Mission,
    PendingPush,
    SavedMission,
    PushToken,
    PushDeliveryLog,
    UserAgreement,
    UserMission,
    UserSettings,
)
import routers.api_v1 as api_v1_module
import notification_jobs as notification_jobs_module
import push_notifications as push_notifications_module
from routers.api_v1 import (
    BUSAN_DISTRICTS,
    AgreementInput,
    KakaoLoginRequest,
    MissionVerifyRequestDto,
    NicknameUpdateRequest,
    NotificationSettingsUpdate,
    PushTokenRequest,
    PushTokenDeleteRequest,
    SignupRequest,
    UPLOAD_DIR,
    get_district_progress,
    get_missions,
    get_document,
    get_my_agreements,
    get_my_profile,
    get_notification_settings,
    get_rankings,
    get_saved_missions,
    kakao_login,
    signup,
    save_mission,
    register_push_token,
    unregister_push_token,
    unsave_mission,
    update_my_nickname,
    update_notification_settings,
    verify_mission,
    withdraw_account,
)
from tourism_scoring import (
    DistrictScore,
    INDICATORS,
    calculate_district_scores,
    calculate_reward_points,
    update_mission_rewards,
)


def _required_agreements(agreed_at: str = "2026-08-16T05:12:44Z"):
    return [
        AgreementInput(doc=doc, version="1.0", agreed=True, agreed_at=agreed_at)
        for doc in ("terms", "privacy", "location")
    ]


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
                    mission_category="장소탐방",
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
                    mission_category="장소탐방",
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
                agreements=_required_agreements(),
            ),
            db,
        )
        subject = get_current_user_email(
            HTTPAuthorizationCredentials(
                scheme="Bearer", credentials=signup_response["token"]
            )
        )
        agreements = get_my_agreements(subject, db)
        assert len(agreements) == 3
        assert {item["doc"] for item in agreements} == {
            "terms",
            "privacy",
            "location",
        }
        assert {item["agreed_at"] for item in agreements} == {
            api_v1_module.datetime(2026, 8, 16, 5, 12, 44)
        }

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

        friend_rankings = get_rankings(
            type="friend", district=None, subject=subject, db=db
        )
        assert [item["userId"] for item in friend_rankings["rankings"]] == [
            "U999",
            subject,
        ]

        user = db.query(AppUser).filter(AppUser.user_code == subject).one()
        user.district_name = "중구"
        friend.district_name = "중구"
        db.commit()
        region_rankings = get_rankings(
            type="region", district=None, subject=subject, db=db
        )
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
                accuracy_m=10,
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
        upload_path = UPLOAD_DIR / subject / upload_name
        upload_path.parent.mkdir(parents=True, exist_ok=True)
        Image.new("RGB", (8, 8), color="blue").save(upload_path, format="JPEG")
        original_generate = api_v1_module.gemini_model.generate_content
        api_v1_module.gemini_model.generate_content = lambda *args, **kwargs: SimpleNamespace(
            text='{"is_success": true, "extracted_text": "사진 확인"}'
        )
        try:
            uploaded_photo = verify_mission(
                MissionVerifyRequestDto(
                    mission_id=2,
                    mission_type="PHOTO",
                    photo_url=f"https://testserver/uploads/{subject}/{upload_name}",
                    latitude=35.1,
                    longitude=129.03,
                    accuracy_m=10,
                ),
                subject,
                db,
            )
            assert uploaded_photo["success"] is True
            photo_completion = (
                db.query(UserMission)
                .filter_by(user_code=subject, mission_id=2)
                .one()
            )
            assert photo_completion.photo_url == (
                f"https://testserver/uploads/{subject}/{upload_name}"
            )
            assert not api_v1_module._uploaded_image_exists(
                f"https://testserver/uploads/{subject}/{upload_name}", "OTHER_USER"
            )
        finally:
            api_v1_module.gemini_model.generate_content = original_generate
            upload_path.unlink(missing_ok=True)

        profile = get_my_profile(subject, db)
        assert profile["name"] == "부산탐험가"
        assert profile["points"] == "220P"
        assert profile["total_points"] == 220
        assert profile["email"] == "tester@example.com"
        assert profile["login_provider"] == "EMAIL"

        try:
            signup(
                SignupRequest(
                    email="another@example.com",
                    password="testpass123",
                    nickname="부산탐험가",
                    agreements=_required_agreements(),
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

    assert scores["중구"].tourism_activity == 0.0
    assert scores["중구"].activation_need == 1.0
    assert scores["중구"].region_bonus == 30
    assert scores["해운대구"].tourism_activity == 1.0
    assert scores["해운대구"].activation_need == 0.0
    assert scores["해운대구"].region_bonus == 0

    assert calculate_reward_points("장소탐방", 20, 10) == 130
    assert calculate_reward_points("등산", 30, 20) == 200


def test_monthly_update_preserves_existing_user_total():
    db = SessionLocal()
    try:
        result = update_mission_rewards(
            db,
            {
                "중구": DistrictScore(
                    district_name="중구",
                    stay_intensity=10.0,
                    consumption_intensity=20.0,
                    tourism_activity=0.0,
                    activation_need=1.0,
                    region_bonus=20,
                )
            },
            bus_locations=[(35.1, 129.031)],
            rail_locations=[(35.1, 129.035)],
        )
        assert result == {"updated_missions": 2, "skipped_missions": 0}
        assert db.get(Mission, 1).reward_points == 125
        completion = db.query(UserMission).filter(UserMission.mission_id == 1).one()
        user = db.query(AppUser).filter(AppUser.user_code == completion.user_code).one()
        assert user.total_points == 220
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
        request = KakaoLoginRequest(
            access_token="valid-token", agreements=_required_agreements()
        )
        response = kakao_login(request, db)
        assert response["access_token"]
        assert response["token"] == response["access_token"]
        user = db.query(AppUser).filter(AppUser.kakao_id == "123456789").one()
        assert user.email is None
        assert user.login_id == "kakao:123456789"
        assert db.query(UserAgreement).filter_by(user_code=user.user_code).count() == 3

        kakao_login(KakaoLoginRequest(access_token="valid-token"), db)
        assert db.query(UserAgreement).filter_by(user_code=user.user_code).count() == 3
    finally:
        api_v1_module.httpx.get = original_get
        db.close()


def _fake_kakao(kakao_id: int, nickname: str, email=None):
    """카카오 /v2/user/me 응답을 흉내내는 객체."""
    account = {"profile": {"nickname": nickname}}
    if email:
        account["email"] = email

    class KakaoResponse:
        status_code = 200

        @staticmethod
        def json():
            return {"id": kakao_id, "kakao_account": account}

    return lambda *args, **kwargs: KakaoResponse()


def test_kakao_duplicate_nickname():
    """카카오 프로필 닉네임이 이미 쓰이고 있어도 500이 아니라 접미사가 붙어야 한다.

    APP_USERS.NICKNAME 에 UNIQUE 제약(005)이 걸려 있어서, 예전에는 흔한 닉네임을 쓰는
    두 번째 카카오 사용자가 IntegrityError → 500 을 받았다.
    """
    original_get = api_v1_module.httpx.get
    db = SessionLocal()
    try:
        api_v1_module.httpx.get = _fake_kakao(811111111, "민수")
        kakao_login(
            KakaoLoginRequest(access_token="t1", agreements=_required_agreements()), db
        )
        first = db.query(AppUser).filter(AppUser.kakao_id == "811111111").one()
        assert first.nickname == "민수"

        # 같은 닉네임을 가진 다른 카카오 계정
        api_v1_module.httpx.get = _fake_kakao(822222222, "민수")
        kakao_login(
            KakaoLoginRequest(access_token="t2", agreements=_required_agreements()), db
        )
        second = db.query(AppUser).filter(AppUser.kakao_id == "822222222").one()
        assert second.nickname != first.nickname
        assert second.nickname.startswith("민수")
        assert db.query(UserAgreement).filter_by(user_code=second.user_code).count() == 3
    finally:
        api_v1_module.httpx.get = original_get
        db.close()


def test_withdrawal_deletes_related_rows_and_uploaded_images():
    """탈퇴는 모든 FK 자식 행과 사용자 업로드 파일까지 하드 삭제한다."""
    original_get = api_v1_module.httpx.get
    db = SessionLocal()
    try:
        api_v1_module.httpx.get = _fake_kakao(
            833333333, "탈퇴예정", email="bye@example.com"
        )
        kakao_login(
            KakaoLoginRequest(access_token="t3", agreements=_required_agreements()), db
        )
        user = db.query(AppUser).filter(AppUser.kakao_id == "833333333").one()
        user_code = user.user_code
        db.add(UserSettings(user_code=user_code))
        db.add(PushToken(user_code=user_code, token="fcm-withdraw-test"))
        db.add(
            PendingPush(
                user_code=user_code,
                title="탈퇴 테스트",
                body="삭제되어야 함",
                notification_type="test",
                idempotency_key="withdraw-test",
                scheduled_at=datetime.utcnow(),
            )
        )
        db.add(
            AppRanking(
                user_code=user_code,
                nickname=user.nickname,
                total_points=0,
                rank_num=1,
            )
        )
        db.add(
            PushDeliveryLog(
                user_code=user_code,
                idempotency_key="withdraw-log-test",
                notification_type="test",
                success_count=1,
                failure_count=0,
                status="sent",
            )
        )
        db.commit()

        user_upload_dir = UPLOAD_DIR / user_code
        user_upload_dir.mkdir(parents=True, exist_ok=True)
        (user_upload_dir / f"{'b' * 32}.jpg").write_bytes(b"private-image")

        result = withdraw_account(subject=user_code, db=db)
        assert result["success"] is True

        assert db.query(AppUser).filter_by(user_code=user_code).count() == 0
        assert db.query(UserAgreement).filter_by(user_code=user_code).count() == 0
        assert db.query(PushToken).filter_by(user_code=user_code).count() == 0
        assert db.query(UserSettings).filter_by(user_code=user_code).count() == 0
        assert db.query(PendingPush).filter_by(user_code=user_code).count() == 0
        assert db.query(PushDeliveryLog).filter_by(user_code=user_code).count() == 0
        assert db.query(AppRanking).filter_by(user_code=user_code).count() == 0
        assert not user_upload_dir.exists()

        # 탈퇴 계정의 토큰으로는 아무 API 도 못 쓴다
        try:
            api_v1_module._get_user(db, user_code)
            raise AssertionError("삭제된 계정은 401이어야 합니다.")
        except HTTPException as exc:
            assert exc.status_code == 401

        # 같은 카카오 계정으로 다시 들어오면 '신규 가입'이라 약관 동의가 다시 필요하다
        try:
            kakao_login(KakaoLoginRequest(access_token="t3"), db)
            raise AssertionError("재가입은 약관 동의 없이는 400이어야 합니다.")
        except HTTPException as exc:
            assert exc.status_code == 400

        kakao_login(
            KakaoLoginRequest(access_token="t3", agreements=_required_agreements()), db
        )
        rejoined = db.query(AppUser).filter(AppUser.kakao_id == "833333333").one()
        assert rejoined.user_code != user_code
        assert rejoined.account_status == "ACTIVE"
    finally:
        api_v1_module.httpx.get = original_get
        db.close()


def test_signup_agreement_validation_and_timestamp_fallback():
    db = SessionLocal()
    try:
        try:
            signup(
                SignupRequest(
                    email="missing-agreements@example.com",
                    password="testpass123",
                    nickname="약관누락",
                ),
                db,
            )
            raise AssertionError("필수 약관 누락은 400이어야 합니다.")
        except HTTPException as exc:
            assert exc.status_code == 400
            assert exc.detail == "필수 약관에 모두 동의해야 가입할 수 있습니다."

        declined = _required_agreements()
        declined[-1] = AgreementInput(
            doc="location",
            version="1.0",
            agreed=False,
            agreed_at="2026-08-16T05:12:51Z",
        )
        try:
            signup(
                SignupRequest(
                    email="declined-location@example.com",
                    password="testpass123",
                    nickname="위치거절",
                    agreements=declined,
                ),
                db,
            )
            raise AssertionError("필수 약관 거절은 400이어야 합니다.")
        except HTTPException as exc:
            assert exc.status_code == 400

        before = api_v1_module.datetime.utcnow()
        signup(
            SignupRequest(
                email="invalid-time@example.com",
                password="testpass123",
                nickname="시간대체",
                agreements=_required_agreements("abc"),
            ),
            db,
        )
        after = api_v1_module.datetime.utcnow()
        user = db.query(AppUser).filter_by(email="invalid-time@example.com").one()
        rows = db.query(UserAgreement).filter_by(user_code=user.user_code).all()
        assert len(rows) == 3
        assert all(before <= row.agreed_at <= after for row in rows)
    finally:
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
            "points": "220P",
            "total_points": 220,
            "email": "tester@example.com",
            "login_provider": "EMAIL",
            "completed_missions": 2,
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


def test_settings_documents_and_push_token_contract():
    db = SessionLocal()
    try:
        signup(
            SignupRequest(
                email="settings@example.com",
                password="testpass123",
                nickname="설정테스터",
                agreements=_required_agreements(),
            ),
            db,
        )
        user = db.query(AppUser).filter(AppUser.email == "settings@example.com").one()
        profile = get_my_profile(user.user_code, db)
        assert profile["total_points"] == 0
        assert profile["email"] == "settings@example.com"
        assert profile["login_provider"] == "EMAIL"
        defaults = get_notification_settings(user.user_code, db)
        assert defaults == {
            "mission_result": True,
            "new_mission": True,
            "ranking_change": False,
            "night_mute": True,
            "marketing": False,
        }

        updated = update_notification_settings(
            NotificationSettingsUpdate(
                ranking_change=True,
                night_mute=False,
                marketing=True,
            ),
            user.user_code,
            db,
        )
        assert updated["ranking_change"] is True
        assert updated["night_mute"] is False
        assert updated["marketing"] is True
        settings = db.get(UserSettings, user.user_code)
        assert settings.marketing_agreed_at is not None

        register_push_token(
            PushTokenRequest(token="test-fcm-token", platform="android"),
            user.user_code,
            db,
        )
        register_push_token(
            PushTokenRequest(token="test-fcm-token", platform="android"),
            user.user_code,
            db,
        )
        assert db.query(PushToken).filter_by(token="test-fcm-token").count() == 1
        unregister_push_token(
            PushTokenDeleteRequest(token="test-fcm-token"), user.user_code, db
        )
        assert db.query(PushToken).filter_by(token="test-fcm-token").count() == 0

        seed_documents(db)
        document = get_document("terms", db)
        assert document["slug"] == "terms"
        assert document["title"] == "이용약관"
        assert document["version"] == "1.0"
        assert document["effective_date"] == "2026-08-11"
        assert "## 제1조" in document["content"]
        assert get_document("privacy", db)["slug"] == "privacy"
        assert get_document("location", db)["slug"] == "location"
    finally:
        db.close()


def test_new_mission_region_filter_and_ranking_rise_only():
    db = SessionLocal()
    original_dispatch = notification_jobs_module.dispatch_notification
    calls = []

    def record_dispatch(**kwargs):
        calls.append(kwargs)
        return "sent"

    notification_jobs_module.dispatch_notification = record_dispatch
    try:
        for email, nickname, district in (
            ("junggu@example.com", "중구사용자", "중구"),
            ("haeundae@example.com", "해운대사용자", "해운대구"),
            ("all-busan@example.com", "전체사용자", None),
        ):
            signup(
                SignupRequest(
                    email=email,
                    password="testpass123",
                    nickname=nickname,
                    agreements=_required_agreements(),
                ),
                db,
            )
            user = db.query(AppUser).filter_by(email=email).one()
            user.district_name = district
        db.commit()

        kst = ZoneInfo("Asia/Seoul")
        initialized_at = datetime(2026, 8, 20, 17, 59, tzinfo=kst)
        initialized = notification_jobs_module.run_new_mission_notifications(
            db, initialized_at
        )
        assert initialized["initialized"] is True

        created_at = datetime(2026, 8, 20, 9, 0)
        db.add(
            Mission(
                mission_id=3,
                title="중구 신규 미션",
                district_name="중구",
                latitude=35.1,
                longitude=129.03,
                reward_points=100,
                mission_type="CURRENT_LOCATION",
                mission_category="관광지방문",
                created_at=created_at,
            )
        )
        db.commit()
        result = notification_jobs_module.run_new_mission_notifications(
            db, datetime(2026, 8, 20, 18, 1, tzinfo=kst)
        )
        assert result["missions"] == 1
        notified_users = {call["user_code"] for call in calls}
        junggu = db.query(AppUser).filter_by(email="junggu@example.com").one()
        haeundae = db.query(AppUser).filter_by(email="haeundae@example.com").one()
        all_busan = db.query(AppUser).filter_by(email="all-busan@example.com").one()
        assert junggu.user_code in notified_users
        assert all_busan.user_code in notified_users
        assert haeundae.user_code not in notified_users
        assert all(call["notification_type"] == "NEW_MISSION" for call in calls)

        calls.clear()
        junggu.total_points = 100
        haeundae.total_points = 50
        db.commit()
        notification_jobs_module.run_ranking_notifications(
            db, datetime(2026, 8, 20, 18, 10, tzinfo=kst)
        )
        assert calls == []
        previous_rank = haeundae.last_notified_rank

        haeundae.total_points = 200
        db.commit()
        notification_jobs_module.run_ranking_notifications(
            db, datetime(2026, 8, 21, 18, 10, tzinfo=kst)
        )
        ranking_calls = [
            call for call in calls if call["notification_type"] == "RANKING_CHANGE"
        ]
        assert [call["user_code"] for call in ranking_calls] == [haeundae.user_code]
        assert f"{previous_rank}위 → {haeundae.last_notified_rank}위" in ranking_calls[0]["body"]
    finally:
        notification_jobs_module.dispatch_notification = original_dispatch
        db.close()


def test_fcm_payload_and_invalid_token_cleanup():
    class UnregisteredError(Exception):
        pass

    class FakeMessaging:
        captured = None

        @staticmethod
        def Notification(**kwargs):
            return kwargs

        @staticmethod
        def AndroidNotification(**kwargs):
            return kwargs

        @staticmethod
        def AndroidConfig(**kwargs):
            return kwargs

        @staticmethod
        def MulticastMessage(**kwargs):
            FakeMessaging.captured = kwargs
            return kwargs

        @staticmethod
        def send_each_for_multicast(_message):
            return SimpleNamespace(
                success_count=1,
                failure_count=1,
                responses=[
                    SimpleNamespace(success=True, exception=None),
                    SimpleNamespace(success=False, exception=UnregisteredError()),
                ],
            )

    db = SessionLocal()
    original_firebase = push_notifications_module._firebase_messaging
    push_notifications_module._firebase_messaging = lambda: FakeMessaging
    try:
        signup(
            SignupRequest(
                email="fcm@example.com",
                password="testpass123",
                nickname="푸시테스터",
                agreements=_required_agreements(),
            ),
            db,
        )
        user = db.query(AppUser).filter_by(email="fcm@example.com").one()
        db.add_all(
            [
                PushToken(user_code=user.user_code, token="valid-token"),
                PushToken(user_code=user.user_code, token="dead-token"),
            ]
        )
        db.commit()
        result = push_notifications_module.send_to_user(
            db,
            user.user_code,
            "NEW_MISSION",
            "새 미션이 열렸어요",
            "새로운 미션이 추가됐어요.",
            {"mission_id": 12},
        )
        assert result.success_count == 1
        assert result.failure_count == 1
        payload = FakeMessaging.captured
        assert payload["android"]["priority"] == "high"
        assert payload["android"]["collapse_key"] == "new_mission"
        assert payload["android"]["notification"]["channel_id"] == "new_mission"
        assert payload["data"] == {"mission_id": "12", "type": "NEW_MISSION"}
        assert db.query(PushToken).filter_by(token="dead-token").count() == 0
        assert db.query(PushToken).filter_by(token="valid-token").count() == 1
        first = push_notifications_module.dispatch_notification(
            db=db,
            user_code=user.user_code,
            notification_type="NEW_MISSION",
            title="새 미션이 열렸어요",
            body="새로운 미션이 추가됐어요.",
            data={"mission_id": "12"},
            idempotency_key="test:new-mission:12",
            now_kst=datetime(2026, 8, 20, 18, 0, tzinfo=ZoneInfo("Asia/Seoul")),
        )
        second = push_notifications_module.dispatch_notification(
            db=db,
            user_code=user.user_code,
            notification_type="NEW_MISSION",
            title="새 미션이 열렸어요",
            body="새로운 미션이 추가됐어요.",
            data={"mission_id": "12"},
            idempotency_key="test:new-mission:12",
            now_kst=datetime(2026, 8, 20, 18, 0, tzinfo=ZoneInfo("Asia/Seoul")),
        )
        assert first == "partial"
        assert second == "duplicate"
        assert (
            db.query(PushDeliveryLog)
            .filter_by(idempotency_key="test:new-mission:12")
            .count()
            == 1
        )
    finally:
        push_notifications_module._firebase_messaging = original_firebase
        db.close()


if __name__ == "__main__":
    test_frontend_contract()
    test_tourism_score_formula()
    test_monthly_update_preserves_existing_user_total()
    test_kakao_signup_without_email()
    test_kakao_duplicate_nickname()
    test_withdrawal_deletes_related_rows_and_uploaded_images()
    test_signup_agreement_validation_and_timestamp_fallback()
    test_nickname_update_contract()
    test_settings_documents_and_push_token_contract()
    test_new_mission_region_filter_and_ranking_rise_only()
    test_fcm_payload_and_invalid_token_cleanup()
    print("API contract test passed")
