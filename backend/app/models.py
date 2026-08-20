from datetime import UTC, datetime

from sqlalchemy import (
    CheckConstraint,
    Column,
    DateTime,
    Float,
    ForeignKey,
    Integer,
    String,
    UniqueConstraint,
)

from database import Base


def utc_now() -> datetime:
    """Oracle TIMESTAMP와 호환되는 timezone-naive UTC 시각을 반환한다."""
    return datetime.now(UTC).replace(tzinfo=None)


class AppUser(Base):
    __tablename__ = "APP_USERS"

    user_id = Column("USER_ID", Integer, primary_key=True, autoincrement=True)
    user_code = Column("USER_CODE", String(20), unique=True, index=True)
    login_id = Column("LOGIN_ID", String(50), nullable=True, unique=True)
    email = Column("EMAIL", String(100), nullable=True, unique=True, index=True)
    password_hash = Column("PASSWORD_HASH", String(255), nullable=True)
    account_status = Column("ACCOUNT_STATUS", String(20), nullable=False, default="ACTIVE")
    nickname = Column("NICKNAME", String(50), nullable=False, unique=True)
    level_no = Column("LEVEL_NO", Integer, nullable=False, default=1)
    total_points = Column("TOTAL_POINTS", Integer, nullable=False, default=0)
    completed_missions = Column("COMPLETED_MISSIONS", Integer, nullable=False, default=0)
    saved_missions = Column("SAVED_MISSIONS", String(1000), nullable=True, default="")

    conquered_districts = Column("CONQUERED_DISTRICTS", Integer, nullable=False, default=0)
    created_at = Column("CREATED_AT", DateTime, default=utc_now)
    district_name = Column(
        "DISTRICT_NAME",
        String(100),
        ForeignKey("DISTRICTS.NAME"),
        nullable=True,
    )
    kakao_id = Column("KAKAO_ID", String(255), nullable=True, unique=True, index=True)


class District(Base):
    __tablename__ = "DISTRICTS"
    __table_args__ = (
        CheckConstraint(
            '"NAME" IN ('
            "'강서구','금정구','기장군','남구','동구','동래구','부산진구','북구',"
            "'사상구','사하구','서구','수영구','연제구','영도구','중구','해운대구'"
            ")",
            name="CK_DISTRICTS_BUSAN_NAME",
        ),
    )

    name = Column("NAME", String(30), primary_key=True)


class Mission(Base):
    __tablename__ = "MISSIONS"

    mission_id = Column("MISSION_ID", Integer, primary_key=True)
    mission_type = Column("MISSION_TYPE", String(50), nullable=False)
    mission_category = Column("MISSION_CATEGORY", String(50), nullable=True)
    district_name = Column(
        "REGION_NAME", String(50), ForeignKey("DISTRICTS.NAME"), nullable=False, index=True
    )
    title = Column("MISSION_NAME", String(200), nullable=False)
    difficulty = Column("DIFFICULTY", String(20), nullable=True)
    reward_points = Column("REWARD_POINTS", Integer, nullable=False, default=0)
    image_url = Column("IMAGE_URL", String(500), nullable=True)
    latitude = Column("LATITUDE", Float, nullable=True)
    longitude = Column("LONGITUDE", Float, nullable=True)


class UserMission(Base):
    __tablename__ = "USER_MISSIONS"

    __table_args__ = (
        UniqueConstraint("USER_ID", "MISSION_ID", name="UQ_USER_MISSION"),
    )
    id = Column("MAPPING_ID", Integer, primary_key=True, autoincrement=True)
    user_code = Column(
        "USER_ID", String(50), ForeignKey("APP_USERS.USER_CODE"), nullable=False, index=True
    )
    mission_id = Column(
        "MISSION_ID", Integer, ForeignKey("MISSIONS.MISSION_ID"), nullable=False, index=True
    )
    status = Column("STATUS", String(20), nullable=False, default="completed")
    verified_at = Column("ASSIGNED_AT", DateTime, nullable=False, default=utc_now)


class SavedMission(Base):
    __tablename__ = "SAVED_MISSIONS"
    __table_args__ = (
        UniqueConstraint("USER_CODE", "MISSION_ID", name="UQ_SAVED_MISSION"),
    )

    id = Column("ID", Integer, primary_key=True, autoincrement=True)
    user_code = Column(
        "USER_CODE", String(20), ForeignKey("APP_USERS.USER_CODE"), nullable=False, index=True
    )
    mission_id = Column(
        "MISSION_ID", Integer, ForeignKey("MISSIONS.MISSION_ID"), nullable=False, index=True
    )
    created_at = Column("CREATED_AT", DateTime, nullable=False, default=utc_now)


class UserAgreement(Base):
    __tablename__ = "USER_AGREEMENTS"
    __table_args__ = (
        UniqueConstraint("USER_CODE", "DOCUMENT_SLUG", "VERSION", name="UQ_USER_AGREEMENT"),
        CheckConstraint(
            '"DOCUMENT_SLUG" IN (\'terms\',\'privacy\',\'location\')',
            name="CK_USER_AGREEMENTS_DOCUMENT",
        ),
    )

    id = Column("ID", Integer, primary_key=True, autoincrement=True)
    user_code = Column(
        "USER_CODE", String(20), ForeignKey("APP_USERS.USER_CODE"), nullable=False, index=True
    )
    document_slug = Column("DOCUMENT_SLUG", String(20), nullable=False)
    version = Column("VERSION", String(20), nullable=False)
    agreed_at = Column("AGREED_AT", DateTime, nullable=False)


class Friendship(Base):
    __tablename__ = "FRIENDSHIPS"
    __table_args__ = (
        UniqueConstraint("USER_CODE", "FRIEND_USER_CODE", name="UQ_FRIENDSHIP"),
        CheckConstraint(
            '"USER_CODE" <> "FRIEND_USER_CODE"',
            name="CK_FRIENDSHIP_NOT_SELF",
        ),
    )

    id = Column("ID", Integer, primary_key=True, autoincrement=True)
    user_code = Column(
        "USER_CODE",
        String(20),
        ForeignKey("APP_USERS.USER_CODE"),
        nullable=False,
        index=True,
    )
    friend_user_code = Column(
        "FRIEND_USER_CODE",
        String(20),
        ForeignKey("APP_USERS.USER_CODE"),
        nullable=False,
        index=True,
    )


__all__ = [
    "AppUser",
    "District",
    "Friendship",
    "Mission",
    "SavedMission",
    "UserAgreement",
    "UserMission",
]
