package com.example.busasnquest.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import com.example.busasnquest.R
import com.example.busasnquest.data.model.MissionType
import com.example.busasnquest.data.model.OngoingMission
import com.example.busasnquest.data.repository.OccupationStat
import com.example.busasnquest.ui.theme.*
import com.kakao.vectormap.LatLng
import com.example.busasnquest.ui.components.KakaoMapView
import com.kakao.vectormap.camera.CameraUpdateFactory

// 미니맵: 히어로 카드와 같은 라운드를 써서 상단 블록들이 같은 계열로 읽히게 한다.
// 높이는 170 → 150dp — 조작 불가한 요약용 지도가 첫 화면을 과하게 차지하지 않도록.
private val MiniMapRadius = Dimens.radiusHero
private val MiniMapHeight = 150.dp

@Composable
fun HomeScreen(
    navController: NavHostController,
    viewModel: HomeViewModel = viewModel()
) {
    val missions by viewModel.homeMissions.collectAsStateWithLifecycle()
    val occupation by viewModel.occupation.collectAsStateWithLifecycle()
    val recommended by viewModel.recommendedMissions.collectAsStateWithLifecycle()
    val points by viewModel.points.collectAsStateWithLifecycle()
    val name by viewModel.name.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = bottomBarSpacing())
    ) {
        Spacer(Modifier.height(Dimens.gapTight))

        // 앱 로고 + 인사말 헤더
        HomeHeader(points = points, name = name)

        Spacer(Modifier.height(Dimens.gapBlock))

        // 검색 바
        SearchPill { navController.navigate("map/부산?focus=true") }

        Spacer(Modifier.height(Dimens.cardGap))

        // 현위치 미니맵 — 현위치 라벨을 지도 위에 얹어 상단 블록을 3개 → 2개로 줄였다.
        HomeMiniMap(location = "부산광역시") { navController.navigate("map/부산") }

        Spacer(Modifier.height(Dimens.sectionGap))

        // 진행중인 미션 요약
        OngoingSummaryCard(
            occupation = occupation,
            missions = missions.map { it.mission },
            onMissionClick = { id -> navController.navigate("missionDetail/$id") },
            onEmptyClick = { navController.navigate("mission") }
        )

        Spacer(Modifier.height(Dimens.sectionGap))

        // 추천 미션
        SectionHeaderRow("추천 미션") { navController.navigate("mission") }
        Spacer(Modifier.height(Dimens.cardGap))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = Dimens.screenPadding),
            horizontalArrangement = Arrangement.spacedBy(Dimens.cardGap)
        ) {
            recommended.forEach { rec ->
                RecommendCard(rec) { navController.navigate("missionDetail/${rec.id}") }
            }
        }

        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun HomeHeader(points: Int, name: String) {
    Column(modifier = Modifier.padding(horizontal = Dimens.screenPadding)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 로고 락업 (조각난 땅 + 깃발 심볼 + "부산 땅따먹기")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.ic_logo_symbol),
                    contentDescription = "부산 땅따먹기",
                    modifier = Modifier
                        .width(58.dp)
                        .height(44.dp)
                )
                Spacer(Modifier.width(10.dp))
                // 워드마크 — 디스플레이 헤딩(콘덴스드), 강조 글자만 브랜드 액센트색
                Row {
                    Text("부산 ", style = displayStyle(26.sp), color = TextMain)
                    Text("땅", style = displayStyle(26.sp), color = Coral)
                    Text("따먹기", style = displayStyle(26.sp), color = TextMain)
                }
            }
            // 포인트 — 앱 공통 표시 (모든 탭이 이 모양을 따른다)
            com.example.busasnquest.ui.components.PointAmount(value = points)
        }
        Spacer(Modifier.height(Dimens.gapTight))
        Text(
            "${name.ifBlank { "탐험가" }}님, 오늘도 부산을 정복해볼까요?",
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
            .padding(horizontal = Dimens.screenPadding)
            .fillMaxWidth()
            // pressable 은 표면(clip/background)보다 먼저 — 알약 전체가 같이 눌린다
            .pressable(onClick = onClick)
            .raisedSurface(RoundedCornerShape(Dimens.radiusPill), elevation = 5.dp)
            .padding(horizontal = 20.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("어느 동네를 정복할까요?", color = TextSub, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Icon(Icons.Filled.Search, contentDescription = "검색", tint = TextSub, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun HomeMiniMap(location: String, onOpenMap: () -> Unit) {
    // 터치를 받는 건 지도 위 투명 오버레이(=지도 조작 차단)지만,
    // 눌린 느낌은 카드 전체에 걸려야 하므로 interactionSource 를 공유한다.
    val press = rememberPressState(scaleDown = 0.985f)

    Box(
        modifier = Modifier
            .padding(horizontal = Dimens.screenPadding)
            .fillMaxWidth()
            .height(MiniMapHeight)
            .scale(press.scale)
            .clip(RoundedCornerShape(MiniMapRadius))
            .background(SurfaceGray)
            .border(Dimens.borderWidth, InkBorder, RoundedCornerShape(MiniMapRadius))
    ) {
        // 생명주기가 붙은 공용 지도 컴포저블 (직접 MapView 를 만들지 말 것 — 복귀 시 먹통 원인)
        KakaoMapView(
            modifier = Modifier.fillMaxSize(),
            onMapError = { error ->
                if (com.example.busasnquest.BuildConfig.DEBUG) {
                    android.util.Log.e("HomeMiniMap", "지도 에러: ${error?.message}")
                }
            },
            onMapReady = { kakaoMap ->
                val busan = LatLng.from(35.1796, 129.0756)
                kakaoMap.moveCamera(
                    CameraUpdateFactory.newCenterPosition(busan, 12)
                )
            }
        )

        // 투명 오버레이: 미니맵은 조작 불가, 탭하면 지도 탭으로만 이동
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(
                    interactionSource = press.interactionSource,
                    indication = null,
                    onClick = onOpenMap
                )
        )

        // 현위치 칩 — 지도 위에 얹어 별도 줄을 없앴다. 지도 탭 자체가 클릭 영역이라 칩은 라벨 역할만.
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(10.dp)
                .clip(CircleShape)
                .background(CardWhite.copy(alpha = 0.94f))
                .border(1.dp, InkBorder.copy(alpha = 0.5f), CircleShape)
                .padding(start = 10.dp, end = 12.dp, top = 7.dp, bottom = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.LocationOn, contentDescription = null, tint = Coral, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(location, color = TextMain, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(2.dp))
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = TextSub, modifier = Modifier.size(15.dp))
        }
    }
}

@Composable
private fun OngoingSummaryCard(
    occupation: OccupationStat,
    missions: List<OngoingMission>,
    onMissionClick: (Int) -> Unit,
    onEmptyClick: () -> Unit
) {
    // 진행률은 "값이 바뀐 것"을 읽히게 하는 모션이라 애니메이션 대상.
    // animateFloatAsState 는 첫 컴포지션에서는 목표값에서 시작하므로 화면을 열 때마다 다시 차오르지 않는다.
    val rate = occupation.rate.coerceIn(0f, 1f)
    val animatedRate by animateFloatAsState(
        targetValue = rate,
        animationSpec = tween(Motion.DurValue, easing = Motion.EaseOut),
        label = "occupationRate"
    )
    val percent = (animatedRate * 100).toInt()

    Column(
        modifier = Modifier
            .padding(horizontal = Dimens.screenPadding)
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.radiusHero))
            .background(CoralTint)
            .border(Dimens.borderWidth, InkBorder, RoundedCornerShape(Dimens.radiusHero))
            .padding(20.dp)
    ) {
        // 크림색(CoralTint) 배경 위 전경색은 Color.kt 의 OnCoralTint* 토큰을 쓴다
        Text("진행중인 미션", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = OnCoralTint)
        Spacer(Modifier.height(4.dp))
        Text(
            "미션을 선택하면\n자세한 정보를 확인할 수 있어요.",
            fontSize = 13.sp,
            color = OnCoralTintSub
        )

        Spacer(Modifier.height(Dimens.gapBlock))

        // 진행 중인 미션 카드 (여러 개면 옆으로 스와이프)
        if (missions.isEmpty()) {
            OngoingRow(
                title = "도전 중인 미션이 없어요",
                subtitle = "미션 탭에서 새 미션에 도전해보세요",
                modifier = Modifier.fillMaxWidth(),
                onClick = onEmptyClick
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                missions.forEach { mission ->
                    OngoingRow(
                        title = mission.title,
                        subtitle = mission.region,
                        modifier = Modifier.width(240.dp),
                        onClick = { onMissionClick(mission.id) }
                    )
                }
            }
        }

        Spacer(Modifier.height(Dimens.gapBlock))

        // 전체 진행률
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.TrackChanges, contentDescription = null, tint = Coral, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("전체 미션 진행률", fontSize = 13.sp, color = Color(0xFF4A2E28), modifier = Modifier.weight(1f))
            Text("$percent%", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = CoralDark)
        }
        Spacer(Modifier.height(Dimens.gapTight))
        // 진행률 바
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(CoralTrack)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedRate)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Coral)
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "${occupation.completedMissions} / ${occupation.totalMissions}",
            fontSize = 12.sp,
            color = OnCoralTintSub,
            modifier = Modifier.align(Alignment.End)
        )
    }
}

/** 진행중 미션 행 — 비었을 때/있을 때 마크업이 똑같아 한 곳으로 합쳤다. */
@Composable
private fun OngoingRow(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .pressable(onClick = onClick)
            .clip(RoundedCornerShape(Dimens.radiusCard))
            .background(CardWhite)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(Dimens.radiusChip))
                .background(CoralTint),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Flag, contentDescription = null, tint = Coral, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextMain, maxLines = 1)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, fontSize = 12.sp, color = TextSub, maxLines = 1)
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = TextSub)
    }
}

@Composable
private fun SectionHeaderRow(title: String, onSeeAll: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.screenPadding),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 섹션 제목 — 디스플레이 헤딩
        Text(title, style = displayStyle(20.sp), color = TextMain)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            // 텍스트 링크는 터치 타깃이 작으므로 눌림도 작게 (0.94 는 과함)
            modifier = Modifier.pressable(scaleDown = 0.96f, onClick = onSeeAll)
        ) {
            Text("전체보기", color = TextSub, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = TextSub, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun RecommendCard(rec: RecommendMission, onClick: () -> Unit) {
    // 배지 글자색은 채움색에서 자동으로 뽑는다 (흰 글자는 이 팔레트에서 대부분 대비 미달)
    val (badgeText, badgeBg) = when (rec.badge) {
        RecommendBadge.POPULAR -> "인기" to Coral
        RecommendBadge.NEW -> "신규" to SeaBlue
        RecommendBadge.RECOMMEND -> "추천" to MedalGold
    }
    val badgeTextColor = onFilled(badgeBg)

    // 카드 안쪽 이미지의 위 모서리는 카드 radius - 테두리 두께로 맞춘다 (틈 방지)
    val innerTopRadius = Dimens.radiusCard - Dimens.borderWidth

    Column(
        modifier = Modifier
            .width(160.dp)
            .pressable(onClick = onClick)
            .clip(RoundedCornerShape(Dimens.radiusCard))
            .background(CardWhite)
            .border(Dimens.borderWidth, InkBorder, RoundedCornerShape(Dimens.radiusCard))
            .padding(bottom = 14.dp)
    ) {
        // 대표 사진 + 배지
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = innerTopRadius,
                        topEnd = innerTopRadius,
                        bottomStart = 0.dp,
                        bottomEnd = 0.dp
                    )
                )
                .background(SeaBlueBg)
        ) {
            if (!rec.imageUrl.isNullOrBlank()) {
                // 서버 대표 사진
                AsyncImage(
                    model = rec.imageUrl,
                    contentDescription = rec.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
            } else {
                // 사진이 없는 미션: 플레이스홀더 아이콘
                Icon(
                    Icons.Filled.Image,
                    contentDescription = null,
                    tint = SeaBlue,
                    modifier = Modifier
                        .size(34.dp)
                        .align(Alignment.Center)
                )
            }
            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .clip(CircleShape)
                    .background(badgeBg)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(badgeText, color = badgeTextColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(10.dp))
        Text(
            rec.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextMain, maxLines = 1,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        Spacer(Modifier.height(3.dp))
        Text(
            rec.subtitle, fontSize = 12.sp, color = TextSub, maxLines = 1,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PointsChip(rec.reward)
            Spacer(Modifier.weight(1f))
            Text(rec.distanceText, fontSize = 12.sp, color = TextSub)
        }
    }
}

@Composable
private fun PointsChip(points: Int) {
    com.example.busasnquest.ui.components.PointAmount(
        value = points,
        badgeSize = 18.dp,
        badgeFontSize = 10.sp,
        fontSize = 14.sp
    )
}

// ── 아래 두 헬퍼는 미션/상세 화면에서도 사용하므로 유지 ──
fun missionTypeLabel(type: MissionType): String = when (type) {
    MissionType.IMAGE_LOCATION   -> "📷 사진 위치 인증"
    MissionType.CURRENT_LOCATION -> "📍 현재 위치 인증"
    MissionType.RECEIPT          -> "🧾 결제 영수증 인증"
}

fun verifyButtonLabel(type: MissionType): String = when (type) {
    MissionType.IMAGE_LOCATION   -> "📷 사진 올려서 인증하기"
    MissionType.CURRENT_LOCATION -> "📍 현재 위치로 인증하기"
    MissionType.RECEIPT          -> "🧾 영수증 올려서 인증하기"
}
