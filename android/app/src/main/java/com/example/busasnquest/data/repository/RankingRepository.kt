
package com.example.busasnquest.data.repository

import com.example.busasnquest.data.model.RankingResponse
import com.example.busasnquest.data.remote.RankingApi

class RankingRepository(
    private val api: RankingApi
) {
    suspend fun fetchRankings(type: String, district: String? = null): RankingResponse {
        return api.getRankings(type, district)
    }
}
