"""Coordinate-free verification protocol. Integrity attests the app, not GPS truth."""
import base64
import hashlib
import hmac
import os
import time

import google.auth
from google.auth.transport.requests import AuthorizedSession
from fastapi import HTTPException


PROTOCOL_VERSION = 2
CHALLENGE_TTL_SECONDS = 300
CHALLENGE_COOLDOWN_SECONDS = 15


def _integrity_config(certificate_env, minimum_version_env, default_minimum_version, lowest_version):
    try:
        project = int(os.environ["PLAY_INTEGRITY_CLOUD_PROJECT_NUMBER"])
        minimum_version = int(os.getenv(minimum_version_env, str(default_minimum_version)))
        certificates = {
            value.strip().rstrip("=")
            for value in os.environ[certificate_env].split(",")
            if value.strip()
        }
        if project <= 0 or minimum_version < lowest_version or not certificates:
            raise ValueError()
        for certificate in certificates:
            if len(base64.urlsafe_b64decode(certificate + "=" * (-len(certificate) % 4))) != 32:
                raise ValueError()
    except (KeyError, ValueError):
        raise HTTPException(503, "위치 인증 보안 설정이 준비되지 않았습니다.") from None
    return project, minimum_version, certificates


def integrity_config():
    """Configuration for Google Play-distributed builds (protocol v1 and PLAY v2)."""
    return _integrity_config(
        "PLAY_INTEGRITY_CERTIFICATE_DIGESTS", "PLAY_INTEGRITY_MIN_VERSION_CODE", 6, 6,
    )


def onestore_integrity_config():
    """Configuration for the separately signed ONE Store build (protocol v2 only)."""
    return _integrity_config(
        "ONESTORE_CERTIFICATE_DIGESTS", "ONESTORE_MIN_VERSION_CODE", 10, 1,
    )


def verification_request_hash(req):
    # Must match VerificationRequestHash.kt. URLs cannot contain line breaks.
    fields = [
        str(req.protocol_version), req.challenge_id or "", str(req.mission_id),
        req.mission_type, "true" if req.local_passed else "false",
        req.photo_url or "", req.receipt_image_url or "",
    ]
    if req.protocol_version >= 2:
        fields.append(req.distribution_channel or "")
    digest = hashlib.sha256("\n".join(fields).encode("utf-8")).digest()
    return base64.urlsafe_b64encode(digest).decode("ascii").rstrip("=")


def decode_integrity_token(token):
    # No token, coordinates, or Google response body is logged or persisted.
    try:
        credentials, _ = google.auth.default(scopes=["https://www.googleapis.com/auth/playintegrity"])
        with AuthorizedSession(credentials) as session:
            response = session.post(
                "https://playintegrity.googleapis.com/v1/kr.co.busanquest:decodeIntegrityToken",
                json={"integrity_token": token}, timeout=20,
            )
            if response.status_code in (400, 401, 403):
                # Credential/permission issues must also fail closed.
                raise HTTPException(403, "앱 보안 확인에 실패했습니다. 공식 스토어 설치본으로 업데이트해주세요.")
            response.raise_for_status()
            return response.json()["tokenPayloadExternal"]
    except HTTPException:
        raise
    except Exception:
        raise HTTPException(503, "앱 보안 확인 서비스에 연결하지 못했습니다. 다시 시도해주세요.") from None


def verify_integrity(req, issued_at):
    channel = "PLAY" if req.protocol_version == 1 else req.distribution_channel
    if channel == "PLAY":
        _, minimum_version, certificates = integrity_config()
    elif req.protocol_version == 2 and channel == "ONESTORE":
        _, minimum_version, certificates = onestore_integrity_config()
    else:
        raise HTTPException(403, "Unsupported app distribution channel.")
    payload = decode_integrity_token(req.integrity_token)
    try:
        details = payload["requestDetails"]
        app = payload["appIntegrity"]
        timestamp = int(details["timestampMillis"]) / 1000
        now = time.time()
        valid_common = (
            details["requestPackageName"] == "kr.co.busanquest"
            and hmac.compare_digest(details["requestHash"], verification_request_hash(req))
            and issued_at.timestamp() - 5 <= timestamp <= now + 5
            and now - timestamp <= 120
            and app["packageName"] == "kr.co.busanquest"
            and int(app["versionCode"]) >= minimum_version
            and bool(certificates.intersection(value.rstrip("=") for value in app["certificateSha256Digest"]))
            and "MEETS_DEVICE_INTEGRITY" in payload["deviceIntegrity"]["deviceRecognitionVerdict"]
        )
        if channel == "PLAY":
            valid_channel = (
                app["appRecognitionVerdict"] == "PLAY_RECOGNIZED"
                and payload["accountDetails"]["appLicensingVerdict"] == "LICENSED"
            )
        else:
            # A genuine ONE Store install is not licensed/recognized by Google Play. The Android
            # client checks ONE Store's StoreEnvironment; the token still binds the request to the
            # known ONE Store signing certificate and a device-integrity verdict.
            valid_channel = app["appRecognitionVerdict"] == "UNRECOGNIZED_VERSION"
        valid = valid_common and valid_channel
    except (KeyError, TypeError, ValueError, AttributeError):
        valid = False
    if not valid:
        raise HTTPException(403, "인증 요청 또는 앱·기기 보안 확인에 실패했습니다. 공식 스토어 설치본으로 다시 시도해주세요.")
