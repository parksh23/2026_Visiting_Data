import hmac
import logging
import os
from typing import Optional

from fastapi import APIRouter, Depends, Header, HTTPException, Query
from sqlalchemy.orm import Session

from database import get_db
from notification_jobs import run_new_mission_notifications, run_ranking_notifications
from push_notifications import process_pending_pushes
from tourism_scoring import refresh_tourism_scores

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/internal/jobs", tags=["internal-jobs"])


def _authorize_job(
    x_job_key: str | None = Header(default=None, alias="X-Job-Key"),
) -> None:
    configured = os.getenv("INTERNAL_JOB_KEY")
    if not configured:
        raise HTTPException(status_code=503, detail="내부 작업 키가 설정되지 않았습니다.")
    if not x_job_key or not hmac.compare_digest(x_job_key, configured):
        raise HTTPException(status_code=403, detail="내부 작업 인증에 실패했습니다.")


def _flush_pending_pushes(db: Session) -> dict:
    """외부 크론이 서버를 깨운 김에 보류 푸시도 함께 처리한다.

    보류 푸시 전용 크론을 1분 간격으로 돌리면 Render 무료 인스턴스가
    항상 깨어 있게 되고, Render 측에서 429(Too Many Requests)로 차단한다.
    그래서 일일 작업 끝에 함께 처리하고, 전용 크론은 하루 1회(08:05)만 둔다.
    """
    try:
        return process_pending_pushes(db)
    except Exception:
        db.rollback()
        logger.exception("일일 작업 중 보류 푸시 처리에 실패했습니다.")
        return {"processed": 0, "sent": 0, "failed": 0, "error": True}


@router.post("/new-mission-notifications")
def trigger_new_mission_notifications(
    _: None = Depends(_authorize_job), db: Session = Depends(get_db)
):
    result = run_new_mission_notifications(db)
    return {**result, "pending_pushes": _flush_pending_pushes(db)}


@router.post("/ranking-notifications")
def trigger_ranking_notifications(
    _: None = Depends(_authorize_job), db: Session = Depends(get_db)
):
    result = run_ranking_notifications(db)
    return {**result, "pending_pushes": _flush_pending_pushes(db)}


@router.post("/pending-pushes")
def trigger_pending_pushes(
    _: None = Depends(_authorize_job), db: Session = Depends(get_db)
):
    return process_pending_pushes(db)


@router.post("/tourism-scores")
def trigger_tourism_scores(
    base_ym: Optional[str] = Query(default=None, pattern=r"^\d{6}$"),
    _: None = Depends(_authorize_job),
    db: Session = Depends(get_db),
):
    """월별 관광지수 갱신. 외부 크론은 X-Job-Key만으로 본문 없이 호출한다."""
    try:
        return refresh_tourism_scores(db, base_ym)
    except (RuntimeError, ValueError) as exc:
        db.rollback()
        raise HTTPException(status_code=400, detail=str(exc)) from exc
    except Exception as exc:
        db.rollback()
        logger.exception("관광지수 갱신 실패")
        raise HTTPException(
            status_code=502,
            detail="관광지수 갱신에 실패해 기존 점수를 유지합니다.",
        ) from exc
