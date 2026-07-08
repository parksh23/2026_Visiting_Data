from app.database import engine
from sqlalchemy import text

with engine.connect() as conn:
    result = conn.execute(text("SELECT 'Oracle connected' FROM dual"))
    print(result.fetchone())