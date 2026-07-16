package com.example.busasnquest.data.remote

import com.example.busasnquest.data.model.RankingResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface RankingApi {
    @GET("api/v1/rankings")
    suspend fun getRankings(
        @Query("type") type: String
    ): RankingResponse
}