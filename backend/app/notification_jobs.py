def run_ranking_notifications(
    db: Session, now_kst: Optional[datetime] = None
) -> dict:
    current_kst = now_kst or datetime.now(KST)
    users = (
        db.query(AppUser)
        .filter(AppUser.account_status == "ACTIVE")
        .order_by(AppUser.total_points.desc(), AppUser.user_code)
        .all()
    )

    db.query(AppRanking).delete(synchronize_session=False)
    db.commit()

    previous_score = None
    current_rank = 0
    notifications = 0
    for index, user in enumerate(users, start=1):
        if previous_score is None or user.total_points < previous_score:
            current_rank = index
        previous_score = user.total_points
        old_rank = user.last_notified_rank
        db.add(
            AppRanking(
                user_code=user.user_code,
                nickname=user.nickname,
                total_points=user.total_points,
                rank_num=current_rank,
            )
        )
        if old_rank is not None and current_rank < old_rank:
            moved = old_rank - current_rank
            result = dispatch_notification(
                db=db,
                user_code=user.user_code,
                notification_type="RANKING_CHANGE",
                title="랭킹이 올랐어요",
                body=f"{old_rank}위 → {current_rank}위 ({moved}계단 상승)",
                data={},
                idempotency_key=(
                    f"ranking:{current_kst.date().isoformat()}:{user.user_code}:"
                    f"{old_rank}:{current_rank}"
                ),
                now_kst=current_kst,
            )
            if result not in {"disabled", "duplicate"}:
                notifications += 1
        user.last_notified_rank = current_rank
    db.commit()
    return {"users": len(users), "notifications": notifications}