package kr.co.busanquest.ui.mission

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import kr.co.busanquest.data.model.MissionState
import kr.co.busanquest.data.model.MissionType
import kr.co.busanquest.ui.components.InlineErrorBanner
import kr.co.busanquest.ui.components.MissionCard
import kr.co.busanquest.ui.components.SegmentedToggle
import kr.co.busanquest.ui.components.rememberMissionVerifier
import kr.co.busanquest.ui.home.HomeViewModel
import kr.co.busanquest.ui.theme.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kr.co.busanquest.data.repository.DistrictMissionProgress
import kr.co.busanquest.data.repository.OccupationStat

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
    val saveError by viewModel.saveError.collectAsStateWithLifecycle()

    // 인증 헬퍼 (사진/위치/영수증 런처를 다 담고 있음)
    val verify = rememberMissionVerifier(homeViewModel)

    // 찜 실패 안내 (401/404/500 등)
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(saveError) {
        saveError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSaveError()
        }
    }

    // 진행 중 미션이 있는 구 (지도에 점 표시)
    val inProgressSet = remember(uiState.allMissions) {
        uiState.allMissions
            .filter { it.state == MissionState.IN_PROGRESS || it.state == MissionState.VERIFYING }
            .map { it.mission.district }
            .toSet()
    }

    // 지도가 weight 로 남은 높이를 전부 쓰도록 LazyColumn 이 아닌 Column 구조
    Box(modifier = Modifier.fillMaxSize()) {
    Column(modifier = Modifier.fillMaxSize()) {

        SectionTitle("미션")

        Spacer(modifier = Modifier.height(Dimens.gapBlock))

        SegmentedToggle(
            options = listOf("지역별", "종류별"),
            selectedIndex = uiState.selectedTab,
            onSelect = { viewModel.selectTab(it) }
        )

        Spacer(modifier = Modifier.height(Dimens.gapBlock))

        // 오류가 발생해도 제목과 탭의 위치는 유지한다.
        loadError?.let { msg ->
            InlineErrorBanner(message = msg, onRetry = viewModel::refreshFromServer)
            Spacer(modifier = Modifier.height(Dimens.cardGap))
        }

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
            val categories = remember(uiState.allMissions) {
                (listOf("장소탐방", "먹거리", "산책·트레킹", "문화·체험", "경기·공연", "등산") +
                    uiState.allMissions.mapNotNull { it.mission.category }).distinct()
            }
            FilterChips(
                options = listOf(
                    "전체" to null,
                    "사진" to MissionFilter(type = MissionType.IMAGE_LOCATION),
                    "위치" to MissionFilter(type = MissionType.CURRENT_LOCATION),
                    "영수증" to MissionFilter(type = MissionType.RECEIPT)
                ) + categories.map { it to MissionFilter(category = it) },
                selected = when {
                    uiState.typeFilter != null -> MissionFilter(type = uiState.typeFilter)
                    uiState.categoryFilter != null -> MissionFilter(category = uiState.categoryFilter)
                    else -> null
                },
                onSelect = { filter ->
                    if (filter?.category != null) viewModel.selectCategoryFilter(filter.category)
                    else viewModel.selectTypeFilter(filter?.type)
                }
            )
            Spacer(modifier = Modifier.height(Dimens.gapBlock))

            val filtered = uiState.allMissions.filter {
                (uiState.typeFilter == null || it.mission.type == uiState.typeFilter) &&
                    (uiState.categoryFilter == null || it.mission.category == uiState.categoryFilter)
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = bottomBarSpacing())
            ) {
                if (filtered.isEmpty()) {
                    item {
                        Text(
                            "조건에 맞는 미션이 없어요.",
                            color = TextSub,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(
                                horizontal = Dimens.screenPadding,
                                vertical = Dimens.screenPadding
                            )
                        )
                    }
                } else {
                    items(filtered, key = { it.mission.id }) { item ->
                        MissionCard(
                            item = item,
                            onChallenge = { viewModel.startMission(item.mission.id) },
                            onClick = { navController.navigate("missionDetail/${item.mission.id}") },
                            onVerify = { verify(item.mission.id, item.mission.type) },
                            onToggleSaved = { viewModel.toggleSaved(item.mission.id) },
                            savePending = uiState.savePending.contains(item.mission.id)
                        )
                        Spacer(modifier = Modifier.height(Dimens.cardGap))
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

        // 찜 실패 안내 스낵바
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = bottomBarSpacing())
        )
    }
}

/** 랭킹·내 정보의 ScreenHeader와 같은 제목 크기 및 상단/좌측 여백. */
@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = displayStyle(28.sp),
        color = TextMain,
        modifier = Modifier
            .padding(horizontal = Dimens.screenPadding)
            .padding(top = Dimens.gapBlock)
    )
}

private data class MissionFilter(
    val type: MissionType? = null,
    val category: String? = null
)

@Composable
private fun <T> FilterChips(
    options: List<Pair<String, T?>>,
    selected: T?,
    onSelect: (T?) -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = Dimens.screenPadding),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (label, type) ->
            val isSelected = selected == type
            // 칩 선택은 위치 이동 없이 색만 넘긴다 (리스트가 아래에서 갈아끼워지는 게 본 이벤트)
            val fg by animateColorAsState(
                targetValue = if (isSelected) OnCoral else TextSub,
                animationSpec = tween(Motion.DurRelease, easing = Motion.EaseOut),
                label = "chipFg"
            )
            Box(
                modifier = Modifier
                    .pressable(scaleDown = 0.95f) { onSelect(type) }
                    .then(
                        if (isSelected)
                            Modifier
                                .clip(CircleShape)
                                .background(Coral)
                                .border(Dimens.borderWidth, InkBorderStrong, CircleShape)
                        else Modifier.raisedSurface(CircleShape)
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    label,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) CoralInk else TextSub
                )
            }
        }
    }
}
