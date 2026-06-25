package com.example.busasnquest.ui.ranking

import androidx.lifecycle.ViewModel
import com.example.busasnquest.data.model.RankEntry
import com.example.busasnquest.data.model.rankingList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


// 화면이 그릴 '상태' 한 덩어리
data class RankingUiState(
    val entries: List<RankEntry> = emptyList(),
    val selectedTab: Int = 0,
    val isLoading: Boolean = false   // ← 추가
)

class RankingViewModel : ViewModel() {

    // 내부에서만 수정 가능한 상태
    private val _uiState = MutableStateFlow(RankingUiState())
    // 화면에는 읽기 전용으로만 노출
    val uiState: StateFlow<RankingUiState> = _uiState.asStateFlow()

    // ViewModel이 만들어질 때 데이터 로드
    init {
        loadRanking()
    }

    private fun loadRanking() {
        viewModelScope.launch {          // ← 코루틴 시작 (자동 취소되는 스코프)
            _uiState.update { it.copy(isLoading = true) }

            delay(1000)                  // ← 서버 통신을 흉내내는 1초 대기 api.getRanking() 나중엔 이거 추가

            _uiState.update {
                it.copy(entries = rankingList, isLoading = false)
            }
        }
    }

    // 화면에서 탭을 누르면 호출되는 함수 (이벤트는 위로 올라온다)
    fun selectTab(index: Int) {
        _uiState.update { it.copy(selectedTab = index) }
    }
}