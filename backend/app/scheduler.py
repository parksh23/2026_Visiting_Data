import logging
import time
from pathlib import Path

from apscheduler.schedulers.background import BackgroundScheduler
from apscheduler.triggers.cron import CronTrigger
from database import SessionLocal
from tourism_scoring import refresh_tourism_scores

logger = logging.getLogger(__name__)
scheduler = BackgroundScheduler(timezone="Asia/Seoul")
UPLOAD_DIR = Path(__file__).resolve().parents[1] / "uploads"
UPLOAD_RETENTION_SECONDS = 24 * 60 * 60


def cleanup_stale_uploads_job():
    """인증 요청으로 이어지지 않은 임시 JPG를 24시간 뒤 삭제한다."""
    if not UPLOAD_DIR.exists():
        return
    cutoff = time.time() - UPLOAD_RETENTION_SECONDS
    removed = 0
    for path in UPLOAD_DIR.glob("*.jpg"):
        try:
            if path.stat().st_mtime < cutoff:
                path.unlink()
                removed += 1
        except OSError:
            logger.exception("임시 업로드 삭제 실패: %s", path.name)
    if removed:
        logger.info("만료된 임시 업로드 %s개 삭제", removed)

def refresh_tourism_scores_job():
    db = SessionLocal()
    try:
        result = refresh_tourism_scores(db)
        logger.info("관광지수 점수 월별 갱신 성공: %s", result)
    except Exception:
        db.rollback()
        logger.exception("관광지수 점수 월별 갱신 실패; 기존 점수를 유지합니다.")
    finally:
        db.close()


def start_scheduler():
    if scheduler.running:
        return
    scheduler.add_job(
        refresh_tourism_scores_job,
        CronTrigger(day=1, hour=3, minute=0, timezone="Asia/Seoul"),
        id="monthly-tourism-score-refresh",
        replace_existing=True,
        max_instances=1,
        coalesce=True,
    )
    scheduler.add_job(
        cleanup_stale_uploads_job,
        "interval",
        hours=1,
        id="stale-upload-cleanup",
        replace_existing=True,
        max_instances=1,
        coalesce=True,
    )
    scheduler.start()


def shutdown_scheduler():
    if scheduler.running:
        scheduler.shutdown(wait=False)
