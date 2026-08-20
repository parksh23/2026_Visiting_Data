package com.example.busasnquest.data.repository

import com.example.busasnquest.data.model.RankingResponse
import com.example.busasnquest.data.remote.RankingApi

class RankingRepository(
    private val api: RankingApi
) {
    /**
     * @param district type="region" 일 때만 의미가 있다. 나머지 타입에서는 null 로 둔다.
     */
    suspend fun fetchRankings(type: String, district: String? = null): RankingResponse {
        return api.getRankings(type, district)
    }
}
