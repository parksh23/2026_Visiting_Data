@router.get("/rankings", response_model=RankingResponse)
def get_rankings(
    type: str = Query("all", pattern="^(all|region|friend)$"),
    district: Optional[str] = Query(None, description="지역 랭킹 조회 시 타겟 지역구"),
    subject: str = Depends(get_current_user_email),
    db: Session = Depends(get_db),
):
    user = _get_user(db, subject)
    query = db.query(AppUser).filter(AppUser.account_status == "ACTIVE")

    def _apply_tie_ranks(data_list: List[dict]) -> List[dict]:
        current_rank = 1
        for i, item in enumerate(data_list):
            # 이전 사람보다 점수가 낮을 때만 순위를 갱신 (인덱스 + 1)
            if i > 0 and item["score"] < data_list[i - 1]["score"]:
                current_rank = i + 1
            item["rank"] = current_rank
        return data_list

    if type == "region":
        # 요청된 지역구가 없으면 유저의 기본 지역구로 대체
        target_district = district or user.district_name
        if not target_district:
            return {"myRank": {"rank": 0, "topPercent": 0, "point": 0}, "rankings": []}

        # 1. 타겟 지역구의 완료된 미션 개수를 유저별로 카운트하는 서브쿼리
        mission_counts = (
            db.query(
                UserMission.user_code,
                func.count(UserMission.mission_id).label("mission_count")
            )
            .join(Mission, UserMission.mission_id == Mission.mission_id)
            .filter(
                UserMission.status == "completed",
                Mission.district_name == target_district
            )
            .group_by(UserMission.user_code)
            .subquery()
        )

        users_with_counts = (
            db.query(AppUser, func.coalesce(mission_counts.c.mission_count, 0).label("count"))
            .outerjoin(mission_counts, AppUser.user_code == mission_counts.c.user_code)
            .filter(AppUser.account_status == "ACTIVE")
            .all()
        )

        # 데이터를 딕셔너리 리스트로 통일
        raw_data = [
            {
                "userId": u.user_code,
                "name": u.nickname,
                "score": count,
                "_tie_score": u.total_points # 동점자 처리를 위한 보조 점수 (목록 정렬용)
            }
            for u, count in users_with_counts
        ]
        # 미션 개수(내림차순) -> 총 포인트(내림차순) -> 유저 코드(오름차순) 정렬
        raw_data.sort(key=lambda x: (-x["score"], -x["_tie_score"], x["userId"]))

        # 공동 순위 일괄 적용
        ranked_data = _apply_tie_ranks(raw_data)
        all_ranked_data = ranked_data

    else:
        # 전체 또는 친구 목록 가져오기
        if type == "friend":
            friend_codes = [
                f_code
                for (f_code,) in db.query(Friendship.friend_user_code)
                .filter(Friendship.user_code == user.user_code)
                .all()
            ]
            ranked_users = query.filter(AppUser.user_code.in_([user.user_code, *friend_codes])).all()
        else:
            ranked_users = query.all()

        # 리스트 화면에 보여줄 데이터 세팅 및 정렬
        raw_data = [
            {"userId": u.user_code, "name": u.nickname, "score": u.total_points}
            for u in ranked_users
        ]
        raw_data.sort(key=lambda x: (-x["score"], x["userId"]))

        # 헬퍼 함수로 화면 표시용 목록에 공동 순위 일괄 적용
        ranked_data = _apply_tie_ranks(raw_data)

        # 내 랭킹(myRank) 퍼센티지를 구하기 위해 전체 유저 기준으로 공동 순위 적용
        all_users = query.all()
        all_raw_data = [
            {"userId": u.user_code, "name": u.nickname, "score": u.total_points}
            for u in all_users
        ]
        all_raw_data.sort(key=lambda x: (-x["score"], x["userId"]))

        # 헬퍼 함수 재활용
        all_ranked_data = _apply_tie_ranks(all_raw_data)

    # 3. 내 랭킹 정보 계산 (전체 모수가 담긴 all_ranked_data에서 탐색)
    my_item = next((r for r in all_ranked_data if r["userId"] == user.user_code), None)
    my_rank = my_item["rank"] if my_item else len(all_ranked_data) + 1
    my_score = my_item["score"] if my_item else 0
    top_percent = max(1, math.ceil(my_rank / max(len(all_ranked_data), 1) * 100))

    # 4. 반환 데이터 구성 (지역 랭킹 처리 시 포함했던 _tie_score 등 불필요한 키 제거)
    final_rankings = [
        {
            "rank": r["rank"],
            "userId": r["userId"],
            "name": r["name"],
            "score": r["score"],
        }
        for r in ranked_data
    ]

    return {
        "myRank": {
            "rank": my_rank,
            "topPercent": top_percent,
            "point": my_score,
        },
        "rankings": final_rankings
    }