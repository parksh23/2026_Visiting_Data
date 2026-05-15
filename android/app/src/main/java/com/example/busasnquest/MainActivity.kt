package com.example.busasnquest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── 새 디자인 색상 정의 (Navy & Soft Blue Theme) ──────────────────
val BgSoftBlue    = Color(0xFFF2F6FA) // 전체 배경 (연한 블루그레이)
val NavyMain      = Color(0xFF22314E) // 메인 네이비 (상단 카드, 플로팅 버튼)
val NavyLight     = Color(0xFF3B4D70) // 보조 네이비
val PointOrange   = Color(0xFFFF9800) // 포인트 오렌지 (별, 포인트 텍스트)
val CardWhite     = Color(0xFFFFFFFF)
val TextMain      = Color(0xFF1E1E1E)
val TextSub       = Color(0xFF888888)
val DividerGray   = Color(0xFFE5E5E5)

// 태그 색상
val TagRedBg      = Color(0xFFFFEAEA)
val TagRedText    = Color(0xFFE53935)
val TagGreenBg    = Color(0xFFE8F5E9)
val TagGreenText  = Color(0xFF43A047)
val TagBlueBg     = Color(0xFFE8EAF6)
val TagBlueText   = Color(0xFF3F51B5)

// ── 데이터 모델 ────────────────────────────────────────────────────
data class MissionItem(
    val title: String,
    val location: String,
    val points: Int,
    val tag: String,
    val currentStep: Int = 0,
    val maxStep: Int = 1,
    val isCompleted: Boolean = false
)

// ── 샘플 데이터 ────────────────────────────────────────────────────
val sampleMissions = listOf(
    MissionItem("초량 시장에서 로컬 결제하기", "동구 초량동", 120, "로컬결제"),
    MissionItem("오륙도 해안길 걷기", "남구 용호동", 100, "둘레길"),
    MissionItem("40년 이상 노포 방문하기", "서구 보수동", 150, "노포 방문")
)

val regionalMissions = listOf(
    MissionItem("금정산성 케이블카 탑승하기", "금정구", 120, "", isCompleted = true),
    MissionItem("태종대 유원지 방문하기", "영도구", 100, "", isCompleted = false),
    MissionItem("광안리 해변 산책하기", "수영구", 80, "", isCompleted = false)
)

// ── MainActivity ──────────────────────────────────────────────────
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                BusanQuestApp()
            }
        }
    }
}

// ── 앱 루트 & 커스텀 바텀 네비게이션 ──────────────────────────────
@Composable
fun BusanQuestApp() {
    var currentScreen by remember { mutableStateOf("홈") }

    Scaffold(
        containerColor = BgSoftBlue,
        bottomBar = {
            CustomBottomNavigation(currentScreen) { currentScreen = it }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (currentScreen) {
                "홈"       -> HomeScreen()
                "미션"     -> MissionScreen()
                "지도"     -> MapScreen()
                "랭킹"     -> RankingPlaceholderScreen()
                "내 정보"  -> ProfileScreen()
            }
        }
    }
}

@Composable
fun CustomBottomNavigation(currentScreen: String, onTabSelected: (String) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)
            .background(Color.Transparent)
    ) {
        // 하단 흰색 바
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(65.dp)
                .align(Alignment.BottomCenter)
                .background(CardWhite)
                .border(1.dp, DividerGray.copy(alpha = 0.5f), RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            BottomNavItem("홈", Icons.Outlined.Home, currentScreen, onTabSelected)
            BottomNavItem("미션", Icons.Outlined.Flag, currentScreen, onTabSelected)
            Spacer(modifier = Modifier.width(60.dp)) // 중앙 지도 버튼 공간
            BottomNavItem("랭킹", Icons.Outlined.EmojiEvents, currentScreen, onTabSelected)
            BottomNavItem("내 정보", Icons.Outlined.Person, currentScreen, onTabSelected)
        }

        // 중앙 플로팅 지도 버튼
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-5).dp)
                .clickable { onTabSelected("지도") },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(65.dp)
                    .shadow(8.dp, CircleShape)
                    .background(NavyMain, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Map, contentDescription = "지도", tint = CardWhite, modifier = Modifier.size(28.dp))
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text("지도", fontSize = 11.sp, fontWeight = if (currentScreen == "지도") FontWeight.Bold else FontWeight.Normal, color = if (currentScreen == "지도") NavyMain else TextSub)
        }
    }
}

@Composable
fun BottomNavItem(title: String, icon: ImageVector, currentScreen: String, onTabSelected: (String) -> Unit) {
    val isSelected = currentScreen == title
    val color = if (isSelected) NavyMain else TextSub
    Column(
        modifier = Modifier
            .width(60.dp)
            .clickable { onTabSelected(title) },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = title, tint = color, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(title, fontSize = 11.sp, color = color, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
    }
}

// ── 공통 컴포넌트: 상단 헤더 ──────────────────────────────────────
@Composable
fun TopHeader(title: String, subtitle: String? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(title, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = NavyMain)
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(subtitle, fontSize = 13.sp, color = TextSub)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // 포인트 칩
            Row(
                modifier = Modifier
                    .background(CardWhite, RoundedCornerShape(20.dp))
                    .border(1.dp, DividerGray, RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Star, contentDescription = "Point", tint = PointOrange, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("2,450P", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextMain)
            }
            // 알림 아이콘
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(CardWhite, CircleShape)
                    .border(1.dp, DividerGray, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Notifications, contentDescription = "알림", tint = NavyMain, modifier = Modifier.size(20.dp))
            }
        }
    }
}

// ── 공통 컴포넌트: 점령률 다크 카드 ──────────────────────────────
@Composable
fun ProgressDarkCard(title: String, percent: Int, subtitle: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(NavyMain)
            .padding(20.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(title, fontSize = 13.sp, color = CardWhite.copy(alpha = 0.8f))
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("$percent", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = CardWhite)
                    Text("%", fontSize = 20.sp, fontWeight = FontWeight.Medium, color = CardWhite, modifier = Modifier.padding(bottom = 6.dp))
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(subtitle, fontSize = 12.sp, color = CardWhite.copy(alpha = 0.6f))
            }

            // 분할선
            Box(modifier = Modifier.width(1.dp).height(80.dp).background(CardWhite.copy(alpha = 0.2f)))

            Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
                Text("다음 보상까지 15%", fontSize = 12.sp, color = CardWhite.copy(alpha = 0.8f))
                Spacer(modifier = Modifier.height(8.dp))
                // 세그먼트 프로그레스 바
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    val colors = listOf(Color(0xFFF28B82), Color(0xFFFBB660), Color(0xFFFCE182), Color(0xFFA6DDA6), NavyLight, NavyLight)
                    colors.forEach { color ->
                        Box(modifier = Modifier.weight(1f).height(8.dp).background(color, RoundedCornerShape(4.dp)))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("0%", fontSize = 10.sp, color = CardWhite.copy(alpha = 0.6f))
                    Text("50%", fontSize = 10.sp, color = CardWhite.copy(alpha = 0.6f))
                    Text("100%", fontSize = 10.sp, color = CardWhite.copy(alpha = 0.6f))
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(start = 12.dp)) {
                Box(
                    modifier = Modifier.size(48.dp).background(NavyLight, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🎁", fontSize = 24.sp)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text("다음 보상", fontSize = 11.sp, color = CardWhite.copy(alpha = 0.8f))
            }
        }
    }
}

// ── 1. 홈 화면 (Home) ──────────────────────────────────────────────
@Composable
fun HomeScreen() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 100.dp) // 바텀 네비게이션 여백
    ) {
        item {
            TopHeader("부산 땅따먹기", "숨은 로컬을 찾고, 부산을 점령하자!")
            ProgressDarkCard("나의 점령률", 35, "5/16 구·군 점령")
        }

        // 지도 (플레이스홀더 영역)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                // 지도 배경 시뮬레이션
                Box(modifier = Modifier.fillMaxSize().background(Color(0xFFE8EFE8).copy(alpha = 0.5f)))
                Text("🗺️\n상세 지도는\n'지도' 탭에서 확인하세요", textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = TextSub)

                // 좌측 플로팅 버튼들
                Column(
                    modifier = Modifier.align(Alignment.TopStart).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(modifier = Modifier.background(CardWhite, RoundedCornerShape(20.dp)).padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.FormatListBulleted, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("지역 목록", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.background(CardWhite, RoundedCornerShape(20.dp)).padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.PieChart, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("점령 현황", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 오늘의 미션
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardWhite, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .padding(top = 24.dp, bottom = 24.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("오늘의 미션", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextMain)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("더보기", fontSize = 12.sp, color = TextSub)
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSub, modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(sampleMissions) { mission ->
                            MissionCard(mission)
                        }
                    }
                }
            }
        }
    }
}

// ── 2. 미션 화면 (Mission) ─────────────────────────────────────────
@Composable
fun MissionScreen() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        item {
            TopHeader("미션", "부산을 탐험하고 포인트를 모아보세요!")
            ProgressDarkCard("나의 진행률", 35, "5/16 미션 완료")
            Spacer(modifier = Modifier.height(24.dp))
        }

        // 탭 버튼
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TabPill("전체 미션", true)
                TabPill("지역 미션", false)
                TabPill("로컬 결제", false)
                TabPill("방문 미션", false)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // 추천 미션
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("추천 미션", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextMain)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("더보기", fontSize = 12.sp, color = TextSub)
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSub, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(sampleMissions) { mission ->
                    MissionCard(mission)
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }

        // 지역 미션 리스트
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("지역 미션", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextMain)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("더보기", fontSize = 12.sp, color = TextSub)
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSub, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        items(regionalMissions) { mission ->
            RegionalMissionRow(mission)
        }
    }
}

// ── 3. 지도 화면 (Map) ─────────────────────────────────────────────
@Composable
fun MapScreen() {
    Column(modifier = Modifier.fillMaxSize()) {
        TopHeader("지도", "부산의 매력을 발견하고 미션을 완료해보세요!")

        // 탭 버튼
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TabPill("전체", true, Icons.Outlined.GridView)
            TabPill("로컬결제", false, Icons.Outlined.CreditCard)
            TabPill("둘레길", false, Icons.Outlined.DirectionsWalk)
        }

        // 지도 & 바텀시트
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            // 지도 배경
            Box(modifier = Modifier.fillMaxSize().background(Color(0xFFE8EFE8).copy(alpha = 0.5f))) {
                Text("🗺️\n전체 지도 영역", modifier = Modifier.align(Alignment.Center), color = TextSub)
                // 우측 하단 컨트롤러
                Column(modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 140.dp, end = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.size(48.dp).background(CardWhite, CircleShape).shadow(2.dp, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.MyLocation, contentDescription = null, tint = TextMain)
                    }
                    Box(modifier = Modifier.size(48.dp).background(CardWhite, CircleShape).shadow(2.dp, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Navigation, contentDescription = null, tint = TextMain)
                    }
                }
            }

            // 하단 진행 현황 시트
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(CardWhite)
                    .padding(top = 12.dp, bottom = 100.dp, start = 24.dp, end = 24.dp) // 바텀 네비게이션 여백
            ) {
                Column {
                    Box(modifier = Modifier.width(40.dp).height(4.dp).background(DividerGray, RoundedCornerShape(2.dp)).align(Alignment.CenterHorizontally))
                    Spacer(modifier = Modifier.height(20.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("지역별 진행 현황", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextMain)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("자세히 보기", fontSize = 12.sp, color = TextSub)
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSub, modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatusCard("완료 지역", "4곳", Icons.Filled.Flag, Modifier.weight(1f))
                        StatusCard("진행 중인 지역", "5곳", Icons.Filled.CheckCircle, Modifier.weight(1f))
                        StatusCard("잠금 해제 필요", "7곳", Icons.Filled.Lock, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

// ── 4. 내 정보 화면 (Profile) ──────────────────────────────────────
@Composable
fun ProfileScreen() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        item {
            TopHeader("부산 땅따먹기")
            Spacer(modifier = Modifier.height(8.dp))
        }

        // 프로필 헤더 카드
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .background(CardWhite, RoundedCornerShape(20.dp))
                    .padding(24.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // 아바타
                        Box(
                            modifier = Modifier
                                .size(70.dp)
                                .background(BgSoftBlue, CircleShape)
                                .border(2.dp, BgSoftBlue, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🦆", fontSize = 40.sp) // 임시 오리 아바타
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("부산러버", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextMain)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Outlined.Edit, contentDescription = "Edit", tint = TextSub, modifier = Modifier.size(14.dp))
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Lv.12 로컬 마스터", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TagBlueText)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = TextSub, modifier = Modifier.size(12.dp))
                                Text(" 부산광역시 해운대구", fontSize = 12.sp, color = TextSub)
                            }
                            Text(" 가입일 2024.05.10", fontSize = 12.sp, color = TextSub)
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSub)
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Divider(color = DividerGray, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(16.dp))

                    // 스탯 요약
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        ProfileStat("2,450P", "보유 포인트", Icons.Filled.Star, PointOrange)
                        Box(modifier = Modifier.width(1.dp).height(40.dp).background(DividerGray))
                        ProfileStat("96", "점령 지역", Icons.Filled.Flag, TagBlueText)
                        Box(modifier = Modifier.width(1.dp).height(40.dp).background(DividerGray))
                        ProfileStat("28", "미션 완료", Icons.Filled.EmojiEvents, PointOrange)
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // 나의 활동 그리드
        item {
            Text("나의 활동", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextMain, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))

            // 2x3 그리드 구현 (Column & Row)
            Column(modifier = Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ActivityCard("점령 현황", "내가 점령한 지역 보기", Icons.Outlined.LocationOn, TagBlueBg, TagBlueText, Modifier.weight(1f))
                    ActivityCard("미션 내역", "완료한 미션 확인", Icons.Outlined.Stars, TagGreenBg, TagGreenText, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ActivityCard("랭킹 기록", "나의 랭킹 변화 보기", Icons.Outlined.EmojiEvents, Color(0xFFF3E5F5), Color(0xFF8E24AA), Modifier.weight(1f))
                    ActivityCard("찜한 장소", "찜해둔 장소 목록", Icons.Outlined.BookmarkBorder, TagRedBg, TagRedText, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ActivityCard("리뷰 관리", "내가 작성한 리뷰 보기", Icons.Outlined.Edit, Color(0xFFFFF8E1), PointOrange, Modifier.weight(1f))
                    ActivityCard("사진 관리", "업로드한 사진 보기", Icons.Outlined.CameraAlt, BgSoftBlue, NavyLight, Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }

        // 설정 리스트
        item {
            Text("설정", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextMain, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
            Column(modifier = Modifier.fillMaxWidth().background(CardWhite)) {
                SettingRow(Icons.Outlined.Notifications, "알림 설정")
                SettingRow(Icons.Outlined.Lock, "개인정보 설정")
                SettingRow(Icons.Outlined.HelpOutline, "도움말")
                SettingRow(Icons.Outlined.Info, "앱 정보", value = "v1.3.0")
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // 로그아웃 버튼
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .background(CardWhite, RoundedCornerShape(16.dp))
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.Logout, contentDescription = null, tint = TagRedText, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("로그아웃", fontSize = 14.sp, color = TagRedText, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ── 플레이스홀더 화면 ──────────────────────────────────────────────
@Composable
fun RankingPlaceholderScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("랭킹 화면 준비중입니다.", color = TextSub)
    }
}

// ── UI 조각 컴포넌트들 ─────────────────────────────────────────────

@Composable
fun MissionCard(mission: MissionItem) {
    Card(
        modifier = Modifier.width(200.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BgSoftBlue)
    ) {
        Column {
            // 이미지 영역 (플레이스홀더)
            Box(modifier = Modifier.fillMaxWidth().height(110.dp).background(Color(0xFFD9E2EC))) {
                // 태그
                val (tagBg, tagColor) = when(mission.tag) {
                    "로컬결제" -> TagRedBg to TagRedText
                    "둘레길" -> TagGreenBg to TagGreenText
                    else -> TagBlueBg to TagBlueText
                }
                Text(
                    text = mission.tag,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = CardWhite,
                    modifier = Modifier.padding(12.dp).background(tagColor, RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            // 내용 영역
            Column(modifier = Modifier.padding(16.dp)) {
                Text(mission.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextMain, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(4.dp))
                Text(mission.location, fontSize = 11.sp, color = TextSub)
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Star, contentDescription = null, tint = PointOrange, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+${mission.points}P", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextMain)
                }
                Spacer(modifier = Modifier.height(12.dp))
                // 프로그레스 바
                Box(modifier = Modifier.fillMaxWidth().height(24.dp).background(Color.White, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                    Text("${mission.currentStep}/${mission.maxStep}", fontSize = 11.sp, color = TextSub)
                }
            }
        }
    }
}

@Composable
fun RegionalMissionRow(mission: MissionItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .background(CardWhite, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 원형 이미지 플레이스홀더
        Box(modifier = Modifier.size(60.dp).background(BgSoftBlue, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Image, contentDescription = null, tint = TextSub)
            if (mission.isCompleted) {
                Box(
                    modifier = Modifier.align(Alignment.BottomEnd).offset(x=4.dp, y=4.dp).size(20.dp).background(CardWhite, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = TagGreenText, modifier = Modifier.size(18.dp))
                }
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(mission.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextMain)
            Spacer(modifier = Modifier.height(2.dp))
            Text(mission.location, fontSize = 12.sp, color = TextSub)
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Star, contentDescription = null, tint = PointOrange, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("+${mission.points}P", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextMain)
            }
        }

        if (!mission.isCompleted) {
            Box(
                modifier = Modifier.background(BgSoftBlue, RoundedCornerShape(20.dp)).padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text("도전하기", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSub)
            }
        }
    }
}

@Composable
fun TabPill(text: String, isSelected: Boolean, icon: ImageVector? = null) {
    Row(
        modifier = Modifier
            .background(if (isSelected) NavyMain else CardWhite, RoundedCornerShape(20.dp))
            .border(1.dp, if (isSelected) Color.Transparent else DividerGray, RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = if (isSelected) CardWhite else TextSub, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (isSelected) CardWhite else TextSub)
    }
}

@Composable
fun StatusCard(title: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(BgSoftBlue, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Icon(icon, contentDescription = null, tint = NavyLight, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text(title, fontSize = 11.sp, color = TextSub)
        Spacer(modifier = Modifier.height(2.dp))
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextMain)
    }
}

@Composable
fun ProfileStat(value: String, label: String, icon: ImageVector, tint: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextMain)
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, fontSize = 11.sp, color = TextSub)
    }
}

@Composable
fun ActivityCard(title: String, subtitle: String, icon: ImageVector, iconBg: Color, iconTint: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(CardWhite, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(40.dp).background(iconBg, CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextMain)
            Text(subtitle, fontSize = 10.sp, color = TextSub)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = DividerGray, modifier = Modifier.size(16.dp))
    }
}

@Composable
fun SettingRow(icon: ImageVector, title: String, value: String? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { }.padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = NavyLight, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(title, fontSize = 14.sp, color = TextMain, modifier = Modifier.weight(1f))
        if (value != null) {
            Text(value, fontSize = 12.sp, color = TextSub)
        } else {
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = DividerGray)
        }
    }
}