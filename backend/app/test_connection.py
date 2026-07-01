"""
Oracle DB 연결만 단독으로 테스트하는 스크립트.
FastAPI 서버 없이 이것만 실행해서 연결이 되는지 먼저 확인하세요.

실행: python test_connection.py
"""

from database import engine
from sqlalchemy import text

try:
    with engine.connect() as conn:
        # 1. 가장 기본적인 연결 확인
        result = conn.execute(text("SELECT 1 FROM dual"))
        print("[OK] DB 연결 성공:", result.fetchone())

        # 2. 현재 접속한 유저/DB 정보 확인
        result = conn.execute(text("SELECT USER FROM dual"))
        print("[INFO] 접속 계정:", result.fetchone()[0])

        # 3. missions 테이블이 실제로 보이는지 + row 수 확인
        result = conn.execute(text("SELECT COUNT(*) FROM missions"))
        print("[INFO] missions 테이블 row 수:", result.fetchone()[0])

except Exception as e:
    print("[FAIL] DB 연결 실패")
    print(type(e).__name__, "-", e)