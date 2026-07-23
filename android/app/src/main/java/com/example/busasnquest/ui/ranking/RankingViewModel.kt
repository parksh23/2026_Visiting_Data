package com.example.busasnquest.ui.ranking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.busasnquest.data.model.RankEntry
import com.example.busasnquest.data.model.RankingResponse
import com.example.busasnquest.data.remote.RetrofitInstance
import com.example.busasnquest.data.repository.RankingRepository
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
            } catch (e: HttpException) {
                _uiState.value = RankingUiState.Error("랭킹을 불러오지 못했습니다. (${e.code()})")
            } catch (e: IOException) {
                _uiState.value = RankingUiState.Error("네트워크 연결을 확인해주세요.")
            } catch (e: Exception) {
                _uiState.value = RankingUiState.Error("랭킹을 불러오는 중 오류가 발생했습니다.")
            }
        }
    }

    // 서버 응답 → 화면 상태 변환
    private fun RankingResponse.toSuccessState() = RankingUiState.Success(
        myRank = myRank.rank.toString(),
        topPercent = "상위 ${myRank.topPercent}%",
        point = "${"%,d".format(myRank.point)}P",
        rankings = rankings.map {
            RankEntry(
                rank = it.rank,
                name = it.name,
                score = "${"%,d".format(it.score)}P",
                // 서버가 내 순위(rank)를 함께 주므로 rank 일치 여부로 내 행 표시
                isMe = it.rank == myRank.rank
            )
        }
    )

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
