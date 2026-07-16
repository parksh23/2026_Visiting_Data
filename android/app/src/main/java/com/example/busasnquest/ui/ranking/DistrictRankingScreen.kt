package com.example.busasnquest.ui.ranking

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.busasnquest.data.model.RankEntry
import com.example.busasnquest.data.repository.MissionRepository
import com.example.busasnquest.ui.theme.*

@Composable
fun DistrictRankingScreen(
    navController: NavHostController,
    districtName: String
) {
    // 내가 이 구에서 완료한 미션 수 (실제 데이터)
    val myCount = MissionRepository.completedCountInDistrict(districtName)

    // 가짜 다른 사용자들 + 내 실제 점수를 섞어서 순위 만들기
    val rankings = buildDistrictRanking(districtName, myCount)

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
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로", tint = NavyMain)
            }
            Text("$districtName 랭킹", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = NavyMain)
        }

        // 내 점수 안내
        Box(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(NavyMain)
                .padding(20.dp)
        ) {
            Column {
                Text("$districtName 에서 나의 기록", color = Color.White.copy(0.8f), fontSize = 13.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text("완료한 미션 ${myCount}개", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            contentPadding = PaddingValues(bottom = Dimens.bottomBarSpace)
        ) {
            items(rankings) { entry ->
                RankingRow(entry)
            }
            item { Spacer(modifier = Modifier.height(40.dp)) }
        }
    }
}

// 구별 가짜 랭킹 + 내 실제 점수 섞기
fun buildDistrictRanking(district: String, myCount: Int): List<RankEntry> {
    // 가짜 다른 사용자들 (이 구에서 완료한 미션 수 기준)
    val others = listOf(
        "바다사랑이" to 5,
        "해운대모험가" to 4,
        "광안리러버" to 3,
        "부산산책자" to 2,
        "푸른바다탐험가" to 1
    )

    // 나를 포함해서 점수순 정렬
    val all = others.map { it.first to it.second } + ("부산갈매기 (나)" to myCount)
    val sorted = all.sortedByDescending { it.second }

    return sorted.mapIndexed { index, (name, count) ->
        RankEntry(
            rank = index + 1,
            name = name,
            score = "${count}개",
            isMe = name == "부산갈매기 (나)"
        )
    }
}