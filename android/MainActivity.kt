// MainActivity.kt
package com.example.busasnquest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.*
import androidx.navigation.compose.*

// ───────────────── COLORS ─────────────────

val BgSoftBlue = Color(0xFFF6F4F1)
val NavyMain = Color(0xFF22314E)
val NavyLight = Color(0xFF3B4D70)
val CardWhite = Color.White
val TextMain = Color(0xFF1E1E1E)
val TextSub = Color(0xFF888888)
val DividerGray = Color(0xFFE5E5E5)
val TrackGray = Color(0xFFE9ECF1)
val PointOrange = Color(0xFFFF9800)
val PointRed = Color(0xFFE94F4F)

// 랭킹 메달 색
val MedalGold = Color(0xFFF4B400)
val MedalSilver = Color(0xFFAEB6C2)
val MedalBronze = Color(0xFFCD7F45)

// 구·군 진행률 바 색상
val BarYellow = Color(0xFFF4C534)
val BarCoral = Color(0xFFEF6F6F)
val BarOrange = Color(0xFFF39A3E)
val BarPurple = Color(0xFF9B8CE0)

// 메뉴 아이콘 배경/틴트
val IconBlueBg = Color(0xFFE4ECFB)
val IconBlue = Color(0xFF3E6DDc)
val IconPinkBg = Color(0xFFFCE4EC)
val IconPink = Color(0xFFE8638F)
val IconGreenBg = Color(0xFFE3F3E8)
val IconGreen = Color(0xFF49A86B)

// ───────────────── 공통 데이터 ─────────────────

const val USER_POINT = "2,450P"
const val USER_NAME = "부산갈매기"

// ───────────────── DATA ─────────────────

data class OngoingMission(
    val title: String,
    val region: String,
    val reward: Int,
    val current: Int,
    val total: Int
)

// 구·군별 진행 현황
data class DistrictProgress(
    val name: String,
    val completed: Int,
    val total: Int,
    val color: Color
)

// 랭킹 항목
data class RankEntry(
    val rank: Int,
    val name: String,
    val score: String,
    val isMe: Boolean = false
)

// 내 정보 - 메뉴 카드
data class MenuItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val tint: Color,
    val bg: Color
)

// 내 정보 - 설정 항목
data class SettingItem(
    val title: String,
    val icon: ImageVector
)

// ───────────────── SAMPLE DATA (스크린샷 기준) ─────────────────

// 홈 - 진행 중인 미션
val ongoingMission = OngoingMission(
    title = "오록도 해안길 걷기",
    region = "남구 용호동",
    reward = 100,
    current = 0,
    total = 1
)

// 미션 - 구·군별 진행 현황 (16개)
val districtProgressList = listOf(
    DistrictProgress("중구", 3, 3, BarYellow),
    DistrictProgress("동구", 2, 2, BarCoral),
    DistrictProgress("해운대구", 2, 2, BarPurple),
    DistrictProgress("북구", 1, 2, BarCoral),
    DistrictProgress("동래구", 1, 2, BarOrange),
    DistrictProgress("수영구", 1, 2, BarCoral),
    DistrictProgress("남구", 0, 2, TrackGray),
    DistrictProgress("부산진구", 0, 2, TrackGray),
    DistrictProgress("금정구", 0, 2, TrackGray),
    DistrictProgress("연제구", 0, 2, TrackGray),
    DistrictProgress("사상구", 0, 1, TrackGray),
    DistrictProgress("사하구", 0, 1, TrackGray),
    DistrictProgress("서구", 0, 1, TrackGray),
    DistrictProgress("영도구", 0, 1, TrackGray),
    DistrictProgress("기장군", 0, 1, TrackGray),
    DistrictProgress("강서구", 0, 1, TrackGray),
)

// 랭킹 리스트
val rankingList = listOf(
    RankEntry(1, "바다사랑이", "5,620P"),
    RankEntry(2, "해운대모험가", "4,320P"),
    RankEntry(3, "광안리러버", "3,150P"),
    RankEntry(4, "부산산책자", "2,850P"),
    RankEntry(5, "푸른바다탐험가", "2,450P"),
    RankEntry(12, "부산갈매기 (나)", "2,450P", isMe = true),
    RankEntry(13, "송도바람", "2,340P"),
    RankEntry(14, "남포동여행자", "2,220P"),
    RankEntry(15, "영도나그네", "2,100P"),
    RankEntry(16, "자갈치사랑", "1,980P"),
)

// 내 정보 - 메뉴 카드
val profileMenuItems = listOf(
    MenuItem("미션 내역", "지금까지 완료한 미션을 확인해보세요", Icons.Outlined.Flag, IconBlue, IconBlueBg),
    MenuItem("찜한 미션", "찜해둔 미션을 모아볼 수 있어요", Icons.Filled.Favorite, IconPink, IconPinkBg),
    MenuItem("사진 관리", "미션 인증 사진을 관리하세요", Icons.Outlined.PhotoCamera, IconGreen, IconGreenBg),
)

// 내 정보 - 설정 리스트
val settingItems = listOf(
    SettingItem("알림 설정", Icons.Outlined.Notifications),
    SettingItem("계정 설정", Icons.Outlined.Person),
    SettingItem("고객센터", Icons.Outlined.HelpOutline),
    SettingItem("이용약관", Icons.Outlined.Description),
    SettingItem("개인정보처리방침", Icons.Outlined.Shield),
)

// ───────────────── ACTIVITY ─────────────────

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        android.util.Log.d("KeyHash", "===== onCreate 실행됨! =====")
        printKeyHash()   // ← ① 여기 추가 (setContent 위)

        setContent {
            MaterialTheme {
                BusanQuestApp()
            }
        }
    }

    // ↓ ② onCreate 닫는 } 다음, 클래스 닫는 } 안쪽에 함수 추가
    private fun printKeyHash() {
        try {
            val info = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageManager.getPackageInfo(
                    packageName,
                    android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(
                    packageName,
                    android.content.pm.PackageManager.GET_SIGNATURES
                )
            }

            val signatures = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                info.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                info.signatures
            }

            signatures?.forEach { signature ->
                val md = java.security.MessageDigest.getInstance("SHA")
                md.update(signature.toByteArray())
                val keyHash = android.util.Base64.encodeToString(
                    md.digest(),
                    android.util.Base64.NO_WRAP
                )
                android.util.Log.d("KeyHash", "키해시: $keyHash")
            }
        } catch (e: Exception) {
            android.util.Log.e("KeyHash", "에러: ${e.message}")
        }
    }
}

// ───────────────── APP ROOT ─────────────────

@Composable
fun BusanQuestApp() {

    val navController = rememberNavController()

    Scaffold(
        containerColor = BgSoftBlue,
        bottomBar = {
            BottomNavigationBar(navController)
        }
    ) { padding ->

        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(padding)
        ) {

            composable("home") { HomeScreen(navController) }

            composable("mission") { MissionScreen(navController) }

            composable(
                route = "map/{region}",
                arguments = listOf(
                    navArgument("region") { type = NavType.StringType }
                )
            ) {
                val region = it.arguments?.getString("region") ?: ""
                MapScreen(region)
            }

            composable("ranking") { RankingScreen() }

            composable("profile") { ProfileScreen() }
        }
    }
}

// ───────────────── BOTTOM NAV ─────────────────

@Composable
fun BottomNavigationBar(navController: NavHostController) {

    val currentRoute =
        navController.currentBackStackEntryAsState().value?.destination?.route

    fun navigateTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.startDestinationId) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(CardWhite)
            .border(1.dp, DividerGray, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {

        BottomItem("홈", Icons.Outlined.Home, currentRoute == "home") { navigateTab("home") }

        BottomItem("미션", Icons.Outlined.Flag, currentRoute == "mission") { navigateTab("mission") }

        BottomItem(
            "지도",
            Icons.Outlined.Map,
            currentRoute?.startsWith("map") == true
        ) { navigateTab("map/부산") }

        BottomItem("랭킹", Icons.Outlined.EmojiEvents, currentRoute == "ranking") { navigateTab("ranking") }

        BottomItem("내 정보", Icons.Outlined.Person, currentRoute == "profile") { navigateTab("profile") }
    }
}

@Composable
fun BottomItem(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    val color = if (selected) NavyMain else TextSub

    Column(
        modifier = Modifier.clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = title, tint = color)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            title,
            fontSize = 11.sp,
            color = color,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

// ───────────────── COMMON COMPONENTS ─────────────────

/**
 * 상단 헤더: 좌측에 제목 + (선택) 강조 단어 + 부제,
 * 우측에 포인트 배지 + 알림 벨.
 */
@Composable
fun ScreenHeader(
    title: String,
    highlight: String? = null,
    subtitle: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {

            // 제목 (강조 단어가 있으면 색을 다르게)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    title,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = NavyMain
                )
                if (highlight != null) {
                    Text(
                        highlight,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = PointRed
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                PointPill()
                Spacer(modifier = Modifier.width(10.dp))
                BellButton()
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(subtitle, color = TextSub, fontSize = 13.sp)
    }
}

@Composable
fun PointPill() {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(CardWhite)
            .border(1.dp, DividerGray, CircleShape)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Filled.Star,
            contentDescription = null,
            tint = PointOrange,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(USER_POINT, fontWeight = FontWeight.Bold, color = NavyMain, fontSize = 14.sp)
    }
}

@Composable
fun BellButton() {
    Box(
        modifier = Modifier
            .size(42.dp)
            .background(CardWhite, CircleShape)
            .border(1.dp, DividerGray, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Outlined.Notifications, contentDescription = "알림", tint = NavyMain)
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = TextMain,
        modifier = Modifier.padding(horizontal = 20.dp)
    )
}

/**
 * 점령률/진행률 카드 (네이비 배경 + 무지개 그라데이션 바).
 */
@Composable
fun ProgressCard(
    label: String,
    percentText: String,
    caption: String,
    progress: Float
) {
    Box(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(NavyMain)
            .padding(24.dp)
    ) {
        Column {
            Text(label, color = Color.White.copy(0.75f), fontSize = 14.sp)

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                percentText,
                fontSize = 42.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(caption, color = Color.White.copy(0.65f), fontSize = 13.sp)

            Spacer(modifier = Modifier.height(18.dp))

            GradientProgressBar(progress)
        }
    }
}

/**
 * 무지개 그라데이션 진행 바 + 0% / 50% / 100% 눈금.
 */
@Composable
fun GradientProgressBar(progress: Float) {
    val spectrum = listOf(
        Color(0xFFFF5A5A),
        Color(0xFFFF9800),
        Color(0xFFF4D03F),
        Color(0xFF8BC34A),
        Color(0xFF4FC3F7)
    )

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(CircleShape)
                .background(Color.White.copy(0.18f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(Brush.horizontalGradient(spectrum))
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("0%", color = Color.White.copy(0.6f), fontSize = 11.sp)
            Text("50%", color = Color.White.copy(0.6f), fontSize = 11.sp)
            Text("100%", color = Color.White.copy(0.6f), fontSize = 11.sp)
        }
    }
}

// ───────────────── HOME ─────────────────

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

// ───────────────── MISSION ─────────────────

@Composable
fun MissionScreen(navController: NavHostController) {

    var selectedTab by remember { mutableIntStateOf(0) }

    LazyColumn {

        item {
            ScreenHeader(
                title = "미션",
                subtitle = "다양한 미션을 완료하고 포인트를 모아보세요!"
            )

            ProgressCard(
                label = "전체 진행률",
                percentText = "35%",
                caption = "5/16 구·군 점령",
                progress = 0.35f
            )

            Spacer(modifier = Modifier.height(20.dp))

            SegmentedToggle(
                options = listOf("전체", "지역"),
                selectedIndex = selectedTab,
                onSelect = { selectedTab = it }
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (selectedTab == 0) "전체 지역 진행 현황" else "지역별 미션",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMain
                )
                FilterChipBox("전체")
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        items(districtProgressList) { district ->
            DistrictProgressRow(district) {
                navController.navigate("map/${district.name}")
            }
        }

        item { Spacer(modifier = Modifier.height(120.dp)) }
    }
}

/** 흰 배경 위의 둥근 세그먼트 토글 (전체 / 지역). */
@Composable
fun SegmentedToggle(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardWhite)
            .padding(4.dp)
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(13.dp))
                    .background(if (selected) NavyMain else Color.Transparent)
                    .clickable { onSelect(index) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    color = if (selected) Color.White else TextSub,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
fun FilterChipBox(label: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(CardWhite)
            .border(1.dp, DividerGray, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TextMain, fontSize = 13.sp)
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = TextSub,
            modifier = Modifier.size(16.dp)
        )
    }
}

/** 구·군 한 줄: 이름 + 진행 바 + 퍼센트 + 개수 + 화살표. */
@Composable
fun DistrictProgressRow(
    district: DistrictProgress,
    onClick: () -> Unit
) {
    val percent = if (district.total == 0) 0f
    else district.completed.toFloat() / district.total
    val percentInt = (percent * 100).toInt()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(
            district.name,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = TextMain,
            modifier = Modifier.width(64.dp)
        )

        // 진행 바 + 퍼센트
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    "$percentInt%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (percentInt == 0) TextSub else district.color
                )
            }
            Spacer(modifier = Modifier.height(5.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(TrackGray)
            ) {
                if (percent > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(percent)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(district.color)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Text(
            "${district.completed}/${district.total}",
            color = TextSub,
            fontSize = 13.sp
        )

        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = TextSub,
            modifier = Modifier.size(20.dp)
        )
    }
}

// ───────────────── MAP ─────────────────

@Composable
fun MapScreen(region: String) {
    Column(modifier = Modifier.fillMaxSize()) {

        ScreenHeader(
            title = "지도",
            subtitle = if (region == "부산") "구·군을 선택해 상세 정보를 확인하세요"
            else "$region 상세 정보를 확인하세요"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.verticalGradient(listOf(Color(0xFFE8F0FF), Color(0xFFDDE7F5)))
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Map,
                    contentDescription = null,
                    tint = NavyMain,
                    modifier = Modifier.size(80.dp)
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    if (region == "부산") "부산 전체 지도" else "$region 지도 확대",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = NavyMain
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("여기에 부산 지도 이미지 삽입", color = TextSub)
            }
        }
    }
}

// ───────────────── RANKING ─────────────────

@Composable
fun RankingScreen() {

    var selectedTab by remember { mutableIntStateOf(0) }

    LazyColumn {

        item {
            ScreenHeader(
                title = "랭킹",
                subtitle = "다른 유저들과 함께 순위를 확인해보세요!"
            )

            MyRankCard(
                rankText = "12",
                topPercent = "상위 8%",
                point = "2,450P",
                selectedTab = selectedTab,
                onSelectTab = { selectedTab = it }
            )

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                FilterChipBox("전체")
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        items(rankingList) { entry ->
            RankingRow(entry)
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFEDF1F6))
                    .clickable { }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("더보기", color = TextSub, fontWeight = FontWeight.Bold)
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = TextSub
                    )
                }
            }
            Spacer(modifier = Modifier.height(120.dp))
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
                val tabs = listOf("전체 랭킹", "지역 랭킹", "친구 랭킹")
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

// ───────────────── PROFILE (내 정보) ─────────────────

@Composable
fun ProfileScreen() {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {

        ScreenHeader(
            title = "내 정보",
            subtitle = "나의 활동과 정보를 확인하세요!"
        )

        ProfileSummaryCard()

        Spacer(modifier = Modifier.height(20.dp))

        // 메뉴 카드 (미션 내역 / 찜한 미션 / 사진 관리)
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(CardWhite)
        ) {
            profileMenuItems.forEachIndexed { index, item ->
                MenuRow(item)
                if (index != profileMenuItems.lastIndex) {
                    HorizontalDivider(
                        color = DividerGray,
                        modifier = Modifier.padding(horizontal = 18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 설정 리스트
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(CardWhite)
        ) {
            settingItems.forEachIndexed { index, item ->
                SettingRow(item)
                if (index != settingItems.lastIndex) {
                    HorizontalDivider(
                        color = DividerGray,
                        modifier = Modifier.padding(horizontal = 18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 로그아웃
        Box(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFFF3E1E1))
                .clickable { }
                .padding(vertical = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("로그아웃", color = PointRed, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(120.dp))
    }
}

@Composable
fun ProfileSummaryCard() {
    Column(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(CardWhite)
            .padding(24.dp)
    ) {

        Row(verticalAlignment = Alignment.CenterVertically) {

            // 아바타 자리표시자
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(
                        Brush.verticalGradient(listOf(Color(0xFFCFE0F2), Color(0xFFB7D0EA))),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Person,
                    contentDescription = null,
                    tint = NavyMain,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.width(18.dp))

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(USER_NAME, fontWeight = FontWeight.Bold, fontSize = 19.sp, color = TextMain)
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        Icons.Outlined.Edit,
                        contentDescription = "이름 편집",
                        tint = TextSub,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("부산을 사랑하는 여행자", color = TextSub, fontSize = 13.sp)
                Text("부산의 매력을 찾아 미션에 도전해요!", color = TextSub, fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        HorizontalDivider(color = DividerGray)
        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ProfileStat("2,450P", "보유 포인트")
            ProfileStat("86", "완료 미션")
            ProfileStat("28", "찜한 미션")
        }
    }
}

@Composable
fun ProfileStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = NavyMain, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, color = TextSub, fontSize = 12.sp)
    }
}

@Composable
fun MenuRow(item: MenuItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(item.bg, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(item.icon, contentDescription = null, tint = item.tint, modifier = Modifier.size(22.dp))
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(item.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextMain)
            Spacer(modifier = Modifier.height(2.dp))
            Text(item.subtitle, color = TextSub, fontSize = 12.sp)
        }

        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSub)
    }
}

@Composable
fun SettingRow(item: SettingItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(item.icon, contentDescription = null, tint = NavyMain, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(item.title, fontSize = 15.sp, color = TextMain, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSub)
    }
}