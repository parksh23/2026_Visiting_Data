package com.example.busasnquest.ui.ranking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.busasnquest.data.model.RankEntry
import com.example.busasnquest.data.model.RankingResponse
import com.example.busasnquest.data.remote.RetrofitInstance
import com.example.busasnquest.data.repository.RankingRepository
import com.example.busasnquest.util.Notifier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

/**
 * 랭킹 탭 종류.
 * 서버 쿼리 파라미터(?type=...)로 보낼 값을 함께 가진다.
 * 문자열 하드코딩으로 인한 오타 사고를 막기 위해 enum 으로 고정.
 */
enum class RankingType(val query: String) {
    ALL("all"),        // 전체 랭킹
    REGION("region"),  // 지역(구·군)별 랭킹
    FRIEND("friend");  // 친구 랭킹

    companion object {
        fun fromTabIndex(index: Int): RankingType = when (index) {
            1 -> REGION
            2 -> FRIEND
            else -> ALL
        }
    }
}

// 화면 상태
sealed interface RankingUiState {
    object Loading : RankingUiState
    data class Success(
        val myRank: String,
        val topPercent: String,
        val point: String,
        val rankings: List<RankEntry>
    ) : RankingUiState
    data class Error(val message: String) : RankingUiState
}

class RankingViewModel(
    private val repository: RankingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<RankingUiState>(RankingUiState.Loading)
    val uiState: StateFlow<RankingUiState> = _uiState.asStateFlow()

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    // 탭별 응답 캐시: 같은 탭을 다시 눌렀을 때 불필요한 재요청 방지
    private val cache = mutableMapOf<RankingType, RankingResponse>()

    init {
        loadRankings(RankingType.ALL)
    }

    fun onSelectTab(index: Int) {
        if (_selectedTab.value == index && _uiState.value is RankingUiState.Success) return
        _selectedTab.value = index
        loadRankings(RankingType.fromTabIndex(index))
    }

    // 에러 화면의 "다시 시도" 버튼용
    fun retry() {
        loadRankings(RankingType.fromTabIndex(_selectedTab.value), force = true)
    }

    private fun loadRankings(type: RankingType, force: Boolean = false) {
        // 캐시가 있으면 바로 표시 (force 면 무시하고 재요청)
        if (!force) {
            cache[type]?.let {
                _uiState.value = it.toSuccessState()
                return
            }
        }

        _uiState.value = RankingUiState.Loading
        viewModelScope.launch {
            try {
                // GET /api/v1/rankings?type=all|region|friend
                val res = repository.fetchRankings(type.query)
                cache[type] = res
                _uiState.value = res.toSuccessState()
                // 순위 변동 알림은 "전체 랭킹" 기준으로만 (탭 전환마다 울리지 않게)
                if (type == RankingType.ALL) {
                    Notifier.checkRankChange(res.myRank.rank)
                }
            } catch (e: HttpException) {
                _uiState.value = RankingUiState.Error("랭킹을 불러오지 못했습니다. (${e.code()})")
            } catch (e: IOException) {
                _uiState.value = RankingUiState.Error("네트워크 연결을 확인해주세요.")
            } catch (e: Exception) {
                _uiState.value = RankingUiState.Error("랭킹을 불러오는 중 오류가 발생했습니다.")
            }
        }
    }

    /**
     * 마지막으로 서버에서 받은 "내 기록"(순위/상위%/포인트).
     *
     * 지역 랭킹은 서버가 사용자의 지역(district_name)을 모르면
     * {rank: 0, topPercent: 0, point: 0} + 빈 목록을 돌려준다.
     * 그대로 표시하면 탭을 옮길 때마다 내 포인트가 0으로 바뀌어 버리므로,
     * 이런 응답에서는 헤더 숫자를 덮어쓰지 않고 직전 값을 그대로 유지한다.
     */
    private var lastMyRank: String? = null
    private var lastTopPercent: String? = null
    private var lastPoint: String? = null

    // 서버 응답 → 화면 상태 변환
    private fun RankingResponse.toSuccessState(): RankingUiState.Success {
        // rank 가 0 이면 서버가 이 탭의 내 기록을 계산하지 못한 것
        val hasMyRecord = myRank.rank > 0

        val rankText = if (hasMyRecord) myRank.rank.toString() else lastMyRank ?: "-"
        val topPercentText =
            if (hasMyRecord) "상위 ${myRank.topPercent}%" else lastTopPercent ?: "-"
        // 숫자만 넘긴다 — "P" 는 화면의 공통 포인트 뱃지가 대신한다
        val pointText =
            if (hasMyRecord) "%,d".format(myRank.point) else lastPoint ?: "-"

        if (hasMyRecord) {
            lastMyRank = rankText
            lastTopPercent = topPercentText
            lastPoint = pointText
        }

        return RankingUiState.Success(
            myRank = rankText,
            topPercent = topPercentText,
            point = pointText,
            rankings = rankings.map {
                RankEntry(
                    rank = it.rank,
                    name = it.name,
                    // "P" 없이 숫자만 — 화면에서 공통 포인트 뱃지를 붙인다
                    score = "%,d".format(it.score),
                    // 서버가 내 순위(rank)를 함께 주므로 rank 일치 여부로 내 행 표시
                    isMe = it.rank == myRank.rank
                )
            }
        )
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                RankingViewModel(
                    // BASE_URL 이 통일된 RetrofitInstance 의 rankingApi 사용
                    RankingRepository(RetrofitInstance.rankingApi)
                )
            }
        }
    }
}
