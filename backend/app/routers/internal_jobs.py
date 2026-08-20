import hmac
import os

from fastapi import APIRouter, Depends, Header, HTTPException
from sqlalchemy.orm import Session

from database import get_db
from notification_jobs import run_new_mission_notifications, run_ranking_notifications
from push_notifications import process_pending_pushes

router = APIRouter(prefix="/internal/jobs", tags=["internal-jobs"])


def _authorize_job(
    x_job_key: str | None = Header(default=None, alias="X-Job-Key"),
) -> None:
    configured = os.getenv("INTERNAL_JOB_KEY")
    if not configured:
        raise HTTPException(status_code=503, detail="내부 작업 키가 설정되지 않았습니다.")
    if not x_job_key or not hmac.compare_digest(x_job_key, configured):
        raise HTTPException(status_code=403, detail="내부 작업 인증에 실패했습니다.")


@router.post("/new-mission-notifications")
def trigger_new_mission_notifications(
    _: None = Depends(_authorize_job), db: Session = Depends(get_db)
):
    return run_new_mission_notifications(db)


@router.post("/ranking-notifications")
def trigger_ranking_notifications(
    _: None = Depends(_authorize_job), db: Session = Depends(get_db)
):
    return run_ranking_notifications(db)


@router.post("/pending-pushes")
def trigger_pending_pushes(
    _: None = Depends(_authorize_job), db: Session = Depends(get_db)
):
    return process_pending_pushes(db)
