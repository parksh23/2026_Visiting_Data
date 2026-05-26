// MainActivity.kt
package com.example.busanquest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument

// ───────────────── COLORS ─────────────────

val BgSoftBlue = Color(0xFFF2F6FA)
val NavyMain = Color(0xFF22314E)
val NavyLight = Color(0xFF3B4D70)
val CardWhite = Color.White
val TextMain = Color(0xFF1E1E1E)
val TextSub = Color(0xFF888888)
val DividerGray = Color(0xFFE5E5E5)
val PointOrange = Color(0xFFFF9800)


// ───────────────── DATA ─────────────────

data class RegionProgress(
    val region: String,
    val completed: Int,
    val total: Int,
    val description: String   // 지역 간략 정보
)

data class OngoingMission(
    val title: String,
    val region: String,
    val progress: Float,
    val reward: Int
)

data class RankingInfo(
    val global: Int,
    val regional: Int,
    val friend: Int
)

// 3선발 친구 랭킹
data class FriendRank(
    val name: String,
    val score: Int,
    val isMe: Boolean = false
)

// 프로필 - 미션 내역
data class MissionRecord(
    val title: String,
    val region: String,
    val date: String,
    val reward: Int,
    val done: Boolean
)

// 프로필 - 찜한 미션
data class FavoriteMission(
    val title: String,
    val region: String,
    val reward: Int
)

// 프로필 - 사진 관리
data class PhotoItem(
    val title: String,
    val region: String
)


// ───────────────── SAMPLE DATA ─────────────────

val ongoingMissions = listOf(
    OngoingMission("광안리 해변 인증샷 찍기", "수영구", 0.7f, 120),
    OngoingMission("남포동 로컬 맛집 방문", "중구", 0.4f, 100)
)

val regionProgressList = listOf(
    RegionProgress("해운대구", 8, 10, "해수욕장과 마린시티 일대 미션이 모여 있는 지역입니다."),
    RegionProgress("수영구", 5, 10, "광안리와 광안대교 야경 미션을 즐길 수 있는 지역입니다."),
    RegionProgress("금정구", 3, 10, "범어사와 금정산 등산로 중심의 미션 지역입니다."),
    RegionProgress("영도구", 9, 10, "흰여울문화마을과 태종대를 포함한 섬 지역입니다."),
    RegionProgress("동래구", 4, 10, "온천과 동래읍성 등 역사 명소 중심 지역입니다."),
)

// 3선발 친구 랭킹 (점수 내림차순)
val friendRanks = listOf(
    FriendRank("민지", 2840),
    FriendRank("나 (JWJJ)", 2560, isMe = true),
    FriendRank("준호", 1980)
)

val missionRecords = listOf(
    MissionRecord("해운대 해변 인증샷", "해운대구", "2026.05.20", 120, true),
    MissionRecord("광안대교 야경 촬영", "수영구", "2026.05.18", 150, true),
    MissionRecord("범어사 방문 미션", "금정구", "2026.05.15", 100, true),
    MissionRecord("남포동 로컬 맛집 방문", "중구", "진행중", 100, false),
    MissionRecord("광안리 해변 인증샷 찍기", "수영구", "진행중", 120, false),
)

val favoriteMissions = listOf(
    FavoriteMission("태종대 등대 트레킹", "영도구", 200),
    FavoriteMission("흰여울문화마을 산책", "영도구", 130),
    FavoriteMission("동래온천 체험", "동래구", 110),
)

val photoItems = listOf(
    PhotoItem("해운대 해변", "해운대구"),
    PhotoItem("광안대교 야경", "수영구"),
    PhotoItem("범어사 전경", "금정구"),
    PhotoItem("흰여울문화마을", "영도구"),
    PhotoItem("동래읍성", "동래구"),
    PhotoItem("남포동 거리", "중구"),
)


// ───────────────── ACTIVITY ─────────────────

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

            composable("home") {
                HomeScreen(navController)
            }

            composable("mission") {
                MissionScreen(navController)
            }

            composable(
                route = "map/{region}",
                arguments = listOf(
                    navArgument("region") {
                        type = NavType.StringType
                    }
                )
            ) {
                val region = it.arguments?.getString("region") ?: ""
                MapScreen(region)
            }

            composable("ranking") {
                RankingScreen()
            }

            composable("profile") {
                ProfileScreen()
            }
        }
    }
}


// ───────────────── BOTTOM NAV ─────────────────

@Composable
fun BottomNavigationBar(
    navController: NavHostController
) {

    val currentRoute =
        navController.currentBackStackEntryAsState()
            .value
            ?.destination
            ?.route

    // 탭 이동 시 백스택이 쌓이지 않도록 공통 옵션 적용
    fun navigateTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.startDestinationId) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(
                RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            )
            .background(CardWhite)
            .border(
                1.dp,
                DividerGray,
                RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {

        BottomItem("홈", Icons.Outlined.Home, currentRoute == "home") {
            navigateTab("home")
        }

        BottomItem("미션", Icons.Outlined.Flag, currentRoute == "mission") {
            navigateTab("mission")
        }

        BottomItem(
            "지도",
            Icons.Outlined.Map,
            currentRoute?.startsWith("map") == true
        ) {
            navigateTab("map/부산")
        }

        BottomItem("랭킹", Icons.Outlined.EmojiEvents, currentRoute == "ranking") {
            navigateTab("ranking")
        }

        BottomItem("프로필", Icons.Outlined.Person, currentRoute == "profile") {
            navigateTab("profile")
        }
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


// ───────────────── HOME ─────────────────

@Composable
fun HomeScreen(
    navController: NavHostController
) {

    // 지역 카드 확장 상태 (간략 정보 표시용)
    var expandedRegion by remember { mutableStateOf<String?>(null) }

    LazyColumn {

        item {
            Header("부산 땅따먹기")
            ProgressCard()
        }

        item {
            SectionTitle("현재 진행중인 미션")
            Spacer(modifier = Modifier.height(12.dp))
        }

        items(ongoingMissions) {
            OngoingMissionCard(it)
        }

        item {
            Spacer(modifier = Modifier.height(28.dp))
            SectionTitle("지역별 진행 현황")
            Spacer(modifier = Modifier.height(12.dp))
        }

        items(regionProgressList) { region ->

            ExpandableRegionCard(
                region = region,
                expanded = expandedRegion == region.region,
                onToggle = {
                    expandedRegion =
                        if (expandedRegion == region.region) null
                        else region.region
                },
                onDetail = {
                    // 상세 -> 지도탭 이동, 해당 구 확대
                    navController.navigate("map/${region.region}")
                }
            )
        }

        item {
            Spacer(modifier = Modifier.height(120.dp))
        }
    }
}


// ───────────────── MISSION ─────────────────

@Composable
fun MissionScreen(
    navController: NavHostController
) {

    var selectedTab by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {

        Header("미션")

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = BgSoftBlue
        ) {

            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("전체") }
            )

            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("지역") }
            )
        }

        AnimatedContent(
            targetState = selectedTab,
            label = "missionTab"
        ) { tab ->

            when (tab) {

                // 전체: 전체 진행률 + 각 구/군별 미션 진행률
                0 -> {

                    LazyColumn {

                        item {
                            ProgressCard()
                            Spacer(modifier = Modifier.height(20.dp))
                            SectionTitle("구·군별 미션 진행률")
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        items(regionProgressList) { region ->
                            RegionProgressCard(
                                region = region,
                                onClick = {
                                    navController.navigate("map/${region.region}")
                                }
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(120.dp))
                        }
                    }
                }

                // 지역: 구/군별 미션 목록
                else -> {

                    LazyColumn {

                        items(regionProgressList) { region ->
                            RegionMissionSection(region)
                        }

                        item {
                            Spacer(modifier = Modifier.height(120.dp))
                        }
                    }
                }
            }
        }
    }
}


// ───────────────── MAP ─────────────────

@Composable
fun MapScreen(region: String) {

    Column(
        modifier = Modifier.fillMaxSize()
    ) {

        Header("지도")

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFFE8F0FF), Color(0xFFDDE7F5))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Icon(
                    Icons.Default.Map,
                    contentDescription = null,
                    tint = NavyMain,
                    modifier = Modifier.size(80.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    if (region == "부산") "부산 전체 지도"
                    else "$region 지도 확대",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = NavyMain
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    if (region == "부산") "구·군을 선택해 상세 정보를 확인하세요"
                    else "$region 상세 정보 표시",
                    color = TextSub
                )
            }
        }
    }
}


// ───────────────── RANKING ─────────────────

@Composable
fun RankingScreen() {

    val ranking = RankingInfo(
        global = 124,
        regional = 8,
        friend = friendRanks.indexOfFirst { it.isMe } + 1
    )

    LazyColumn {

        item {
            Header("랭킹")

            // 최상단: 현재 내 순위 (전체 / 지역 / 친구)
            RankingCard(ranking)

            Spacer(modifier = Modifier.height(20.dp))

            // 3선발 친구 랭킹
            SectionTitle("친구 랭킹 (선발 3인)")
            Spacer(modifier = Modifier.height(12.dp))
        }

        items(friendRanks.size) { index ->
            FriendRankRow(
                rank = index + 1,
                friend = friendRanks[index]
            )
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
            SectionTitle("TOP 플레이어")
            Spacer(modifier = Modifier.height(12.dp))
        }

        items(10) {
            RankingRow(
                rank = it + 1,
                name = "플레이어 ${it + 1}",
                score = "${3000 - (it * 120)}P"
            )
        }

        item {
            Spacer(modifier = Modifier.height(120.dp))
        }
    }
}


// ───────────────── PROFILE ─────────────────

@Composable
fun ProfileScreen() {

    var selectedTab by remember { mutableStateOf(0) }

    val tabs = listOf("활동", "미션 내역", "찜한 미션", "사진 관리")

    Column(
        modifier = Modifier.fillMaxSize()
    ) {

        Header("프로필")

        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = BgSoftBlue,
            edgePadding = 12.dp
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }

        AnimatedContent(
            targetState = selectedTab,
            label = "profileTab"
        ) { tab ->

            when (tab) {
                0 -> ProfileActivityTab()
                1 -> MissionRecordTab()
                2 -> FavoriteMissionTab()
                else -> PhotoManageTab()
            }
        }
    }
}


@Composable
fun ProfileActivityTab() {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {

        // 사용자 요약 카드
        Box(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth()
                .shadow(10.dp, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(NavyMain)
                .padding(24.dp)
        ) {
            Column {
                Text("JWJJ", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("부산 탐험가 · Lv.5", color = Color.White.copy(0.7f))

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatColumn("보유 포인트", "2,560P")
                    StatColumn("완료 미션", "3개")
                    StatColumn("점령 지역", "29곳")
                }
            }
        }

        SectionTitle("최근 활동")
        Spacer(modifier = Modifier.height(12.dp))

        missionRecords.filter { it.done }.forEach { record ->
            MissionRecordCard(record)
        }

        Spacer(modifier = Modifier.height(120.dp))
    }
}


@Composable
fun StatColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = PointOrange, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, color = Color.White.copy(0.7f), fontSize = 12.sp)
    }
}


@Composable
fun MissionRecordTab() {

    LazyColumn {
        item {
            Spacer(modifier = Modifier.height(8.dp))
        }
        items(missionRecords) { record ->
            MissionRecordCard(record)
        }
        item {
            Spacer(modifier = Modifier.height(120.dp))
        }
    }
}


@Composable
fun MissionRecordCard(record: MissionRecord) {

    Row(
        modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(CardWhite)
            .padding(20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {

        Column(modifier = Modifier.weight(1f)) {
            Text(record.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text("${record.region} · ${record.date}", color = TextSub, fontSize = 13.sp)
        }

        Column(horizontalAlignment = Alignment.End) {
            Icon(
                if (record.done) Icons.Default.CheckCircle else Icons.Default.Schedule,
                contentDescription = null,
                tint = if (record.done) PointOrange else TextSub
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "+${record.reward}P",
                color = if (record.done) PointOrange else TextSub,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
    }
}


@Composable
fun FavoriteMissionTab() {

    LazyColumn {
        item {
            Spacer(modifier = Modifier.height(8.dp))
        }
        items(favoriteMissions) { fav ->

            Row(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
                    .background(CardWhite)
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Column(modifier = Modifier.weight(1f)) {
                    Text(fav.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("${fav.region} · +${fav.reward}P", color = TextSub, fontSize = 13.sp)
                }

                Icon(
                    Icons.Default.Favorite,
                    contentDescription = "찜한 미션",
                    tint = PointOrange
                )
            }
        }
        item {
            Spacer(modifier = Modifier.height(120.dp))
        }
    }
}


@Composable
fun PhotoManageTab() {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = 8.dp)
    ) {

        // LazyVerticalGrid를 스크롤 Column 안에 중첩하면 크래시가 나므로
        // chunked로 직접 2열 그리드 구성
        photoItems.chunked(2).forEach { rowItems ->

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                rowItems.forEach { photo ->
                    PhotoCell(photo, Modifier.weight(1f))
                }

                // 홀수 개일 때 마지막 칸 빈 공간 채우기
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        Spacer(modifier = Modifier.height(120.dp))
    }
}


@Composable
fun PhotoCell(photo: PhotoItem, modifier: Modifier = Modifier) {

    Column(
        modifier = modifier
            .shadow(4.dp, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .background(CardWhite)
    ) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFFE8F0FF), Color(0xFFDDE7F5))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Photo,
                contentDescription = null,
                tint = NavyMain,
                modifier = Modifier.size(36.dp)
            )
        }

        Column(modifier = Modifier.padding(12.dp)) {
            Text(photo.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(photo.region, color = TextSub, fontSize = 12.sp)
        }
    }
}


// ───────────────── COMPONENTS ─────────────────

@Composable
fun Header(title: String) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(
            title,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = NavyMain
        )

        Box(
            modifier = Modifier
                .size(42.dp)
                .background(CardWhite, CircleShape)
                .border(1.dp, DividerGray, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.Notifications,
                contentDescription = "알림",
                tint = NavyMain
            )
        }
    }
}


@Composable
fun ProgressCard() {

    Box(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(NavyMain)
            .padding(24.dp)
    ) {

        Column {

            Text("전체 진행률", color = Color.White.copy(0.7f))

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                "35%",
                fontSize = 42.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { 0.35f },
                modifier = Modifier.fillMaxWidth(),
                color = PointOrange
            )
        }
    }
}


@Composable
fun SectionTitle(text: String) {

    Text(
        text,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        color = TextMain,
        modifier = Modifier.padding(horizontal = 20.dp)
    )
}


@Composable
fun OngoingMissionCard(
    mission: OngoingMission
) {

    Box(
        modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(CardWhite)
            .padding(20.dp)
    ) {

        Column {

            Text(mission.title, fontWeight = FontWeight.Bold, fontSize = 17.sp)

            Spacer(modifier = Modifier.height(8.dp))

            Text(mission.region, color = TextSub)

            Spacer(modifier = Modifier.height(16.dp))

            LinearProgressIndicator(
                progress = { mission.progress },
                modifier = Modifier.fillMaxWidth(),
                color = PointOrange
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text("${(mission.progress * 100).toInt()}% 진행중 · +${mission.reward}P")
        }
    }
}


/**
 * 홈 탭 - 지역 클릭 시 간략 정보를 펼치고,
 * '지도에서 자세히 보기'를 누르면 지도탭으로 이동해 해당 구를 확대한다.
 */
@Composable
fun ExpandableRegionCard(
    region: RegionProgress,
    expanded: Boolean,
    onToggle: () -> Unit,
    onDetail: () -> Unit
) {

    val progress = region.completed.toFloat() / region.total

    Box(
        modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(CardWhite)
            .clickable { onToggle() }
            .animateContentSize()
            .padding(20.dp)
    ) {

        Column {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    region.region,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )

                // 펼침 상태에 따라 화살표 회전
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.rotate(if (expanded) 90f else 0f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = PointOrange
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text("${region.completed}/${region.total} 완료")

            // 간략 정보 영역
            AnimatedVisibility(visible = expanded) {

                Column {

                    Spacer(modifier = Modifier.height(16.dp))

                    HorizontalDivider(color = DividerGray)

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        region.description,
                        color = TextSub,
                        fontSize = 14.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onDetail,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = NavyMain),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            Icons.Default.Map,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("지도에서 자세히 보기", color = Color.White)
                    }
                }
            }
        }
    }
}


@Composable
fun RegionProgressCard(
    region: RegionProgress,
    onClick: () -> Unit
) {

    val progress = region.completed.toFloat() / region.total

    Box(
        modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(CardWhite)
            .clickable { onClick() }
            .padding(20.dp)
    ) {

        Column {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Text(region.region, fontWeight = FontWeight.Bold, fontSize = 18.sp)

                Icon(Icons.Default.ChevronRight, contentDescription = null)
            }

            Spacer(modifier = Modifier.height(16.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = PointOrange
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text("${region.completed}/${region.total} 완료")
        }
    }
}


@Composable
fun RegionMissionSection(
    region: RegionProgress
) {

    Column(
        modifier = Modifier.padding(20.dp)
    ) {

        Text(region.region, fontSize = 20.sp, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(16.dp))

        repeat(3) {
            OngoingMissionCard(
                OngoingMission(
                    "지역 미션 ${it + 1}",
                    region.region,
                    0.3f + (it * 0.2f),
                    100
                )
            )
        }
    }
}


@Composable
fun RankingCard(
    ranking: RankingInfo
) {

    Box(
        modifier = Modifier
            .padding(20.dp)
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(NavyMain)
            .padding(24.dp)
    ) {

        Column {

            Text("현재 내 순위", color = Color.White.copy(0.7f))

            Spacer(modifier = Modifier.height(20.dp))

            RankingInfoRow("전체 랭킹", "#${ranking.global}")
            RankingInfoRow("지역 랭킹", "#${ranking.regional}")
            RankingInfoRow("친구 랭킹 (3인)", "#${ranking.friend}")
        }
    }
}


@Composable
fun RankingInfoRow(
    title: String,
    rank: String
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {

        Text(title, color = Color.White)

        Text(rank, color = PointOrange, fontWeight = FontWeight.Bold)
    }
}


/** 3선발 친구 랭킹 행 (본인은 강조 표시) */
@Composable
fun FriendRankRow(
    rank: Int,
    friend: FriendRank
) {

    Row(
        modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(if (friend.isMe) NavyLight else CardWhite)
            .padding(18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {

        Row(verticalAlignment = Alignment.CenterVertically) {

            Text(
                "#$rank",
                fontWeight = FontWeight.Bold,
                color = if (friend.isMe) Color.White else NavyMain
            )

            Spacer(modifier = Modifier.width(20.dp))

            Text(
                friend.name,
                color = if (friend.isMe) Color.White else TextMain,
                fontWeight = if (friend.isMe) FontWeight.Bold else FontWeight.Normal
            )
        }

        Text(
            "${friend.score}P",
            fontWeight = FontWeight.Bold,
            color = PointOrange
        )
    }
}


@Composable
fun RankingRow(
    rank: Int,
    name: String,
    score: String
) {

    Row(
        modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardWhite)
            .padding(18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {

        Row(verticalAlignment = Alignment.CenterVertically) {

            Text("#$rank", fontWeight = FontWeight.Bold, color = NavyMain)

            Spacer(modifier = Modifier.width(20.dp))

            Text(name)
        }

        Text(score, fontWeight = FontWeight.Bold, color = PointOrange)
    }
}