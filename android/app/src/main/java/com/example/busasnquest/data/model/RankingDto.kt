
package com.example.busasnquest.data.model

// 서버 전체 응답
data class RankingResponse(
    val myRank: MyRankDto,
    val rankings: List<RankEntryDto>
)

// 내 순위 정보
data class MyRankDto(
    val rank: Int,
    val topPercent: Int,
    val point: Int
)

// 랭킹 한 줄
data class RankEntryDto(
    val rank: Int,
    val userId: String,
    val name: String,
    val score: Int
)