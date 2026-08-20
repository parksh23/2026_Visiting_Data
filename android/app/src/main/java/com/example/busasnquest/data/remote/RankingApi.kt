package com.example.busasnquest.data.remote

import com.example.busasnquest.data.model.RankingResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface RankingApi {
    /**
     * 랭킹 조회. GET /api/v1/rankings
     *
     * @param type     all | region | friend
     * @param district type=region 일 때 대상 구·군 이름("해운대구").
     *                 null 이면 서버가 내 등록 지역으로 폴백한다.
     *                 Retrofit 은 null 인 @Query 를 URL 에 아예 붙이지 않는다.
     */
    @GET("api/v1/rankings")
    suspend fun getRankings(
        @Query("type") type: String,
        @Query("district") district: String? = null
    ): RankingResponse
}
