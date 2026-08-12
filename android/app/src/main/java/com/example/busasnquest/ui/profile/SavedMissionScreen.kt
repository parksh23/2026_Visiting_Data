package com.example.busasnquest.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.busasnquest.data.repository.MissionWithState
import com.example.busasnquest.ui.theme.*

/**
 * 찜한 미션 목록 화면.
 *
 * - GET /api/v1/missions/saved 결과를 최근 찜한 순으로 보여준다.
 * - 하트를 누르면 DELETE /api/v1/missions/{id}/saved → 성공 시 목록에서 즉시 제거된다.
 * - 항목을 누르면 기존 미션 상세 화면으로 이동한다.
 */
@Composable
fun SavedMissionScreen(
    navController: NavHostController,
    viewModel: ProfileViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val loading by viewModel.savedLoading.collectAsStateWithLifecycle()
    val error by viewModel.savedError.collectAsStateWithLifecycle()
    val savePending by viewModel.savePending.collectAsStateWithLifecycle()
    val saved = uiState.savedMissions

    val snackbarHostState = remember { SnackbarHostState() }

    // 화면에 들어올 때마다 서버 기준으로 다시 불러온다 (앱 재실행에도 상태 유지)
    LaunchedEffect(Unit) { viewModel.refreshSavedMissions() }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSavedError()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                Text("찜한 미션", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = NavyMain)
            }

            when {
                // 첫 로딩 (이미 목록이 있으면 깜빡이지 않게 그대로 보여준다)
                loading && saved.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Coral)
                    }
                }

                // 빈 상태
                saved.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "아직 찜한 미션이 없어요.\n미션 카드의 하트를 눌러보세요!",
                            color = TextSub,
                            fontSize = 14.sp
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = Dimens.bottomBarSpace)
                    ) {
                        item {
                            Text(
                                "찜한 미션 ${saved.size}개",
                                color = TextSub,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(
                                    horizontal = Dimens.screenPadding,
                                    vertical = Dimens.gapTight
                                )
                            )
                        }
                        items(saved, key = { it.mission.id }) { item ->
                            SavedRow(
                                item = item,
                                pending = savePending.contains(item.mission.id),
                                onClick = { navController.navigate("missionDetail/${item.mission.id}") },
                                onUnsave = { viewModel.unsaveMission(item.mission.id) }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(40.dp)) }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
fun SavedRow(
    item: MissionWithState,
    pending: Boolean = false,
    onClick: () -> Unit = {},
    onUnsave: () -> Unit
) {
    val mission = item.mission

    Row(
        modifier = Modifier
            .padding(horizontal = Dimens.screenPadding, vertical = 6.dp)
            .fillMaxWidth()
            .pressable(onClick = onClick)
            .clip(RoundedCornerShape(Dimens.radiusCard))
            .background(CardWhite)
            .border(Dimens.borderWidth, InkBorder, RoundedCornerShape(Dimens.radiusCard))
            .padding(Dimens.gapBlock),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(mission.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextMain)
            Spacer(modifier = Modifier.height(2.dp))
            Text(mission.region, color = TextSub, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(6.dp))
            // 보상 — 앱 공통 포인트 표시
            com.example.busasnquest.ui.components.PointAmount(
                value = mission.reward,
                prefix = "+",
                badgeSize = 15.dp,
                badgeFontSize = 8.sp,
                fontSize = 13.sp,
                gap = 4.dp
            )
        }

        // 찜 해제 하트 — 24dp 아이콘 단독 클릭은 터치 타깃이 작다 → 44dp 박스로 확대
        // 요청 중에는 잠금 → 연속 클릭으로 DELETE 가 여러 번 나가지 않는다
        Box(
            modifier = Modifier
                .size(44.dp)
                .pressable(enabled = !pending, scaleDown = 0.86f, onClick = onUnsave),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Favorite,
                contentDescription = "찜 해제",
                tint = if (pending) Coral.copy(alpha = 0.4f) else Coral,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
