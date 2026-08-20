package kr.co.busanquest.ui.ranking

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import kr.co.busanquest.ui.components.ErrorView
import kr.co.busanquest.ui.components.LoadingView
import kr.co.busanquest.ui.theme.*

/**
 * 구·군 하나의 랭킹 상세 화면.
 *
 * 데이터는 전부 서버에서 온다 — GET /api/v1/rankings?type=region&district={구 이름}
 * 점수 단위는 포인트가 아니라 "그 구에서 완료한 미션 수"다.
 */
@Composable
fun DistrictRankingScreen(
    navController: NavHostController,
    districtName: String,
    viewModel: DistrictRankingViewModel = viewModel(
        factory = DistrictRankingViewModel.factory(districtName)
    )
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgSoftBlue)
    ) {
        // 상단 바
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "뒤로",
                    tint = NavyMain
                )
            }
            Text(
                "$districtName 랭킹",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = NavyMain
            )
        }

        when (val s = state) {
            is DistrictRankingUiState.Loading -> {
                LoadingView("랭킹을 불러오는 중...")
            }

            is DistrictRankingUiState.Error -> {
                ErrorView(message = s.message, onRetry = viewModel::retry)
            }

            is DistrictRankingUiState.Success -> {
                // 내 기록 카드
                Box(
                    modifier = Modifier
                        .padding(
                            horizontal = Dimens.screenPadding,
                            vertical = Dimens.gapTight
                        )
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(Dimens.radiusCard))
                        .background(NavyMain)
                        .padding(20.dp)
                ) {
                    Column {
                        Text(
                            "$districtName 에서 나의 기록",
                            color = Color.White.copy(0.8f),
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                "완료한 미션 ${s.myScore}개",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            // 서버가 순위를 계산하지 못했으면(rank=0) 순위는 감춘다
                            if (s.myRank > 0) {
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    "${s.myRank}위",
                                    color = Color.White.copy(0.8f),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (s.rankings.isEmpty()) {
                    Text(
                        "아직 이 지역의 랭킹 기록이 없어요.",
                        color = TextSub,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = bottomBarSpacing())
                    ) {
                        items(s.rankings) { entry ->
                            // 지역 랭킹의 점수는 "3개"(완료 미션 수)라 P 뱃지를 붙이지 않는다
                            RankingRow(entry, scoreIsPoint = false)
                        }
                        item { Spacer(modifier = Modifier.height(40.dp)) }
                    }
                }
            }
        }
    }
}
