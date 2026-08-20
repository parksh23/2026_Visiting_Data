import hashlib
from datetime import datetime, timezone
from typing import Optional

from sqlalchemy.orm import Session

from models import AppRanking, AppUser, Mission, NotificationJobState
from push_notifications import KST, dispatch_notification

NEW_MISSION_JOB = "new-mission-notifications"


def _utc_naive(value: datetime) -> datetime:
    if value.tzinfo is None:
        return value
    return value.astimezone(timezone.utc).replace(tzinfo=None)


def run_new_mission_notifications(
    db: Session, now_kst: Optional[datetime] = None
) -> dict:
    current_kst = now_kst or datetime.now(KST)
    current_utc = _utc_naive(current_kst)
    state = db.get(NotificationJobState, NEW_MISSION_JOB)
    if state is None:
        db.add(NotificationJobState(job_name=NEW_MISSION_JOB, last_run_at=current_utc))
        db.commit()
        return {"initialized": True, "missions": 0, "users": 0, "notifications": 0}

    missions = (
        db.query(Mission)
        .filter(Mission.created_at > state.last_run_at, Mission.created_at <= current_utc)
        .order_by(Mission.created_at, Mission.mission_id)
        .all()
    )
    if not missions:
        state.last_run_at = current_utc
        db.commit()
        return {"initialized": False, "missions": 0, "users": 0, "notifications": 0}

    users = db.query(AppUser).filter(AppUser.account_status == "ACTIVE").all()
    notifications = 0
    for user in users:
        targets = (
            [mission for mission in missions if mission.district_name == user.district_name]
            if user.district_name
            else missions
        )
        if not targets:
            continue
        first = targets[0]
        body = (
            f"‘{first.title}’ 미션이 추가됐어요."
            if len(targets) == 1
            else f"‘{first.title}’ 외 {len(targets) - 1}개 미션이 추가됐어요."
        )
        mission_ids = ",".join(str(item.mission_id) for item in targets)
        digest = hashlib.sha256(mission_ids.encode("utf-8")).hexdigest()[:16]
        result = dispatch_notification(
            db=db,
            user_code=user.user_code,
            notification_type="NEW_MISSION",
            title="새 미션이 열렸어요",
            body=body,
            data={"mission_id": str(first.mission_id)},
            idempotency_key=f"new-mission:{user.user_code}:{digest}",
            now_kst=current_kst,
        )
        if result not in {"disabled", "duplicate"}:
            notifications += 1
    state.last_run_at = current_utc
    db.commit()
    return {
        "initialized": False,
        "missions": len(missions),
        "users": len(users),
        "notifications": notifications,
    }


def run_ranking_notifications(
    db: Session, now_kst: Optional[datetime] = None
) -> dict:
    current_kst = now_kst or datetime.now(KST)
    users = (
        db.query(AppUser)
        .filter(AppUser.account_status == "ACTIVE")
        .order_by(AppUser.total_points.desc(), AppUser.user_code)
        .all()
    )
    db.query(AppRanking).delete(synchronize_session=False)

    previous_score = None
    current_rank = 0
    notifications = 0
    for index, user in enumerate(users, start=1):
        if previous_score is None or user.total_points < previous_score:
            current_rank = index
        previous_score = user.total_points
        old_rank = user.last_notified_rank
        db.add(
            AppRanking(
                user_code=user.user_code,
                nickname=user.nickname,
                total_points=user.total_points,
                rank_num=current_rank,
            )
        )
        if old_rank is not None and current_rank < old_rank:
            moved = old_rank - current_rank
            result = dispatch_notification(
                db=db,
                user_code=user.user_code,
                notification_type="RANKING_CHANGE",
                title="랭킹이 올랐어요",
                body=f"{old_rank}위 → {current_rank}위 ({moved}계단 상승)",
                data={},
                idempotency_key=(
                    f"ranking:{current_kst.date().isoformat()}:{user.user_code}:"
                    f"{old_rank}:{current_rank}"
                ),
                now_kst=current_kst,
            )
            if result not in {"disabled", "duplicate"}:
                notifications += 1
        user.last_notified_rank = current_rank
    db.commit()
    return {"users": len(users), "notifications": notifications}
