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

    id = Column("ID", Integer, Sequence("text_files_seq"), primary_key=True)
    filename = Column("FILENAME", String(255), nullable=False)
    content = Column("CONTENT", Text, nullable=False)
    created_at = Column("CREATED_AT", DateTime, default=datetime.utcnow)