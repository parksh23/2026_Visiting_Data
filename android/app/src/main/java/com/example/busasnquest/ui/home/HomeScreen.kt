package com.example.busasnquest.ui.home

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.busasnquest.data.model.MissionType
import com.example.busasnquest.data.model.OngoingMission
import com.example.busasnquest.data.repository.OccupationStat
import com.example.busasnquest.ui.theme.*
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.MapView
import com.kakao.vectormap.camera.CameraUpdateFactory

@Composable
fun HomeScreen(
    navController: NavHostController,
    viewModel: HomeViewModel = viewModel()
) {
    val missions by viewModel.homeMissions.collectAsStateWithLifecycle()
    val occupation by viewModel.occupation.collectAsStateWithLifecycle()
    val recommended by viewModel.recommendedMissions.collectAsStateWithLifecycle()
    val points by viewModel.points.collectAsStateWithLifecycle()



    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = Dimens.bottomBarSpace)
    ) {
        Spacer(Modifier.height(12.dp))

        // 앱 로고 + 인사말 헤더
        HomeHeader(points = points)

        Spacer(Modifier.height(16.dp))

        // 검색 바
        SearchPill { navController.navigate("map/부산?focus=true") }

        Spacer(Modifier.height(16.dp))

        // 현위치 미니맵 (탭하면 지도 탭으로)
        HomeMiniMap { navController.navigate("map/부산") }

        Spacer(Modifier.height(12.dp))

        // 현위치 주소
        LocationRow { navController.navigate("map/부산") }

        Spacer(Modifier.height(20.dp))

        // 진행중인 미션 요약
        OngoingSummaryCard(
            occupation = occupation,
            missions = missions.map { it.mission },
            onMissionClick = { id -> navController.navigate("missionDetail/$id") },
            onEmptyClick = { navController.navigate("mission") }
        )

        Spacer(Modifier.height(24.dp))

        // 추천 미션
        SectionHeaderRow("추천 미션") { navController.navigate("mission") }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            recommended.forEach { rec ->
                RecommendCard(rec) { navController.navigate("mission") }
            }
        }

        Spacer(Modifier.height(120.dp))
    }
}

@Composable
private fun HomeHeader(points: Int) {
    Column(modifier = Modifier.padding(horizontal = Dimens.screenPadding)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 로고 락업 (깃발 + "부산 땅따먹기")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(Coral),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Flag, contentDescription = null, tint = Color.White, modifier = Modifier.size(19.dp))
                }
                Spacer(Modifier.width(9.dp))
                Row {
                    Text(
                        "부산 ",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = TextMain,
                        fontFamily = LogoFontFamily,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        "땅따먹기",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = Coral,
                        fontFamily = LogoFontFamily,
                        letterSpacing = (-0.5).sp
                    )
                }
            }
            // 포인트 칩 + 알림 벨
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(CoralTint)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(17.dp).clip(CircleShape).background(Coral),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("P", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(5.dp))
                    Text("%,d".format(points), color = Coral, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(
            "부산갈매기님, 오늘도 부산을 정복해볼까요?",
            fontSize = 14.sp,
            color = TextSub,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun SearchPill(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(CardWhite)
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("어느 동네를 정복할까요?", color = TextSub, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Icon(Icons.Filled.Search, contentDescription = "검색", tint = TextSub, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun HomeMiniMap(onOpenMap: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .height(170.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(SurfaceGray)
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                MapView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    start(
                        object : MapLifeCycleCallback() {
                            override fun onMapDestroy() {}
                            override fun onMapError(error: Exception?) {
                                android.util.Log.e("HomeMiniMap", "지도 에러: ${error?.message}")
                            }
                        },
                        object : KakaoMapReadyCallback() {
                            override fun onMapReady(kakaoMap: KakaoMap) {
                                val busan = LatLng.from(35.1796, 129.0756)
                                kakaoMap.moveCamera(
                                    CameraUpdateFactory.newCenterPosition(busan, 12)
                                )
                            }
                        }
                    )
                }
            }
        )
        // 투명 오버레이: 미니맵은 조작 불가, 탭하면 지도 탭으로만 이동
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { onOpenMap() }
        )
    }
}

@Composable
private fun LocationRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.LocationOn, contentDescription = null, tint = Coral, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(6.dp))
        Text("현위치 : 부산광역시", color = TextMain, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(4.dp))
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = TextSub, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun OngoingSummaryCard(
    occupation: OccupationStat,
    missions: List<OngoingMission>,
    onMissionClick: (Int) -> Unit,
    onEmptyClick: () -> Unit
) {
    val percent = (occupation.rate * 100).toInt()

    Column(
        modifier = Modifier
            .padding(horizontal = Dimens.screenPadding)
            .fillMaxWidth()
            .shadow(Dimens.elevationFloating, RoundedCornerShape(Dimens.radiusHero))
            .clip(RoundedCornerShape(Dimens.radiusHero))
            .background(CoralTint)
            .padding(20.dp)
    ) {
        Text("진행중인 미션", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextMain)
        Spacer(Modifier.height(4.dp))
        Text(
            "미션을 선택하면\n자세한 정보를 확인할 수 있어요.",
            fontSize = 13.sp,
            color = TextSub
        )

        Spacer(Modifier.height(16.dp))

        // 진행 중인 미션 카드 (여러 개면 옆으로 스와이프)
        if (missions.isEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardWhite)
                    .clickable { onEmptyClick() }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CoralTint),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Flag, contentDescription = null, tint = Coral, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("도전 중인 미션이 없어요", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextMain, maxLines = 1)
                    Spacer(Modifier.height(2.dp))
                    Text("미션 탭에서 새 미션에 도전해보세요", fontSize = 12.sp, color = TextSub, maxLines = 1)
                }
                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = TextSub)
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                missions.forEach { mission ->
                    Row(
                        modifier = Modifier
                            .width(240.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(CardWhite)
                            .clickable { onMissionClick(mission.id) }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(CoralTint),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Flag, contentDescription = null, tint = Coral, modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(mission.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextMain, maxLines = 1)
                            Spacer(Modifier.height(2.dp))
                            Text(mission.region, fontSize = 12.sp, color = TextSub, maxLines = 1)
                        }
                        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = TextSub)
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // 전체 진행률
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.TrackChanges, contentDescription = null, tint = Coral, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("전체 미션 진행률", fontSize = 13.sp, color = TextMain, modifier = Modifier.weight(1f))
            Text("$percent%", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Coral)
        }
        Spacer(Modifier.height(8.dp))
        // 진행률 바
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFFF0DEDE))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(occupation.rate.coerceIn(0f, 1f))
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Coral)
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "${occupation.completedMissions} / ${occupation.totalMissions}",
            fontSize = 12.sp,
            color = TextSub,
            modifier = Modifier.align(Alignment.End)
        )
    }
}

@Composable
private fun SectionHeaderRow(title: String, onSeeAll: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextMain)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { onSeeAll() }
        ) {
            Text("전체보기", color = TextSub, fontSize = 13.sp)
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = TextSub, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun RecommendCard(rec: RecommendMission, onClick: () -> Unit) {
    val (badgeText, badgeBg, badgeTextColor) = when (rec.badge) {
        RecommendBadge.POPULAR -> Triple("인기", Coral, Color.White)
        RecommendBadge.NEW -> Triple("신규", SeaBlue, Color.White)
        RecommendBadge.RECOMMEND -> Triple("추천", MedalGold, Color(0xFF5A4300))
    }

    Column(
        modifier = Modifier
            .width(160.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(CardWhite)
            .clickable { onClick() }
            .padding(bottom = 14.dp)
    ) {
        // 이미지 자리표시 + 배지
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(SeaBlueBg)
        ) {
            Icon(
                Icons.Filled.Image,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .size(34.dp)
                    .align(Alignment.Center)
            )
            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(badgeBg)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(badgeText, color = badgeTextColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(10.dp))
        Text(rec.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextMain, maxLines = 1)
        Spacer(Modifier.height(3.dp))
        Text(rec.subtitle, fontSize = 12.sp, color = TextSub, maxLines = 1)

        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            PointsChip(rec.reward)
            Spacer(Modifier.weight(1f))
            Text(rec.distanceText, fontSize = 12.sp, color = TextSub)
        }
    }
}

@Composable
private fun PointsChip(points: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(Coral),
            contentAlignment = Alignment.Center
        ) {
            Text("P", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(5.dp))
        Text("$points", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Coral)
    }
}

// ── 아래 두 헬퍼는 미션/상세 화면에서도 사용하므로 유지 ──
fun missionTypeLabel(type: MissionType): String = when (type) {
    MissionType.PHOTO_LOCATION   -> "📷 사진 위치 인증"
    MissionType.CURRENT_LOCATION -> "📍 현재 위치 인증"
    MissionType.RECEIPT          -> "🧾 결제 영수증 인증"
}

fun verifyButtonLabel(type: MissionType): String = when (type) {
    MissionType.PHOTO_LOCATION   -> "📷 사진 올려서 인증하기"
    MissionType.CURRENT_LOCATION -> "📍 현재 위치로 인증하기"
    MissionType.RECEIPT          -> "🧾 영수증 올려서 인증하기"
}
