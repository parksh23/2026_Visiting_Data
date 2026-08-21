from datetime import datetime

from sqlalchemy import (
    CheckConstraint,
    Column,
    DateTime,
    Float,
    ForeignKey,
    Integer,
    Sequence,
    String,
    Text,
    UniqueConstraint,
)

from database import Base


class TextFile(Base):
    __tablename__ = "TEXT_FILES"

    id = Column("ID", Integer, Sequence("text_files_seq"), primary_key=True, autoincrement=True)
    filename = Column("FILENAME", String(255), nullable=False)
    content = Column("CONTENT", Text, nullable=False)
    created_at = Column("CREATED_AT", DateTime, default=datetime.utcnow)


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
    created_at = Column("CREATED_AT", DateTime, default=datetime.utcnow)
    district_name = Column(
        "DISTRICT_NAME",
        String(100),
        ForeignKey("DISTRICTS.NAME"),
        nullable=True,
    )
    kakao_id = Column("KAKAO_ID", String(255), nullable=True, unique=True, index=True)
    last_notified_rank = Column("LAST_NOTIFIED_RANK", Integer, nullable=True)


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
    created_at = Column("CREATED_AT", DateTime, nullable=False, default=datetime.utcnow)


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
    verified_at = Column("ASSIGNED_AT", DateTime, nullable=False, default=datetime.utcnow)
    photo_url = Column("PHOTO_URL", String(1000), nullable=True)
    receipt_image_url = Column("RECEIPT_IMAGE_URL", String(1000), nullable=True)


class SavedMission(Base):
    __tablename__ = "SAVED_MISSIONS"

    user_code = Column(
        "USER_CODE",
        String(20),
        ForeignKey("APP_USERS.USER_CODE"),
        primary_key=True,
    )
    mission_id = Column(
        "MISSION_ID",
        Integer,
        ForeignKey("MISSIONS.MISSION_ID"),
        primary_key=True,
    )
    created_at = Column("CREATED_AT", DateTime, nullable=False, default=datetime.utcnow)

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


class AppRanking(Base):
    __tablename__ = "APP_RANKINGS"

    user_code = Column(
        "USER_CODE",
        String(20),
        ForeignKey("APP_USERS.USER_CODE"),
        primary_key=True
    )
    nickname = Column("NICKNAME", String(50), nullable=False)
    total_points = Column("TOTAL_POINTS", Integer, nullable=False, default=0)
    rank_num = Column("RANK_NUM", Integer, nullable=False)


class UserAgreement(Base):
    __tablename__ = "USER_AGREEMENTS"

    id = Column("ID", Integer, primary_key=True, autoincrement=True)
    user_code = Column(
        "USER_CODE",
        String(20),
        ForeignKey("APP_USERS.USER_CODE"),
        nullable=False,
        index=True,
    )
    doc_slug = Column("DOC_SLUG", String(30), nullable=False)
    doc_version = Column("DOC_VERSION", String(20), nullable=False)
    agreed = Column("AGREED", Integer, nullable=False, default=1)
    agreed_at = Column("AGREED_AT", DateTime, nullable=False, default=datetime.utcnow)
    created_at = Column("CREATED_AT", DateTime, nullable=False, default=datetime.utcnow)


class UserSettings(Base):
    __tablename__ = "USER_SETTINGS"

    user_code = Column(
        "USER_CODE",
        String(20),
        ForeignKey("APP_USERS.USER_CODE"),
        primary_key=True,
    )
    mission_result = Column("MISSION_RESULT", Integer, nullable=False, default=1)
    new_mission = Column("NEW_MISSION", Integer, nullable=False, default=1)
    ranking_change = Column("RANKING_CHANGE", Integer, nullable=False, default=0)
    night_mute = Column("NIGHT_MUTE", Integer, nullable=False, default=1)
    marketing = Column("MARKETING", Integer, nullable=False, default=0)
    marketing_agreed_at = Column("MARKETING_AGREED_AT", DateTime, nullable=True)


class PushToken(Base):
    __tablename__ = "PUSH_TOKENS"

    id = Column("ID", Integer, primary_key=True, autoincrement=True)
    user_code = Column(
        "USER_CODE",
        String(20),
        ForeignKey("APP_USERS.USER_CODE"),
        nullable=False,
        index=True,
    )
    token = Column("TOKEN", String(512), nullable=False, unique=True)
    platform = Column("PLATFORM", String(20), nullable=False, default="android")
    created_at = Column("CREATED_AT", DateTime, nullable=False, default=datetime.utcnow)
    updated_at = Column(
        "UPDATED_AT", DateTime, nullable=False, default=datetime.utcnow, onupdate=datetime.utcnow
    )


class PendingPush(Base):
    __tablename__ = "PENDING_PUSHES"

    id = Column("ID", Integer, primary_key=True, autoincrement=True)
    user_code = Column(
        "USER_CODE",
        String(20),
        ForeignKey("APP_USERS.USER_CODE"),
        nullable=False,
        index=True,
    )
    title = Column("TITLE", String(200), nullable=False)
    body = Column("BODY", String(1000), nullable=False)
    notification_type = Column("NOTIFICATION_TYPE", String(30), nullable=False)
    idempotency_key = Column("IDEMPOTENCY_KEY", String(200), nullable=False, unique=True)
    data_json = Column("DATA_JSON", Text, nullable=True)
    scheduled_at = Column("SCHEDULED_AT", DateTime, nullable=False, index=True)
    sent_at = Column("SENT_AT", DateTime, nullable=True)
    status = Column("STATUS", String(20), nullable=False, default="pending")
    attempts = Column("ATTEMPTS", Integer, nullable=False, default=0)
    last_error = Column("LAST_ERROR", String(1000), nullable=True)
    created_at = Column("CREATED_AT", DateTime, nullable=False, default=datetime.utcnow)


class PushDeliveryLog(Base):
    __tablename__ = "PUSH_DELIVERY_LOGS"

    id = Column("ID", Integer, primary_key=True, autoincrement=True)
    idempotency_key = Column("IDEMPOTENCY_KEY", String(200), nullable=False, unique=True)
    user_code = Column(
        "USER_CODE",
        String(20),
        ForeignKey("APP_USERS.USER_CODE"),
        nullable=False,
        index=True,
    )
    notification_type = Column("NOTIFICATION_TYPE", String(30), nullable=False)
    success_count = Column("SUCCESS_COUNT", Integer, nullable=False, default=0)
    failure_count = Column("FAILURE_COUNT", Integer, nullable=False, default=0)
    status = Column("STATUS", String(20), nullable=False)
    error_message = Column("ERROR_MESSAGE", String(1000), nullable=True)
    created_at = Column("CREATED_AT", DateTime, nullable=False, default=datetime.utcnow)


class NotificationJobState(Base):
    __tablename__ = "NOTIFICATION_JOB_STATE"

    job_name = Column("JOB_NAME", String(50), primary_key=True)
    last_run_at = Column("LAST_RUN_AT", DateTime, nullable=False)