package com.example.busasnquest.ui.mission

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.busasnquest.data.model.MissionState
import com.example.busasnquest.data.model.MissionType
import com.example.busasnquest.ui.components.InlineErrorBanner
import com.example.busasnquest.ui.components.MissionCard
import com.example.busasnquest.ui.components.ScreenHeader
import com.example.busasnquest.ui.components.SegmentedToggle
import com.example.busasnquest.ui.components.rememberMissionVerifier
import com.example.busasnquest.ui.home.HomeViewModel
import com.example.busasnquest.ui.theme.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * 미션 탭 (리디자인 v2)
 * - 세그먼트 [지역별 | 종류별]
 * - 지역별: 부산 지도 실루엣 히트맵 (남은 세로 공간 전체 사용)
 * - 종류별: 인증 방식 필터 칩 + 미션 카드 리스트 (스크롤)
 * 지도 탭과의 역할 분리(A안): 여기는 "게임판", 지도 탭은 실제 카카오맵.
 */
@Composable
fun MissionScreen(
    navController: NavHostController,
    viewModel: MissionViewModel = viewModel(),
    homeViewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val loadError by viewModel.loadError.collectAsStateWithLifecycle()

    // 인증 헬퍼 (사진/위치/영수증 런처를 다 담고 있음)
    val verify = rememberMissionVerifier(homeViewModel)

    // 진행 중 미션이 있는 구 (지도에 점 표시)
    val inProgressSet = remember(uiState.allMissions) {
        uiState.allMissions
            .filter { it.state == MissionState.IN_PROGRESS || it.state == MissionState.VERIFYING }
            .map { it.mission.district }
            .toSet()
    }

    // 지도가 weight 로 남은 높이를 전부 쓰도록 LazyColumn 이 아닌 Column 구조
    Column(modifier = Modifier.fillMaxSize()) {

        ScreenHeader(
            title = "미션",
            subtitle = "부산을 점령하고 포인트를 모아보세요!"
        )

        // 서버에서 미션을 못 불러온 경우 안내 배너 (로컬 데이터는 계속 표시)
        loadError?.let { msg ->
            InlineErrorBanner(message = msg, onRetry = viewModel::refreshFromServer)
            Spacer(modifier = Modifier.height(12.dp))
        }

        SegmentedToggle(
            options = listOf("지역별", "종류별"),
            selectedIndex = uiState.selectedTab,
            onSelect = { viewModel.selectTab(it) }
        )

        Spacer(modifier = Modifier.height(14.dp))

        if (uiState.selectedTab == 0) {
            // ───── 지역별: 부산 지도 실루엣 히트맵 (남은 화면 전체 사용) ─────
            BusanMap(
                districts = uiState.districts,
                inProgressSet = inProgressSet,
                selected = uiState.selectedDistrict,
                onSelect = { viewModel.selectDistrict(it) },
                modifier = Modifier
                    .weight(1f)
                    // 시스템 내비게이션 인셋 + 플로팅 탭바 높이만큼 확보 (지도 하단 가림 방지)
                    .navigationBarsPadding()
                    .padding(bottom = 92.dp)
            )
        } else {
            // ───── 종류별: 인증 방식 필터 칩 + 미션 카드 리스트 ─────
            TypeFilterChips(
                selected = uiState.typeFilter,
                onSelect = { viewModel.selectTypeFilter(it) }
            )
            Spacer(modifier = Modifier.height(14.dp))

            val filtered = uiState.allMissions.filter {
                uiState.typeFilter == null || it.mission.type == uiState.typeFilter
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = Dimens.bottomBarSpace + 100.dp)
            ) {
                if (filtered.isEmpty()) {
                    item {
                        Text(
                            "조건에 맞는 미션이 없어요.",
                            color = TextSub,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp)
                        )
                    }
                } else {
                    items(filtered, key = { it.mission.id }) { item ->
                        MissionCard(
                            item = item,
                            onChallenge = { viewModel.startMission(item.mission.id) },
                            onClick = { navController.navigate("missionDetail/${item.mission.id}") },
                            onVerify = { verify(item.mission.id, item.mission.type) },
                            onToggleSaved = { viewModel.toggleSaved(item.mission.id) }
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                    }
                }
            }
        }
    }

    // ───── 구 선택 바텀시트 ─────
    uiState.selectedDistrict?.let { district ->
        DistrictBottomSheet(
            districtName = district,
            missions = uiState.allMissions.filter { it.mission.district == district },
            onDismiss = { viewModel.dismissDistrict() },
            onMissionClick = { id ->
                viewModel.dismissDistrict()
                navController.navigate("missionDetail/$id")
            },
            onChallenge = { id -> viewModel.startMission(id) },
            onVerify = { id, type -> verify(id, type) }
        )
    }
}

/** 종류별 탭의 인증 방식 필터 칩 (전체 / 사진 / 위치 / 영수증). */
@Composable
private fun TypeFilterChips(
    selected: MissionType?,
    onSelect: (MissionType?) -> Unit
) {
    val options: List<Pair<String, MissionType?>> = listOf(
        "전체" to null,
        "사진" to MissionType.PHOTO_LOCATION,
        "위치" to MissionType.CURRENT_LOCATION,
        "영수증" to MissionType.RECEIPT
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = Dimens.screenPadding),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (label, type) ->
            val isSelected = selected == type
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (isSelected) Coral else CardWhite)
                    .then(
                        if (isSelected) Modifier
                        else Modifier.border(1.dp, DividerGray, RoundedCornerShape(999.dp))
                    )
                    .clickable { onSelect(type) }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    label,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) Color.White else TextSub
                )
            }
        }
    }
}
