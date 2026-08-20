import logging

from apscheduler.schedulers.background import BackgroundScheduler
from apscheduler.triggers.cron import CronTrigger
from database import SessionLocal
from notification_jobs import run_new_mission_notifications, run_ranking_notifications
from tourism_scoring import refresh_tourism_scores
from push_notifications import process_pending_pushes

logger = logging.getLogger(__name__)
scheduler = BackgroundScheduler(timezone="Asia/Seoul")

def update_rankings_job():
    db = SessionLocal()
    try:
        result = run_ranking_notifications(db)
        logger.info("랭킹 갱신 및 상승 알림 처리 결과: %s", result)
    except Exception:
        db.rollback()
        logger.exception("랭킹 갱신 및 상승 알림 처리에 실패했습니다.")
    finally:
        db.close()


def new_mission_notifications_job():
    db = SessionLocal()
    try:
        result = run_new_mission_notifications(db)
        logger.info("새 미션 알림 처리 결과: %s", result)
    except Exception:
        db.rollback()
        logger.exception("새 미션 알림 처리에 실패했습니다.")
    finally:
        db.close()


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


def process_pending_pushes_job():
    db = SessionLocal()
    try:
        result = process_pending_pushes(db)
        if result["processed"]:
            logger.info("예약 푸시 처리 결과: %s", result)
    except Exception:
        db.rollback()
        logger.exception("예약 푸시 처리에 실패했습니다.")
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
        new_mission_notifications_job,
        CronTrigger(hour=18, minute=0, timezone="Asia/Seoul"),
        id="daily-new-mission-notifications",
        replace_existing=True,
        max_instances=1,
        coalesce=True,
    )
    scheduler.add_job(
        update_rankings_job,
        CronTrigger(hour=18, minute=10, timezone="Asia/Seoul"),
        id="daily-ranking-notifications",
        replace_existing=True,
        max_instances=1,
        coalesce=True,
    )
    scheduler.add_job(
        process_pending_pushes_job,
        "interval",
        minutes=1,
        id="pending-push-delivery",
        replace_existing=True,
        max_instances=1,
        coalesce=True,
    )
    scheduler.start()


def shutdown_scheduler():
    if scheduler.running:
        scheduler.shutdown(wait=False)
