package com.example.busasnquest.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.busasnquest.data.model.OngoingMission
import com.example.busasnquest.data.model.ongoingMission
import com.example.busasnquest.ui.components.ProgressCard
import com.example.busasnquest.ui.components.ScreenHeader
import com.example.busasnquest.ui.components.SectionTitle
import com.example.busasnquest.ui.theme.*
import androidx.compose.material3.Button
import com.example.busasnquest.data.repository.UserRepository

@Composable
fun HomeScreen(navController: NavHostController) {

    LazyColumn {

        item {
            ScreenHeader(
                title = "부산 ",
                highlight = "땅따먹기",
                subtitle = "숨은 보물을 찾고, 부산을 점령하자!"
            )

            ProgressCard(
                label = "나의 점령률",
                percentText = "35%",
                caption = "5/16 구·군 점령",
                progress = 0.35f
            )

            Spacer(modifier = Modifier.height(24.dp))

            SectionTitle("지도에서 지역을 선택하세요")
            Spacer(modifier = Modifier.height(12.dp))

            MapPlaceholder {
                navController.navigate("map/부산")
            }

            Spacer(modifier = Modifier.height(28.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("진행 중인 미션", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextMain)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { navController.navigate("mission") }
                ) {
                    Text("더보기", color = TextSub, fontSize = 13.sp)
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = TextSub,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OngoingMissionCard(ongoingMission)

            Spacer(modifier = Modifier.height(28.dp))

            SectionTitle("최근 점령 지역")
            Spacer(modifier = Modifier.height(12.dp))

            RecentCapturedCard("해운대구", "점령일 2024.05.16")

            Spacer(modifier = Modifier.height(120.dp))
        }
    }
}

/**
 * 부산 지도 자리표시자.
 * 실제 지도 이미지를 넣으려면 이 Box 안의 내용을
 * Image(painter = painterResource(R.drawable.busan_map), ...) 로 교체하세요.
 */
@Composable
fun MapPlaceholder(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .height(280.dp)
            .shadow(4.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(listOf(Color(0xFFE8F0FF), Color(0xFFDDE7F5)))
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Map,
                contentDescription = null,
                tint = NavyMain,
                modifier = Modifier.size(72.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("여기에 부산 지도 이미지 삽입", fontWeight = FontWeight.Bold, color = NavyMain)
            Spacer(modifier = Modifier.height(6.dp))
            Text("탭하여 지도 화면으로 이동", color = TextSub, fontSize = 13.sp)
        }
    }
}

@Composable
fun OngoingMissionCard(mission: OngoingMission) {
    Column(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CardWhite)
    ) {

        // 미션 이미지 자리표시자 + '지역 미션' 배지
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .background(
                    Brush.verticalGradient(listOf(Color(0xFFCFE0F2), Color(0xFFB7D0EA)))
                )
        ) {
            Icon(
                Icons.Default.Image,
                contentDescription = null,
                tint = Color.White.copy(0.8f),
                modifier = Modifier
                    .size(48.dp)
                    .align(Alignment.Center)
            )
            Box(
                modifier = Modifier
                    .padding(12.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(IconGreen)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("지역 미션", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Column(modifier = Modifier.padding(18.dp)) {

            Text(mission.title, fontWeight = FontWeight.Bold, fontSize = 17.sp)

            Spacer(modifier = Modifier.height(6.dp))

            Text(mission.region, color = TextSub, fontSize = 13.sp)

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Star,
                    contentDescription = null,
                    tint = PointOrange,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("+${mission.reward}P", fontWeight = FontWeight.Bold, color = PointOrange)
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { mission.current.toFloat() / mission.total },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CircleShape),
                color = PointOrange,
                trackColor = TrackGray
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text("${mission.current}/${mission.total}", color = TextSub, fontSize = 12.sp)
        }
    }
}

@Composable
fun RecentCapturedCard(region: String, date: String) {

    Row(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CardWhite)
            .padding(18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(IconGreenBg, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = IconGreen,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(region, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(date, color = TextSub, fontSize = 12.sp)
            }
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSub)
    }
}
