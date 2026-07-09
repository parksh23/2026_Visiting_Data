from sqlalchemy import Column, Integer, String
from database import Base


class Mission(Base):
    """
    DBeaver로 넣으신 34개 부산 미션 테이블과 매핑됩니다.
    🚨 주의: 아래 __tablename__ 과 컬럼명은 실제 Oracle 테이블 구조와
    반드시 일치해야 합니다 (대소문자는 SQLAlchemy가 자동으로 대문자 변환해서 처리하므로
    보통은 신경 쓰지 않아도 되지만, 테이블/컬럼명 자체(spelling)는 정확히 맞아야 합니다).

    DBeaver에서 아래 쿼리로 실제 컬럼명을 확인해보세요:
        SELECT column_name, data_type FROM user_tab_columns WHERE table_name = 'MISSIONS';
    """

    __tablename__ = "missions"

    mission_id = Column(Integer, primary_key=True, index=True)
    mission_type = Column(String(50))
    region_name = Column(String(50))
    mission_name = Column(String(200))
    difficulty = Column(String(20), nullable=True)
    base_score = Column(Integer, nullable=True)
    final_score = Column(Integer, nullable=True)