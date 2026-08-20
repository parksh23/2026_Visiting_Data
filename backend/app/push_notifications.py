import json
import logging
import os
from dataclasses import dataclass
from datetime import datetime, time, timedelta, timezone
from pathlib import Path
from typing import Optional
from zoneinfo import ZoneInfo

from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from models import (
    PendingPush,
    PushDeliveryLog,
    PushToken,
    UserSettings,
)

logger = logging.getLogger(__name__)
KST = ZoneInfo("Asia/Seoul")
CHANNELS = {"NEW_MISSION": "new_mission", "RANKING_CHANGE": "ranking_change"}


@dataclass(frozen=True)
class DeliveryResult:
    success_count: int = 0
    failure_count: int = 0
    error_message: Optional[str] = None

    @property
    def status(self) -> str:
        if self.success_count and self.failure_count:
            return "partial"
        if self.success_count:
            return "sent"
        return "failed"


def _firebase_messaging():
    """Initialize Firebase lazily; credentials never live in the repository."""
    try:
        import firebase_admin
        from firebase_admin import credentials, messaging
    except ImportError:
        logger.warning("firebase-admin is not installed; push delivery is disabled.")
        return None

    if not firebase_admin._apps:
        credentials_json = os.getenv("FIREBASE_CREDENTIALS_JSON")
        credentials_path = os.getenv("GOOGLE_APPLICATION_CREDENTIALS")
        try:
            if credentials_json:
                firebase_admin.initialize_app(
                    credentials.Certificate(json.loads(credentials_json))
                )
            elif credentials_path:
                firebase_admin.initialize_app(
                    credentials.Certificate(str(Path(credentials_path)))
                )
            else:
                firebase_admin.initialize_app()
        except Exception:
            logger.exception("Firebase credentials could not be initialized.")
            return None
    return messaging


def _is_invalid_token_error(exception: Exception) -> bool:
    name = exception.__class__.__name__.lower()
    code = str(getattr(exception, "code", "")).lower()
    message = str(exception).lower()
    return (
        "unregistered" in name
        or "invalidargument" in name
        or "unregistered" in code
        or "invalid-argument" in code
        or "requested entity was not found" in message
        or "registration token is not a valid" in message
    )


def send_to_user(
    db: Session,
    user_code: str,
    notification_type: str,
    title: str,
    body: str,
    data: Optional[dict] = None,
) -> DeliveryResult:
    token_rows = db.query(PushToken).filter(PushToken.user_code == user_code).all()
    if not token_rows:
        return DeliveryResult(error_message="registered token not found")
    messaging = _firebase_messaging()
    if messaging is None:
        return DeliveryResult(
            failure_count=len(token_rows), error_message="firebase is not configured"
        )

    channel_id = CHANNELS[notification_type]
    payload = {str(key): str(value) for key, value in (data or {}).items()}
    payload["type"] = notification_type
    message = messaging.MulticastMessage(
        tokens=[row.token for row in token_rows],
        notification=messaging.Notification(title=title, body=body),
        android=messaging.AndroidConfig(
            priority="high",
            collapse_key=channel_id,
            notification=messaging.AndroidNotification(
                channel_id=channel_id,
                priority="default",
            ),
        ),
        data=payload,
    )
    try:
        response = messaging.send_each_for_multicast(message)
    except Exception as exc:
        logger.exception("FCM delivery failed for user %s.", user_code)
        return DeliveryResult(
            failure_count=len(token_rows), error_message=str(exc)[:1000]
        )

    invalid_rows = []
    errors = []
    for token_row, item in zip(token_rows, response.responses):
        if item.success:
            continue
        if item.exception is not None:
            errors.append(str(item.exception))
            if _is_invalid_token_error(item.exception):
                invalid_rows.append(token_row)
    for token_row in invalid_rows:
        db.delete(token_row)
    if invalid_rows:
        db.commit()
    return DeliveryResult(
        success_count=response.success_count,
        failure_count=response.failure_count,
        error_message="; ".join(errors)[:1000] or None,
    )


def _next_kst_eight(now_kst: datetime) -> datetime:
    target = datetime.combine(now_kst.date(), time(hour=8), tzinfo=KST)
    if now_kst.time() >= time(hour=21):
        target += timedelta(days=1)
    return target.astimezone(timezone.utc).replace(tzinfo=None)


def _setting_enabled(settings: Optional[UserSettings], notification_type: str) -> bool:
    field = CHANNELS[notification_type]
    default = notification_type == "NEW_MISSION"
    return default if settings is None else bool(getattr(settings, field))


def _claim_delivery(
    db: Session, idempotency_key: str, user_code: str, notification_type: str
) -> bool:
    db.add(
        PushDeliveryLog(
            idempotency_key=idempotency_key,
            user_code=user_code,
            notification_type=notification_type,
            status="processing",
        )
    )
    try:
        db.commit()
        return True
    except IntegrityError:
        db.rollback()
        return False


def _log_delivery(
    db: Session,
    idempotency_key: str,
    user_code: str,
    notification_type: str,
    result: DeliveryResult,
    status: Optional[str] = None,
) -> None:
    row = (
        db.query(PushDeliveryLog)
        .filter(PushDeliveryLog.idempotency_key == idempotency_key)
        .first()
    )
    if row is None:
        row = PushDeliveryLog(
            idempotency_key=idempotency_key,
            user_code=user_code,
            notification_type=notification_type,
        )
        db.add(row)
    row.success_count = result.success_count
    row.failure_count = result.failure_count
    row.status = status or result.status
    row.error_message = result.error_message
    db.commit()


def dispatch_notification(
    db: Session,
    user_code: str,
    notification_type: str,
    title: str,
    body: str,
    data: dict,
    idempotency_key: str,
    now_kst: Optional[datetime] = None,
) -> str:
    if notification_type not in CHANNELS:
        raise ValueError(f"unsupported notification type: {notification_type}")
    if not _claim_delivery(db, idempotency_key, user_code, notification_type):
        return "duplicate"

    settings = db.get(UserSettings, user_code)
    if not _setting_enabled(settings, notification_type):
        _log_delivery(
            db,
            idempotency_key,
            user_code,
            notification_type,
            DeliveryResult(error_message="disabled by user setting"),
            status="skipped",
        )
        return "disabled"

    current_kst = now_kst or datetime.now(KST)
    night_mute = True if settings is None else bool(settings.night_mute)
    if night_mute and (
        current_kst.time() >= time(hour=21) or current_kst.time() < time(hour=8)
    ):
        db.add(
            PendingPush(
                user_code=user_code,
                title=title,
                body=body,
                notification_type=notification_type,
                idempotency_key=idempotency_key,
                data_json=json.dumps(data, ensure_ascii=False),
                scheduled_at=_next_kst_eight(current_kst),
            )
        )
        try:
            db.commit()
        except IntegrityError:
            db.rollback()
            _log_delivery(
                db,
                idempotency_key,
                user_code,
                notification_type,
                DeliveryResult(error_message="pending queue conflict"),
            )
            return "duplicate"
        _log_delivery(
            db,
            idempotency_key,
            user_code,
            notification_type,
            DeliveryResult(),
            status="queued",
        )
        return "queued"

    result = send_to_user(db, user_code, notification_type, title, body, data)
    _log_delivery(db, idempotency_key, user_code, notification_type, result)
    return result.status


def process_pending_pushes(db: Session, limit: int = 100) -> dict:
    now = datetime.utcnow()
    rows = (
        db.query(PendingPush)
        .filter(PendingPush.status == "pending", PendingPush.scheduled_at <= now)
        .order_by(PendingPush.scheduled_at)
        .limit(limit)
        .with_for_update(skip_locked=True)
        .all()
    )
    for row in rows:
        row.status = "processing"
    db.commit()
    sent = 0
    failed = 0
    for row in rows:
        try:
            data = json.loads(row.data_json) if row.data_json else {}
        except json.JSONDecodeError:
            data = {}
        result = send_to_user(
            db,
            row.user_code,
            row.notification_type,
            row.title,
            row.body,
            data,
        )
        row.attempts += 1
        row.last_error = result.error_message
        if result.success_count:
            row.status = "sent"
            row.sent_at = now
            sent += 1
            _log_delivery(
                db,
                row.idempotency_key,
                row.user_code,
                row.notification_type,
                result,
            )
        elif row.attempts >= 3:
            row.status = "failed"
            failed += 1
            _log_delivery(
                db,
                row.idempotency_key,
                row.user_code,
                row.notification_type,
                result,
            )
        else:
            row.status = "pending"
    db.commit()
    return {"processed": len(rows), "sent": sent, "failed": failed}
