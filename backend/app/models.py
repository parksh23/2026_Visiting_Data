from sqlalchemy import Column, Integer, String, DateTime, Text
from datetime import datetime

from app.database import Base


# =========================
# txt 파일 저장용 모델
# text_files.py에서 사용함
# 이걸 지우면 서버 실행 시 ImportError 발생
# =========================
class TextFile(Base):
    __tablename__ = "TEXT_FILES"

    id = Column("ID", Integer, primary_key=True)
    filename = Column("FILENAME", String(255), nullable=False)
    content = Column("CONTENT", Text, nullable=False)
    created_at = Column("CREATED_AT", DateTime, default=datetime.utcnow)


# =========================
# 사용자 정보 모델
# 기존 users 테이블과 연결
# =========================
class AppUser(Base):
    __tablename__ = "APP_USERS"

    user_code = Column("USER_CODE", String(20), primary_key=True, index=True)

    login_id = Column("LOGIN_ID", String(100), nullable=False, unique=True)

    email = Column("EMAIL", String(255), nullable=False, unique=True, index=True)

    password_hash = Column("PASSWORD_HASH", String(255), nullable=True)

    account_status = Column("ACCOUNT_STATUS", String(30), nullable=False, default="ACTIVE")

    nickname = Column("NICKNAME", String(100), nullable=False)

    level_no = Column("LEVEL_NO", Integer, nullable=False, default=1)

    total_points = Column("TOTAL_POINTS", Integer, nullable=False, default=0)

    completed_missions = Column("COMPLETED_MISSIONS", Integer, nullable=False, default=0)

    saved_missions = Column("SAVED_MISSIONS", Integer, nullable=False, default=0)

    conquered_districts = Column("CONQUERED_DISTRICTS", Integer, nullable=False, default=0)