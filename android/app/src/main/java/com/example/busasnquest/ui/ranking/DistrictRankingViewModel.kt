package com.example.busasnquest.ui.ranking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.busasnquest.data.model.RankEntry
import com.example.busasnquest.data.model.RankingResponse
import com.example.busasnquest.data.remote.RetrofitInstance
import com.example.busasnquest.data.repository.RankingRepository
import com.example.busasnquest.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

/**
 * 구·군 상세 랭킹 화면 상태.
 *
 * 점수(score)는 포인트가 아니라 "그 구에서 완료한 미션 수"다 — 서버 region 랭킹의 규칙.
 */
sealed interface DistrictRankingUiState {
    object Loading : DistrictRankingUiState

    data class Success(
        val myRank: Int,                  // 0 이면 서버가 내 순위를 계산하지 못한 것
        val myScore: Int,                 // 이 구에서 내가 완료한 미션 수
        val rankings: List<RankEntry>
    ) : DistrictRankingUiState

    data class Error(val message: String) : DistrictRankingUiState
}

/**
 * 구·군 하나의 랭킹을 서버에서 받아오는 ViewModel.
 * GET /api/v1/rankings?type=region&district={구 이름}
 */
class DistrictRankingViewModel(
    private val repository: RankingRepository,
    private val districtName: String
) : ViewModel() {

    private val _uiState = MutableStateFlow<DistrictRankingUiState>(DistrictRankingUiState.Loading)
    val uiState: StateFlow<DistrictRankingUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    /** 에러 화면의 "다시 시도" 버튼용 */
    fun retry() = load()

    private fun load() {
        _uiState.value = DistrictRankingUiState.Loading
        viewModelScope.launch {
            // 내 행을 찾으려면 닉네임이 필요하다. 아직 못 불러왔으면 여기서 한 번 채운다.
            if (UserRepository.name.value.isBlank()) {
                UserRepository.refreshProfile()
            }
            try {
                val res = repository.fetchRankings(
                    type = RankingType.REGION.query,
                    district = districtName
                )
                _uiState.value = res.toSuccessState()
            } catch (e: HttpException) {
                _uiState.value =
                    DistrictRankingUiState.Error("랭킹을 불러오지 못했습니다. (${e.code()})")
            } catch (e: IOException) {
                _uiState.value = DistrictRankingUiState.Error("네트워크 연결을 확인해주세요.")
            } catch (e: Exception) {
                _uiState.value =
                    DistrictRankingUiState.Error("랭킹을 불러오는 중 오류가 발생했습니다.")
            }
        }
    }

    private fun RankingResponse.toSuccessState(): DistrictRankingUiState.Success {
        val myName = UserRepository.name.value

        return DistrictRankingUiState.Success(
            myRank = myRank.rank,
            myScore = myRank.point,
            rankings = rankings.map {
                RankEntry(
                    rank = it.rank,
                    name = it.name,
                    // 지역 랭킹의 점수 단위는 "개"(완료 미션 수)
                    score = "${it.score}개",
                    // 닉네임은 서버에서 유일값이라 이름으로 내 행을 찾는다.
                    // 동점자가 같은 rank 를 받으므로 rank 비교로는 구분되지 않는다.
                    isMe = myName.isNotBlank() && it.name == myName
                )
            }
        )
    }

    companion object {
        /** 구 이름이 화면마다 달라서 팩토리를 인자로 만든다 */
        fun factory(districtName: String) = viewModelFactory {
            initializer {
                DistrictRankingViewModel(
                    RankingRepository(RetrofitInstance.rankingApi),
                    districtName
                )
            }
        }
    }
}
