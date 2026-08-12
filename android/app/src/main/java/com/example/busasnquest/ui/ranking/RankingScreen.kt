package com.example.busasnquest.ui.ranking

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.busasnquest.data.model.RankEntry
import com.example.busasnquest.ui.components.ErrorView
import com.example.busasnquest.ui.components.ScreenHeader
import com.example.busasnquest.ui.components.SkeletonBox
import com.example.busasnquest.ui.theme.CardWhite
import com.example.busasnquest.ui.theme.Coral
import com.example.busasnquest.ui.theme.Dimens
import com.example.busasnquest.ui.theme.CoralDark
import com.example.busasnquest.ui.theme.CoralTint
import com.example.busasnquest.ui.theme.InkBorder
import com.example.busasnquest.ui.theme.MedalBronze
import com.example.busasnquest.ui.theme.MedalGold
import com.example.busasnquest.ui.theme.MedalSilver
import com.example.busasnquest.ui.theme.Motion
import com.example.busasnquest.ui.theme.OnCoralTint
import com.example.busasnquest.ui.theme.SeaBlue
import com.example.busasnquest.ui.theme.SeaBlueBg
import com.example.busasnquest.ui.theme.TextMain
import com.example.busasnquest.ui.theme.TextSub
import com.example.busasnquest.ui.theme.accentStyle
import com.example.busasnquest.ui.theme.displayStyle
import com.example.busasnquest.ui.theme.pressable
import com.example.busasnquest.ui.theme.CoralInk
import androidx.navigation.NavHostController

@Composable
fun RankingScreen(
    navController: NavHostController,
    // 서버 연동: 탭 선택 시 /api/v1/rankings?type=all|region|friend 호출
    viewModel: RankingViewModel = viewModel(factory = RankingViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()

    when (val s = state) {
        is RankingUiState.Loading -> {
            RankingSkeleton()
        }

        is RankingUiState.Error -> {
            ErrorView(message = s.message, onRetry = viewModel::retry)
        }

        is RankingUiState.Success -> {
            LazyColumn(
                contentPadding = PaddingValues(bottom = Dimens.bottomBarSpace)
            ) {
                item {
                    ScreenHeader(
                        title = "랭킹",
                        subtitle = "다른 유저들과 함께 순위를 확인해보세요!"
                    )

                    MyRankCard(
                        rankText = s.myRank,
                        topPercent = s.topPercent,
                        point = s.point,
                        selectedTab = selectedTab,
                        onSelectTab = viewModel::onSelectTab
                    )
                    Spacer(modifier = Modifier.height(Dimens.gapBlock))
                }

                if (selectedTab == 1) {
                    // 지역 랭킹: 16개 구 목록
                    items(busanDistricts) { district ->
                        DistrictRankRow(district) {
                            navController.navigate("districtRanking/$district")
                        }
                    }
                } else {
                    // 전체 랭킹: 상위 3명은 포디움, 나머지는 리스트
                    item {
                        RankingPodium(s.rankings.take(3))
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                    items(s.rankings.drop(3)) { entry ->
                        RankingRow(entry)
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }
}

/** 랭킹 로딩 스켈레톤: 세그먼트 pill + 포디움 원 3개 + 리스트 줄 5개 */
@Composable
private fun RankingSkeleton() {
    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(
            title = "랭킹",
            subtitle = "다른 유저들과 함께 순위를 확인해보세요!"
        )

        Column(modifier = Modifier.padding(horizontal = Dimens.screenPadding)) {
            SkeletonBox(
                Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                shape = CircleShape
            )
            Spacer(Modifier.height(Dimens.sectionGap))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.Bottom
            ) {
                SkeletonBox(Modifier.size(46.dp), shape = CircleShape)
                Spacer(Modifier.width(20.dp))
                SkeletonBox(Modifier.size(60.dp), shape = CircleShape)
                Spacer(Modifier.width(20.dp))
                SkeletonBox(Modifier.size(46.dp), shape = CircleShape)
            }
            Spacer(Modifier.height(Dimens.sectionGap))

            repeat(5) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SkeletonBox(Modifier.size(30.dp), shape = CircleShape)
                    Spacer(Modifier.width(12.dp))
                    SkeletonBox(
                        Modifier
                            .weight(1f)
                            .height(14.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    SkeletonBox(
                        Modifier
                            .width(52.dp)
                            .height(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MyRankCard(
    rankText: String,
    topPercent: String,
    point: String,
    selectedTab: Int,
    onSelectTab: (Int) -> Unit
) {
    Box(
        modifier = Modifier
            .padding(horizontal = Dimens.screenPadding)
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.radiusHero))
            .background(Coral)
            .border(Dimens.borderWidth, InkBorder, RoundedCornerShape(Dimens.radiusHero))
            .padding(Dimens.cardPadding + 6.dp)
    ) {
        Column {

            Text("내 순위", color = Color.White.copy(0.85f), fontSize = 14.sp)

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.EmojiEvents,
                    contentDescription = null,
                    tint = MedalGold,
                    modifier = Modifier.size(64.dp)
                )

                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        // 순위 숫자 — 액센트 폰트
                        Text(
                            rankText,
                            color = Color.White,
                            style = accentStyle(48.sp)
                        )
                        Text(
                            "위",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 8.dp, start = 2.dp)
                        )
                    }
                    Text(topPercent, color = Color.White.copy(0.7f), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(2.dp))
                    // 내 포인트 — 앱 공통 표시 (네이비 카드 위라 숫자는 흰색)
                    com.example.busasnquest.ui.components.PointAmount(
                        text = point,
                        badgeSize = 18.dp,
                        badgeFontSize = 10.sp,
                        fontSize = 17.sp,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 전체/지역/친구 랭킹 세그먼트
            val trackShape = RoundedCornerShape(Dimens.radiusButton)
            val thumbShape = RoundedCornerShape(Dimens.radiusButton - 4.dp)  // 트랙 안쪽 여백만큼 뺀다
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(trackShape)
                    .background(CoralDark)
                    .border(Dimens.borderWidth, InkBorder, trackShape)
                    .padding(4.dp)
            ) {
                val tabs = listOf("전체 랭킹", "지역 랭킹")
                tabs.forEachIndexed { index, label ->
                    val selected = index == selectedTab
                    // 세그먼트는 위치 이동 없이 색만 넘긴다
                    val thumbBg by animateColorAsState(
                        targetValue = if (selected) Color.White else Color.Transparent,
                        animationSpec = tween(Motion.DurRelease, easing = Motion.EaseOut),
                        label = "rankTabBg"
                    )
                    val thumbFg by animateColorAsState(
                        targetValue = if (selected) Coral else Color.White.copy(0.85f),
                        animationSpec = tween(Motion.DurRelease, easing = Motion.EaseOut),
                        label = "rankTabFg"
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(thumbShape)
                            .background(thumbBg)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onSelectTab(index) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label,
                            color = if (selected) CoralDark else Color.White.copy(0.85f),
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RankingRow(
    entry: RankEntry,
    // 점수가 포인트면 P 뱃지를 붙인다. 지역 랭킹처럼 "3개" 같은 값이면 false.
    scoreIsPoint: Boolean = true
) {

    val medalColor = when (entry.rank) {
        1 -> MedalGold
        2 -> MedalSilver
        3 -> MedalBronze
        else -> null
    }

    // 내 행은 크림 배경이라 다크 테마 글자색을 그대로 쓰면 안 읽힌다 → 전경색을 배경에 맞춰 뒤집는다
    val fgMain = if (entry.isMe) OnCoralTint else TextMain
    val fgAccent = if (entry.isMe) OnCoralTint else Coral

    Row(
        modifier = Modifier
            .padding(horizontal = Dimens.screenPadding, vertical = 5.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.radiusCard))
            .background(if (entry.isMe) CoralTint else CardWhite)
            .border(
                Dimens.borderWidth,
                if (entry.isMe) Coral else InkBorder,
                RoundedCornerShape(Dimens.radiusCard)
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {

        Row(verticalAlignment = Alignment.CenterVertically) {

            // 순위 (1~3위는 메달 배지)
            Box(
                modifier = Modifier.width(32.dp),
                contentAlignment = Alignment.Center
            ) {
                if (medalColor != null) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .background(medalColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "${entry.rank}",
                            color = CoralInk,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    Text(
                        "${entry.rank}",
                        fontWeight = FontWeight.Bold,
                        color = CoralDark,
                        fontSize = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // 프로필 아이콘 자리표시자
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(SeaBlueBg, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Person,
                    contentDescription = null,
                    tint = SeaBlue,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Text(
                entry.name,
                fontWeight = if (entry.isMe) FontWeight.Bold else FontWeight.Medium,
                color = fgMain,
                fontSize = 15.sp
            )
        }

        if (scoreIsPoint) {
            // 앱 공통 포인트 표시 (홈 헤더와 같은 모양)
            com.example.busasnquest.ui.components.PointAmount(
                text = entry.score,
                badgeSize = 16.dp,
                badgeFontSize = 9.sp,
                fontSize = 15.sp,
                gap = 4.dp
            )
        } else {
            Text(
                entry.score,
                fontWeight = FontWeight.Bold,
                color = CoralDark,
                fontSize = 15.sp
            )
        }
    }
}

// 상위 3명 포디움
@Composable
fun RankingPodium(top: List<RankEntry>) {
    if (top.isEmpty()) return
    // 2등 - 1등 - 3등 순서로 배치
    val ordered = listOf(
        top.getOrNull(1) to 2,
        top.getOrNull(0) to 1,
        top.getOrNull(2) to 3
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.screenPadding, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        ordered.forEach { (entry, place) ->
            if (entry != null) {
                PodiumColumn(entry, place, Modifier.weight(1f))
            } else {
                Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PodiumColumn(entry: RankEntry, place: Int, modifier: Modifier = Modifier) {
    val medal = when (place) {
        1 -> MedalGold
        2 -> MedalSilver
        else -> MedalBronze
    }
    val avatarSize = if (place == 1) 60.dp else 48.dp
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(avatarSize)
                .background(medal, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("$place", color = CoralInk, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            entry.name,
            color = TextMain,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            maxLines = 1
        )
        // 시상대 점수 — 앱 공통 포인트 표시 (작은 크기)
        com.example.busasnquest.ui.components.PointAmount(
            text = entry.score,
            badgeSize = 12.dp,
            badgeFontSize = 7.sp,
            fontSize = 11.sp,
            color = TextSub,
            gap = 3.dp
        )
    }
}

// 부산 16개 구·군 (랭킹 지역 목록용)
val busanDistricts = listOf(
    "중구", "서구", "동구", "영도구", "부산진구", "동래구",
    "남구", "북구", "해운대구", "사하구", "금정구", "강서구",
    "연제구", "수영구", "사상구", "기장군"
)

@Composable
fun DistrictRankRow(district: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .padding(horizontal = Dimens.screenPadding, vertical = 5.dp)
            .fillMaxWidth()
            // 독립된 카드형 행이라 scale 로 눌린다 (카드 안 리스트 행이면 pressableRow)
            .pressable(onClick = onClick)
            .clip(RoundedCornerShape(Dimens.radiusCard))
            .background(CardWhite)
            .border(Dimens.borderWidth, InkBorder, RoundedCornerShape(Dimens.radiusCard))
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(district, fontWeight = FontWeight.Bold, color = TextMain, fontSize = 15.sp)
        Icon(
            Icons.Default.KeyboardArrowRight,
            contentDescription = null,
            tint = TextSub
        )
    }
}
