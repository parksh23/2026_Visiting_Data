from sqlalchemy import Column, Integer, String, DateTime, Text
from datetime import datetime
# 💡 app. 접두어 제거
from database import Base
from pydantic import BaseModel
from typing import List

class TextFile(Base):
    __tablename__ = "TEXT_FILES"

    id = Column("ID", Integer, primary_key=True)
    filename = Column("FILENAME", String(255), nullable=False)
    content = Column("CONTENT", Text, nullable=False)
    created_at = Column("CREATED_AT", DateTime, default=datetime.utcnow)

# 개별 랭킹 항목 모델
class RankingItem(BaseModel):
    RANK_NUM: int
    NICKNAME: str
    TOTAL_POINTS: int
    IS_ME: bool

# 내 랭킹 전용 모델
class MyRankingItem(BaseModel):
    RANK_NUM: int
    NICKNAME: str
    TOTAL_POINTS: int

# 최종 응답 모델
class RankingResponse(BaseModel):
    MY_RANKING: MyRankingItem
    RANKING_LIST: List[RankingItem]