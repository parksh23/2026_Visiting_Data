import logging

from apscheduler.schedulers.background import BackgroundScheduler
from apscheduler.triggers.cron import CronTrigger
from sqlalchemy import text
from database import SessionLocal
from tourism_scoring import refresh_tourism_scores

logger = logging.getLogger(__name__)
scheduler = BackgroundScheduler(timezone="Asia/Seoul")

def update_rankings_job():
    db = SessionLocal()
    try:
        # 1. 기존 데이터를 통째로 날립니다 (속도가 매우 빠름)
        db.execute(text("TRUNCATE TABLE APP_RANKINGS"))

        # 2. APP_USERS 테이블에서 순위를 계산한 뒤 APP_RANKINGS에 한 번에 삽입합니다
        insert_query = text("""
            INSERT INTO APP_RANKINGS (USER_CODE, NICKNAME, TOTAL_POINTS, RANK_NUM)
            SELECT
                USER_CODE,
                NICKNAME,
                TOTAL_POINTS,
                RANK() OVER (ORDER BY TOTAL_POINTS DESC, USER_CODE ASC) AS RANK_NUM
            FROM APP_USERS
        """)
        db.execute(insert_query)
        db.commit()
        print("✅ [배치] 랭킹 업데이트 성공")

    except Exception as e:
        db.rollback()
        print(f"❌ [배치] 랭킹 업데이트 실패: {e}")
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
    scheduler.start()


def shutdown_scheduler():
    if scheduler.running:
        scheduler.shutdown(wait=False)
