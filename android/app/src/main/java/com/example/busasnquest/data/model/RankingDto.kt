
package com.example.busasnquest.data.model

import com.google.gson.annotations.SerializedName

// 서버 전체 응답
// ⚠️ 규격서 4-4: 랭킹 응답만 camelCase (myRank, topPercent, userId).
//    다른 API(snake_case)와 규칙이 다르므로 @SerializedName 으로 키를 명시해 고정한다.
data class RankingResponse(
    @SerializedName("myRank")
    val myRank: MyRankDto,

    @SerializedName("rankings")
    val rankings: List<RankEntryDto> = emptyList()
)

// 내 순위 정보
data class MyRankDto(
    @SerializedName("rank")
    val rank: Int,

    @SerializedName("topPercent")
    val topPercent: Int,

    @SerializedName("point")
    val point: Int
)

// 랭킹 한 줄
data class RankEntryDto(
    @SerializedName("rank")
    val rank: Int,

    @SerializedName("userId")
    val userId: String,

    @SerializedName("name")
    val name: String,

    @SerializedName("score")
    val score: Int
)
