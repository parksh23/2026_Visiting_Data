import base64
import copy
import io
import os
import sys
import time
from datetime import datetime, timedelta, timezone
from pathlib import Path

os.environ["DATABASE_URL"] = "sqlite:///:memory:"
os.environ["JWT_SECRET_KEY"] = "test-secret"
sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "app"))

import pytest
from fastapi import FastAPI, HTTPException
from fastapi.testclient import TestClient
from pydantic import ValidationError
from PIL import Image
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from sqlalchemy.pool import StaticPool
from database import Base, get_db
from models import AppUser, District, LocationChallenge, Mission, UserMission
import location_verification as security
import routers.api_v1 as api

CERT = base64.urlsafe_b64encode(bytes(range(32))).decode().rstrip("=")


@pytest.fixture
def db(monkeypatch):
    monkeypatch.setenv("PLAY_INTEGRITY_CLOUD_PROJECT_NUMBER", "123456789")
    monkeypatch.setenv("PLAY_INTEGRITY_CERTIFICATE_DIGESTS", CERT)
    monkeypatch.setenv("PLAY_INTEGRITY_MIN_VERSION_CODE", "6")
    engine = create_engine("sqlite://", connect_args={"check_same_thread": False}, poolclass=StaticPool)
    Base.metadata.create_all(engine)
    session = sessionmaker(bind=engine)()
    session.add(District(name="중구"))
    session.add_all([AppUser(user_code=u, nickname=u) for u in ("test-a", "test-b")])
    for i in (1, 2):
        session.add(Mission(mission_id=i, title=f"Mission {i}", district_name="중구",
            mission_type="CURRENT_LOCATION", latitude=35.1, longitude=129.03, reward_points=100))
        session.add(UserMission(user_code="test-a", mission_id=i, status="ongoing"))
    session.add(UserMission(user_code="test-b", mission_id=1, status="ongoing"))
    session.commit()
    yield session
    session.close()
    engine.dispose()


def request(db):
    challenge = api.create_location_challenge(1, "test-a", db)
    return api.MissionVerifyRequestDto(mission_id=1, mission_type="CURRENT_LOCATION",
        challenge_id=challenge["challenge_id"], local_passed=True, integrity_token="opaque-token")


def verdict(req):
    return {
        "requestDetails": {"requestPackageName": "kr.co.busanquest", "timestampMillis": str(int(time.time()*1000)),
            "requestHash": security.verification_request_hash(req)},
        "appIntegrity": {"appRecognitionVerdict": "PLAY_RECOGNIZED", "packageName": "kr.co.busanquest",
            "versionCode": "6", "certificateSha256Digest": [CERT]},
        "deviceIntegrity": {"deviceRecognitionVerdict": ["MEETS_DEVICE_INTEGRITY"]},
        "accountDetails": {"appLicensingVerdict": "LICENSED"},
    }


def points(db):
    db.expire_all()
    return db.query(AppUser).filter_by(user_code="test-a").one().total_points


def test_valid_proof_rewards_once_and_replay_is_rejected(db, monkeypatch):
    req = request(db)
    monkeypatch.setattr(security, "decode_integrity_token", lambda token: verdict(req))
    assert api.verify_mission(req, "test-a", db)["success"]
    assert not api.verify_mission(req, "test-a", db)["success"]
    assert points(db) == 100
    assert db.query(LocationChallenge).one().consumed == 1


@pytest.mark.parametrize("stored_type", ["current_location", "CURRENT_LOCATION  ", " current_location ",
    "LOCATION", "gps", "위치 인증", "현재 위치 인증"])
def test_legacy_location_type_consistent_from_list_to_reward(db, monkeypatch, stored_type):
    mission = db.query(Mission).filter_by(mission_id=1).one()
    mission.title = "금정산성 북문 성곽길 걷기"
    mission.mission_type = stored_type
    db.commit()
    listed = next(item for item in api.get_missions("test-a", db) if item["mission_id"] == 1)
    assert listed["mission_type"] == "CURRENT_LOCATION"
    challenge = api.create_location_challenge(1, "test-a", db)
    assert challenge["mission_type"] == "CURRENT_LOCATION"
    req = api.MissionVerifyRequestDto(mission_id=1, mission_type=challenge["mission_type"],
        challenge_id=challenge["challenge_id"], local_passed=True, integrity_token="opaque-token")
    monkeypatch.setattr(security, "decode_integrity_token", lambda token: verdict(req))
    assert api.verify_mission(req, "test-a", db)["success"]
    assert points(db) == 100


@pytest.mark.parametrize("stored_type", [" photo ", "PHOTO_LOCATION", "IMAGE", "IMAGE_LOCATION"])
def test_legacy_photo_type_keeps_photo_validation(db, stored_type):
    db.query(Mission).filter_by(mission_id=1).one().mission_type = stored_type
    db.commit()
    assert api.create_location_challenge(1, "test-a", db)["mission_type"] == "PHOTO"
    assert api.verify_mission(api.MissionVerifyRequestDto(mission_id=1, mission_type="PHOTO"), "test-a", db) == {
        "success": False, "message": "서버에 업로드된 인증 사진을 확인할 수 없습니다.",
    }
    assert not api.verify_mission(api.MissionVerifyRequestDto(mission_id=1,
        mission_type="CURRENT_LOCATION"), "test-a", db)["success"]


@pytest.mark.parametrize("stored_type", ["RECEIPT", "unknown", ""])
def test_non_location_types_still_reject_challenges(db, stored_type):
    db.query(Mission).filter_by(mission_id=1).one().mission_type = stored_type
    db.commit()
    with pytest.raises(HTTPException) as exc:
        api.create_location_challenge(1, "test-a", db)
    assert exc.value.status_code == 400


@pytest.mark.parametrize("stored_status", ["ongoing", "ONGOING", " ongoing ", "in_progress", "IN_PROGRESS"])
def test_displayed_ongoing_status_can_start_and_verify(db, monkeypatch, stored_status):
    db.query(UserMission).filter_by(user_code="test-a", mission_id=1).one().status = stored_status
    db.commit()
    listed = next(item for item in api.get_missions("test-a", db) if item["mission_id"] == 1)
    assert listed["status"] == "ongoing"
    assert api.start_mission(1, "test-a", db)["success"]
    req = request(db)
    monkeypatch.setattr(security, "decode_integrity_token", lambda token: verdict(req))
    assert api.verify_mission(req, "test-a", db)["success"]
    assert points(db) == 100
    assert not api.verify_mission(req, "test-a", db)["success"]


@pytest.mark.parametrize("stored_status", [None, "not_started", "COMPLETED", " completed ", "cancelled"])
def test_no_started_record_never_implicitly_starts_or_verifies(db, stored_status):
    row = db.query(UserMission).filter_by(user_code="test-a", mission_id=1).one()
    if stored_status is None:
        db.delete(row)
    else:
        row.status = stored_status
    db.commit()
    with pytest.raises(HTTPException) as exc:
        api.create_location_challenge(1, "test-a", db)
    assert exc.value.status_code == 409
    assert points(db) == 0


@pytest.mark.parametrize("field,value", [
    ("latitude", 35.1), ("longitude", 129.03), ("accuracy_m", 10), ("distance_m", 0),
    ("local_passed", "true"), ("photo_url", "https://example.com/a\nb"),
])
def test_location_fields_and_coercion_are_forbidden(field, value):
    values = dict(mission_id=1, mission_type="CURRENT_LOCATION")
    values[field] = value
    with pytest.raises(ValidationError):
        api.MissionVerifyRequestDto(**values)


@pytest.mark.parametrize("section,key,value", [
    ("requestDetails", "requestHash", "wrong"),
    ("requestDetails", "requestPackageName", "other.app"),
    ("requestDetails", "timestampMillis", "1"),
    ("requestDetails", "timestampMillis", "9999999999999"),
    ("appIntegrity", "appRecognitionVerdict", "UNRECOGNIZED_VERSION"),
    ("appIntegrity", "packageName", "other.app"),
    ("appIntegrity", "versionCode", "5"),
    ("appIntegrity", "certificateSha256Digest", ["wrong"]),
    ("deviceIntegrity", "deviceRecognitionVerdict", ["MEETS_BASIC_INTEGRITY"]),
    ("accountDetails", "appLicensingVerdict", "UNLICENSED"),
])
def test_bad_integrity_fails_closed_and_consumes_challenge(db, monkeypatch, section, key, value):
    req = request(db)
    payload = verdict(req)
    payload[section][key] = value
    monkeypatch.setattr(security, "decode_integrity_token", lambda token: payload)
    with pytest.raises(HTTPException) as exc:
        api.verify_mission(req, "test-a", db)
    assert exc.value.status_code == 403
    assert points(db) == 0
    assert db.query(LocationChallenge).one().consumed == 1
    with pytest.raises(HTTPException) as replay:
        api.verify_mission(req, "test-a", db)
    assert replay.value.status_code == 409


@pytest.mark.parametrize("change", ["account", "mission", "expired", "policy", "missing", "false"])
def test_challenge_binding(db, monkeypatch, change):
    req = request(db)
    subject = "test-a"
    if change == "account": subject = "test-b"
    if change == "mission": req.mission_id = 2
    if change == "expired": db.query(LocationChallenge).one().expires_at = datetime.now(timezone.utc).replace(tzinfo=None) - timedelta(seconds=1)
    if change == "policy": db.query(Mission).filter_by(mission_id=1).one().latitude += 0.01
    if change == "missing": req.integrity_token = None
    if change == "false": req.local_passed = False
    db.commit()
    monkeypatch.setattr(security, "decode_integrity_token", lambda token: pytest.fail("Must reject before Google"))
    with pytest.raises(HTTPException):
        api.verify_mission(req, subject, db)
    assert points(db) == 0


def test_challenge_rate_limit_and_no_unconfigured_bypass(db, monkeypatch):
    request(db)
    with pytest.raises(HTTPException) as exc:
        api.create_location_challenge(2, "test-a", db)
    assert exc.value.status_code == 429
    monkeypatch.delenv("PLAY_INTEGRITY_CERTIFICATE_DIGESTS")
    with pytest.raises(HTTPException) as exc:
        api.create_location_challenge(1, "test-b", db)
    assert exc.value.status_code == 503


def test_google_outage_never_rewards(db, monkeypatch):
    req = request(db)
    def unavailable(token):
        raise HTTPException(503, "unavailable")
    monkeypatch.setattr(security, "decode_integrity_token", unavailable)
    with pytest.raises(HTTPException):
        api.verify_mission(req, "test-a", db)
    assert points(db) == 0


def test_overlapping_request_cannot_reuse_claimed_proof(db, monkeypatch):
    req = request(db)
    def overlapping(token):
        with pytest.raises(HTTPException) as replay:
            api.verify_mission(copy.deepcopy(req), "test-a", db)
        assert replay.value.status_code == 409
        return verdict(req)
    monkeypatch.setattr(security, "decode_integrity_token", overlapping)
    assert api.verify_mission(req, "test-a", db)["success"]
    assert points(db) == 100


def test_cancel_during_integrity_check_does_not_award(db, monkeypatch):
    req = request(db)
    def cancel(token):
        db.query(UserMission).filter_by(user_code="test-a", mission_id=1).delete(synchronize_session=False)
        db.commit()
        return verdict(req)
    monkeypatch.setattr(security, "decode_integrity_token", cancel)
    assert not api.verify_mission(req, "test-a", db)["success"]
    assert points(db) == 0


def test_two_distinct_overlapping_proofs_only_reward_once(db, monkeypatch):
    first = request(db)
    def decode(token):
        nonlocal second
        if token == "second-token":
            return verdict(second)
        # While request one is waiting for Google, allow another user action.
        db.query(LocationChallenge).one().issued_at -= timedelta(seconds=16)
        db.commit()
        second = request(db)
        second.integrity_token = "second-token"
        assert api.verify_mission(second, "test-a", db)["success"]
        return verdict(first)
    second = None
    monkeypatch.setattr(security, "decode_integrity_token", decode)
    assert not api.verify_mission(first, "test-a", db)["success"]
    assert points(db) == 100


def test_http_contract_rejects_legacy_coordinates(db):
    app = FastAPI()
    app.include_router(api.router)
    app.dependency_overrides[get_db] = lambda: db
    app.dependency_overrides[api.get_current_user_email] = lambda: "test-a"
    with TestClient(app) as client:
        assert client.post("/api/v1/missions/verify", json={
            "mission_id": 1, "mission_type": "CURRENT_LOCATION", "latitude": 35.1,
        }).status_code == 422
        assert client.post("/api/v1/missions/verify", json={
            "mission_id": 1, "mission_type": "CURRENT_LOCATION", "local_passed": True,
        }).status_code == 400
    assert points(db) == 0


def test_hash_is_shared_with_android():
    req = api.MissionVerifyRequestDto(mission_id=1, mission_type="CURRENT_LOCATION",
        challenge_id="a" * 64, local_passed=True)
    assert security.verification_request_hash(req) == "V-qTQ3l1j5_H9WQceYbK3OvHYmoXLemB_cyhomqIW5E"


def test_upload_strips_exif_and_preserves_image_pixels():
    image = Image.new("RGB", (8, 8), "blue")
    exif = Image.Exif()
    exif[0x010E] = "private metadata"
    exif[0x8825] = {1: "N", 2: (35.0, 6.0, 0.0), 3: "E", 4: (129.0, 1.0, 48.0)}
    stream = io.BytesIO()
    image.save(stream, format="JPEG", exif=exif)
    original = stream.getvalue()
    assert Image.open(io.BytesIO(original)).getexif()
    sanitized = Image.open(io.BytesIO(api._sanitize_uploaded_jpeg(original)))
    assert not sanitized.getexif()
    assert sanitized.size == (8, 8)
    assert sanitized.getpixel((0, 0))[2] > 240


def test_invalid_upload_is_rejected():
    with pytest.raises(HTTPException) as exc:
        api._sanitize_uploaded_jpeg(b"not an image")
    assert exc.value.status_code == 400
