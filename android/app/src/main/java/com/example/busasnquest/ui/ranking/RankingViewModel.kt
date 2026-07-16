package com.example.busasnquest.ui.ranking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.busasnquest.data.model.RankEntry
import com.example.busasnquest.data.remote.RetrofitInstance
import com.example.busasnquest.data.repository.RankingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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

    init {
        loadRankings(0)
    }

    fun onSelectTab(index: Int) {
        _selectedTab.value = index
        loadRankings(index)
    }

    private fun loadRankings(tab: Int) {
        viewModelScope.launch {
            _uiState.value = RankingUiState.Loading

            try {
                val type = when (tab) {
                    1 -> "region"
                    2 -> "friend"
                    else -> "all"
                }

                val res = repository.fetchRankings(type)

                _uiState.value = RankingUiState.Success(
                    myRank = res.myRank.rank.toString(),
                    topPercent = "상위 ${res.myRank.topPercent}%",
                    point = "%,dP".format(res.myRank.point),
                    rankings = res.rankings.map {
                        RankEntry(
                            rank = it.rank,
                            name = it.name,
                            score = "%,dP".format(it.score),
                            isMe = false
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = RankingUiState.Error(
                    e.message ?: "랭킹을 불러오지 못했습니다."
                )
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                RankingViewModel(
                    RankingRepository(RetrofitInstance.rankingApi)
                )
            }
        }
    }
}