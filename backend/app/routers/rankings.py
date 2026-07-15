from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.orm import Session
from sqlalchemy import text
from database import get_db
from models import RankingResponse, RankingItem, MyRankingItem

router = APIRouter(prefix="/api/v1/rankings", tags=["Rankings"])

@router.get("", response_model=RankingResponse)
def get_rankings(
    user_code: str = Query(..., description="조회할 유저의 코드를 입력하세요."),
    db: Session = Depends(get_db)
):
    # 1. 랭킹 테이블에서 상위 50명 조회
    top_50_query = text("""
        SELECT USER_CODE, NICKNAME, TOTAL_POINTS, RANK_NUM
        FROM APP_RANKINGS
        WHERE RANK_NUM <= 50
        ORDER BY RANK_NUM ASC
    """)
    top_50_rows = db.execute(top_50_query).fetchall()

# 2. 내 랭킹 정보 조회 (고정값이 아닌, 안드로이드가 보낸 user_code를 사용!)
    my_ranking_query = text("""
        SELECT RANK_NUM, NICKNAME, TOTAL_POINTS
        FROM APP_RANKINGS
        WHERE USER_CODE = :user_code
    """)
    my_row = db.execute(my_ranking_query, {"user_code": user_code}).fetchone()

    # 아직 랭킹에 없는 신규 유저일 경우 0점 처리
    if not my_row:
        my_ranking_data = MyRankingItem(RANK_NUM=0, NICKNAME="신규유저", TOTAL_POINTS=0)
    else:
        my_ranking_data = MyRankingItem(
            RANK_NUM=my_row[0],
            NICKNAME=my_row[1],
            TOTAL_POINTS=my_row[2]
        )

    # 3. 내 순위 강조를 위한 IS_ME 처리
    ranking_list = []
    for row in top_50_rows:
        is_me = (row[0] == user_code)
        ranking_list.append(
            RankingItem(
                RANK_NUM=row[3],
                NICKNAME=row[1],
                TOTAL_POINTS=row[2],
                IS_ME=is_me
            )
        )

    # 4. JSON 반환
    return RankingResponse(
        MY_RANKING=my_ranking_data,
        RANKING_LIST=ranking_list
    )