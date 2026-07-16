package com.example.busasnquest.ui.ranking

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.busasnquest.data.model.RankEntry
import com.example.busasnquest.ui.components.ScreenHeader
import com.example.busasnquest.ui.theme.CardWhite
import com.example.busasnquest.ui.theme.IconBlue
import com.example.busasnquest.ui.theme.IconBlueBg
import com.example.busasnquest.ui.theme.MedalBronze
import com.example.busasnquest.ui.theme.MedalGold
import com.example.busasnquest.ui.theme.MedalSilver
import com.example.busasnquest.ui.theme.NavyLight
import com.example.busasnquest.ui.theme.NavyMain
import com.example.busasnquest.ui.theme.TextMain
import com.example.busasnquest.ui.theme.TextSub
import androidx.navigation.NavHostController

@Composable
fun RankingScreen(
    navController: NavHostController,
    viewModel: RankingViewModel = viewModel(factory = RankingViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()

    when (val s = state) {
        is RankingUiState.Loading -> {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        is RankingUiState.Error -> {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text(s.message, color = TextSub)
            }
        }

        is RankingUiState.Success -> {
            LazyColumn {
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
                    Spacer(modifier = Modifier.height(18.dp))
                }

                if (selectedTab == 1) {
                    // 지역 랭킹: 16개 구 목록
                    items(busanDistricts) { district ->
                        DistrictRankRow(district) {
                            navController.navigate("districtRanking/$district")
                        }
                    }
                } else {
                    // 전체 랭킹: 순위 목록
                    items(s.rankings) { entry ->
                        RankingRow(entry)
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(120.dp))
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
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(NavyMain)
            .padding(24.dp)
    ) {
        Column {

            Text("내 순위", color = Color.White.copy(0.8f), fontSize = 14.sp)

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
                        Text(
                            rankText,
                            color = Color.White,
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "위",
                            color = Color.White,
                            fontSize = 18.sp,
                            modifier = Modifier.padding(bottom = 8.dp, start = 2.dp)
                        )
                    }
                    Text(topPercent, color = Color.White.copy(0.7f), fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(point, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 전체/지역/친구 랭킹 세그먼트
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(NavyLight)
                    .padding(4.dp)
            ) {
                val tabs = listOf("전체 랭킹", "지역 랭킹")
                tabs.forEachIndexed { index, label ->
                    val selected = index == selectedTab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(11.dp))
                            .background(if (selected) Color.White else Color.Transparent)
                            .clickable { onSelectTab(index) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label,
                            color = if (selected) NavyMain else Color.White.copy(0.8f),
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
fun RankingRow(entry: RankEntry) {

    val medalColor = when (entry.rank) {
        1 -> MedalGold
        2 -> MedalSilver
        3 -> MedalBronze
        else -> null
    }

    Row(
        modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 5.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (entry.isMe) Color(0xFFFDF1E0) else CardWhite)
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
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    Text(
                        "${entry.rank}",
                        fontWeight = FontWeight.Bold,
                        color = NavyMain,
                        fontSize = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // 프로필 아이콘 자리표시자
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(IconBlueBg, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Person,
                    contentDescription = null,
                    tint = IconBlue,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Text(
                entry.name,
                fontWeight = if (entry.isMe) FontWeight.Bold else FontWeight.Medium,
                color = TextMain,
                fontSize = 15.sp
            )
        }

        Text(
            entry.score,
            fontWeight = FontWeight.Bold,
            color = NavyMain,
            fontSize = 15.sp
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
            .padding(horizontal = 20.dp, vertical = 5.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardWhite)
            .clickable { onClick() }
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
