package kr.co.busanquest.data.repository

import kr.co.busanquest.data.model.RankingResponse
import kr.co.busanquest.data.remote.RankingApi

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
