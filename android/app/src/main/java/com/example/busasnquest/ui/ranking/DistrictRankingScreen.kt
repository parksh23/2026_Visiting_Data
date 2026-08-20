package com.example.busasnquest.ui.ranking

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.busasnquest.data.model.RankEntry
import com.example.busasnquest.data.remote.RetrofitInstance
import com.example.busasnquest.data.repository.RankingRepository
import com.example.busasnquest.ui.components.ErrorView
import com.example.busasnquest.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

internal sealed interface DistrictRankingUiState {
    data object Loading : DistrictRankingUiState
    data class Success(val myCount: Int, val rankings: List<RankEntry>) : DistrictRankingUiState
    data class Error(val message: String) : DistrictRankingUiState
}

internal class DistrictRankingViewModel(
    private val districtName: String,
    private val repository: RankingRepository
) : ViewModel() {
    private val _state = MutableStateFlow<DistrictRankingUiState>(DistrictRankingUiState.Loading)
    val state: StateFlow<DistrictRankingUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _state.value = DistrictRankingUiState.Loading
        viewModelScope.launch {
            _state.value = try {
                val response = repository.fetchRankings("region", districtName)
                DistrictRankingUiState.Success(
                    myCount = response.myRank.point,
                    rankings = response.rankings.map { entry ->
                        RankEntry(
                            rank = entry.rank,
                            name = entry.name,
                            score = "${entry.score}개",
                            isMe = entry.userId == response.myRank.userId
                        )
                    }
                )
            } catch (error: HttpException) {
                DistrictRankingUiState.Error("지역 랭킹을 불러오지 못했습니다. (${error.code()})")
            } catch (_: IOException) {
                DistrictRankingUiState.Error("네트워크 연결을 확인해주세요.")
            } catch (_: Exception) {
                DistrictRankingUiState.Error("지역 랭킹을 불러오는 중 오류가 발생했습니다.")
            }
        }
    }

    companion object {
        fun factory(districtName: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    DistrictRankingViewModel(
                        districtName,
                        RankingRepository(RetrofitInstance.rankingApi)
                    ) as T
            }
    }
}

@Composable
internal fun DistrictRankingScreen(
    navController: NavHostController,
    districtName: String,
    viewModel: DistrictRankingViewModel = viewModel(
        key = "district-ranking-$districtName",
        factory = DistrictRankingViewModel.factory(districtName)
    )
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgSoftBlue)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로", tint = NavyMain)
            }
            Text("$districtName 랭킹", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = NavyMain)
        }

        when (val current = state) {
            DistrictRankingUiState.Loading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            is DistrictRankingUiState.Error -> ErrorView(
                message = current.message,
                onRetry = viewModel::refresh
            )
            is DistrictRankingUiState.Success -> {
                Box(
                    modifier = Modifier
                        .padding(horizontal = Dimens.screenPadding, vertical = Dimens.gapTight)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(Dimens.radiusCard))
                        .background(NavyMain)
                        .padding(20.dp)
                ) {
                    Column {
                        Text("$districtName 에서 나의 기록", color = Color.White.copy(0.8f), fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "완료한 미션 ${current.myCount}개",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(contentPadding = PaddingValues(bottom = bottomBarSpacing())) {
                    items(current.rankings, key = { "${it.rank}-${it.name}" }) { entry ->
                        RankingRow(entry, scoreIsPoint = false)
                    }
                    item { Spacer(modifier = Modifier.height(40.dp)) }
                }
            }
        }
    }
}
