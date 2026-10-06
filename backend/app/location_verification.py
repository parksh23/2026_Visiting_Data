"""Coordinate-free verification protocol. Integrity attests the app, not GPS truth."""
import base64
import hashlib
import hmac
import logging
import os
import time

import google.auth
from google.auth.transport.requests import AuthorizedSession
from fastapi import HTTPException


PROTOCOL_VERSION = 2
CHALLENGE_TTL_SECONDS = 300
CHALLENGE_COOLDOWN_SECONDS = 15
logger = logging.getLogger(__name__)


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


def _google_error_codes(response):
    """Return only allowlisted codes; never log free text, metadata or tokens."""
    statuses = {"PERMISSION_DENIED", "UNAUTHENTICATED", "INVALID_ARGUMENT",
                "RESOURCE_EXHAUSTED", "NOT_FOUND", "INTERNAL", "UNAVAILABLE"}
    reasons = {"SERVICE_DISABLED", "ACCESS_TOKEN_SCOPE_INSUFFICIENT",
               "IAM_PERMISSION_DENIED", "CONSUMER_INVALID", "CONSUMER_SUSPENDED",
               "BILLING_DISABLED", "SECURITY_POLICY_VIOLATED", "RATE_LIMIT_EXCEEDED",
               "API_KEY_INVALID", "ACCESS_TOKEN_EXPIRED", "ACCESS_TOKEN_TYPE_UNSUPPORTED",
               "accessNotConfigured", "forbidden", "insufficientPermissions",
               "permissionDenied", "authError", "quotaExceeded", "rateLimitExceeded"}
    try:
        error = response.json().get("error", {})
        raw_status = error.get("status")
        status = raw_status if isinstance(raw_status, str) and raw_status in statuses else "UNKNOWN"
        found = set()
        for field in ("details", "errors"):
            entries = error.get(field, [])
            if not isinstance(entries, list):
                continue
            for entry in entries:
                reason = entry.get("reason") if isinstance(entry, dict) else None
                if isinstance(reason, str):
                    found.add(reason if reason in reasons else "OTHER")
        return status, ",".join(sorted(found)) or "NOT_PROVIDED"
    except Exception:
        # Diagnostics must not change how an upstream error is handled.
        return "UNREADABLE", "NOT_PROVIDED"


def decode_integrity_token(token):
    # No token, coordinates, or Google response body is logged or persisted.
    stage = "credentials"
    try:
        credentials, _ = google.auth.default(scopes=["https://www.googleapis.com/auth/playintegrity"])
        stage = "google_request"
        with AuthorizedSession(credentials) as session:
            response = session.post(
                "https://playintegrity.googleapis.com/v1/kr.co.busanquest:decodeIntegrityToken",
                json={"integrity_token": token}, timeout=20,
            )
            if response.status_code >= 400:
                error_status, reasons = _google_error_codes(response)
                logger.warning(
                    "location_integrity stage=google_response status=%s google_status=%s reasons=%s",
                    response.status_code, error_status, reasons,
                )
            if response.status_code in (400, 401, 403):
                # Credential/permission issues must also fail closed.
                raise HTTPException(403, "앱 보안 확인에 실패했습니다. 공식 스토어 설치본으로 업데이트해주세요.")
            response.raise_for_status()
            stage = "google_payload"
            return response.json()["tokenPayloadExternal"]
    except HTTPException:
        raise
    except Exception as exc:
        # Exception messages and tracebacks may contain credentials or response data.
        logger.warning("location_integrity stage=%s error_type=%s", stage, type(exc).__name__)
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
    now = time.time()
    # Evaluate independently so one missing field does not hide other failures.
    # Only static check names are logged, never values from the payload/request.
    checks = {
        "request_package": lambda: payload["requestDetails"]["requestPackageName"] == "kr.co.busanquest",
        "request_hash": lambda: hmac.compare_digest(payload["requestDetails"]["requestHash"], verification_request_hash(req)),
        "timestamp_window": lambda: issued_at.timestamp() - 5 <= int(payload["requestDetails"]["timestampMillis"]) / 1000 <= now + 5,
        "timestamp_age": lambda: now - int(payload["requestDetails"]["timestampMillis"]) / 1000 <= 120,
        "app_package": lambda: payload["appIntegrity"]["packageName"] == "kr.co.busanquest",
        "app_version": lambda: int(payload["appIntegrity"]["versionCode"]) >= minimum_version,
        "certificate": lambda: bool(certificates.intersection(value.rstrip("=") for value in payload["appIntegrity"]["certificateSha256Digest"])),
        "device_integrity": lambda: "MEETS_DEVICE_INTEGRITY" in payload["deviceIntegrity"]["deviceRecognitionVerdict"],
        "app_recognition": lambda: payload["appIntegrity"]["appRecognitionVerdict"] == ("PLAY_RECOGNIZED" if channel == "PLAY" else "UNRECOGNIZED_VERSION"),
    }
    if channel == "PLAY":
        checks["play_license"] = lambda: payload["accountDetails"]["appLicensingVerdict"] == "LICENSED"
    failures = []
    for name, check in checks.items():
        try:
            if not check():
                failures.append(name)
        except (KeyError, TypeError, ValueError, AttributeError):
            failures.append(name + ":missing_or_invalid")
    if failures:
        logger.warning("location_integrity stage=verdict channel=%s failed_checks=%s", channel, ",".join(failures))
        raise HTTPException(403, "인증 요청 또는 앱·기기 보안 확인에 실패했습니다. 공식 스토어 설치본으로 다시 시도해주세요.")
