from sqlalchemy import Column, Integer, String, DateTime, Text
from datetime import datetime
from app.database import Base


class TextFile(Base):
    __tablename__ = "TEXT_FILES"

    id = Column("ID", Integer, primary_key=True)
    filename = Column("FILENAME", String(255), nullable=False)
    content = Column("CONTENT", Text, nullable=False)
    created_at = Column("CREATED_AT", DateTime, default=datetime.utcnow)