
package com.example.busasnquest.data.remote

import com.example.busasnquest.data.model.RankingResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface RankingApi {
    @GET("rankings")
    suspend fun getRankings(
        @Query("type") type: String   // "all" / "region" / "friend"
    ): RankingResponse
}