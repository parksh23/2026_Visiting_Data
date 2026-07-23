from datetime import datetime

from sqlalchemy import (
    Column,
    DateTime,
    Float,
    ForeignKey,
    Integer,
    String,
    Text,
    UniqueConstraint,
)

from database import Base


class TextFile(Base):
    __tablename__ = "TEXT_FILES"

    id = Column("ID", Integer, primary_key=True)
    filename = Column("FILENAME", String(255), nullable=False)
    content = Column("CONTENT", Text, nullable=False)
    created_at = Column("CREATED_AT", DateTime, default=datetime.utcnow)


class AppUser(Base):
    __tablename__ = "APP_USERS"

    user_code = Column("USER_CODE", String(20), primary_key=True, index=True)
    login_id = Column("LOGIN_ID", String(100), nullable=True, unique=True)
    email = Column("EMAIL", String(255), nullable=True, unique=True, index=True)
    password_hash = Column("PASSWORD_HASH", String(255), nullable=True)
    kakao_id = Column("KAKAO_ID", String(100), nullable=True, unique=True, index=True)
    account_status = Column("ACCOUNT_STATUS", String(30), nullable=False, default="ACTIVE")
    nickname = Column("NICKNAME", String(100), nullable=False)
    district_name = Column("DISTRICT_NAME", String(30), nullable=True)
    level_no = Column("LEVEL_NO", Integer, nullable=False, default=1)
    total_points = Column("TOTAL_POINTS", Integer, nullable=False, default=0)
    completed_missions = Column("COMPLETED_MISSIONS", Integer, nullable=False, default=0)
    saved_missions = Column("SAVED_MISSIONS", Integer, nullable=False, default=0)
    conquered_districts = Column("CONQUERED_DISTRICTS", Integer, nullable=False, default=0)


class District(Base):
    __tablename__ = "DISTRICTS"

    name = Column("NAME", String(30), primary_key=True)


class Mission(Base):
    __tablename__ = "MISSIONS"

    mission_id = Column("MISSION_ID", Integer, primary_key=True)
    title = Column("TITLE", String(255), nullable=False)
    district_name = Column(
        "DISTRICT_NAME", String(30), ForeignKey("DISTRICTS.NAME"), nullable=False, index=True
    )
    location = Column("LOCATION", String(255), nullable=False)
    latitude = Column("LATITUDE", Float, nullable=True)
    longitude = Column("LONGITUDE", Float, nullable=True)
    radius_m = Column("RADIUS_M", Integer, nullable=False, default=300)
    reward_points = Column("REWARD_POINTS", Integer, nullable=False, default=0)
    mission_type = Column("MISSION_TYPE", String(30), nullable=False)
    image_url = Column("IMAGE_URL", String(1000), nullable=True)


class UserMission(Base):
    __tablename__ = "USER_MISSIONS"
    __table_args__ = (
        UniqueConstraint("USER_CODE", "MISSION_ID", name="UQ_USER_MISSION"),
    )

    id = Column("ID", Integer, primary_key=True, autoincrement=True)
    user_code = Column(
        "USER_CODE", String(20), ForeignKey("APP_USERS.USER_CODE"), nullable=False, index=True
    )
    mission_id = Column(
        "MISSION_ID", Integer, ForeignKey("MISSIONS.MISSION_ID"), nullable=False, index=True
    )
    status = Column("STATUS", String(30), nullable=False, default="completed")
    verified_at = Column("VERIFIED_AT", DateTime, nullable=False, default=datetime.utcnow)
    photo_url = Column("PHOTO_URL", String(1000), nullable=True)
    receipt_image_url = Column("RECEIPT_IMAGE_URL", String(1000), nullable=True)
