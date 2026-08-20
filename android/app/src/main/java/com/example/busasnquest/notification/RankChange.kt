package com.example.busasnquest.notification

enum class RankChangeDirection { UP, DOWN }

data class RankChange(
    val previousRank: Int,
    val currentRank: Int,
    val direction: RankChangeDirection,
    val difference: Int
)

/** 최초 조회와 유효하지 않은 순위(0)는 알림 대상에서 제외한다. */
fun detectRankChange(previousRank: Int?, currentRank: Int): RankChange? {
    if (previousRank == null || previousRank <= 0 || currentRank <= 0) return null
    if (previousRank == currentRank) return null

    return RankChange(
        previousRank = previousRank,
        currentRank = currentRank,
        direction = if (currentRank < previousRank) RankChangeDirection.UP else RankChangeDirection.DOWN,
        difference = kotlin.math.abs(previousRank - currentRank)
    )
}
