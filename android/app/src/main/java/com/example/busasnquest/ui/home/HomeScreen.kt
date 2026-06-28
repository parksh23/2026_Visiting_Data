package com.example.busasnquest.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.busasnquest.data.model.MissionState
import com.example.busasnquest.data.model.MissionType
import com.example.busasnquest.data.model.OngoingMission
import com.example.busasnquest.ui.components.ProgressCard
import com.example.busasnquest.ui.components.ScreenHeader
import com.example.busasnquest.ui.components.SectionTitle
import com.example.busasnquest.ui.components.rememberMissionVerifier
import com.example.busasnquest.ui.theme.CardWhite
import com.example.busasnquest.ui.theme.IconBlue
import com.example.busasnquest.ui.theme.IconGreen
import com.example.busasnquest.ui.theme.IconGreenBg
import com.example.busasnquest.ui.theme.NavyMain
import com.example.busasnquest.ui.theme.PointOrange
import com.example.busasnquest.ui.theme.PointRed
import com.example.busasnquest.ui.theme.TextMain
import com.example.busasnquest.ui.theme.TextSub
import com.example.busasnquest.data.repository.OccupationStat
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import com.example.busasnquest.R
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import com.example.busasnquest.ui.components.clickableNoRipple

@Composable
fun HomeScreen(
    navController: NavHostController,
    viewModel: HomeViewModel = viewModel()
) {
    val missions by viewModel.homeMissions.collectAsStateWithLifecycle()
    val occupation by viewModel.occupation.collectAsStateWithLifecycle()

    // 인증 헬퍼 (사진/위치/영수증 런처를 다 담고 있음)
    val verify = rememberMissionVerifier(viewModel)

    LazyColumn {

        item {
            ScreenHeader(
                title = "부산 ",
                highlight = "땅따먹기",
                subtitle = "숨은 보물을 찾고, 부산을 점령하자!"
            )

            ProgressCard(
                label = "나의 점령률",
                percentText = "${(occupation.rate * 100).toInt()}%",
                caption = "${occupation.completedMissions}/${occupation.totalMissions} 미션 완료",
                progress = occupation.rate
            )

            Spacer(modifier = Modifier.height(24.dp))

            SectionTitle("지도에서 지역을 선택하세요")
            Spacer(modifier = Modifier.height(12.dp))

            MapPlaceholder { districtName ->
                navController.navigate("map/$districtName")
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

            // 진행 중인 미션이 없을 때 안내
            if (missions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(CardWhite)
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "도전 중인 미션이 없어요.\n미션 탭에서 도전해보세요!",
                        color = TextSub,
                        fontSize = 14.sp
                    )
                }
            }

            // 진행 중인 미션들을 카드로
            missions.forEach { item ->
                val id = item.mission.id
                OngoingMissionCard(
                    mission = item.mission,
                    state = item.state,
                    error = item.error,
                    onClick = { navController.navigate("missionDetail/$id") },
                    onPickPhoto = { verify(id, item.mission.type) },
                    onUseCurrentLocation = { verify(id, item.mission.type) },
                    onCaptureReceipt = { verify(id, item.mission.type) }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))


            Spacer(modifier = Modifier.height(120.dp))
        }
    }
}

@Composable
fun MapPlaceholder(onDistrictClick: (String) -> Unit) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        // 약도 이미지
        Image(
            painter = painterResource(id = R.drawable.busan_map),
            contentDescription = "부산 지도",
            modifier = Modifier.fillMaxWidth(),
            contentScale = ContentScale.FillWidth
        )

        // 이미지 실제 표시 크기 (가로는 꽉 차고, 세로는 비율대로)
        val mapWidth = maxWidth
        // 약도 이미지의 가로:세로 비율 (이미지가 세로로 좀 더 김)
        val mapHeight = mapWidth * 1.21f

        // ── 16개 구·군 클릭 영역 ──
        DistrictHotspot("기장군", 0.80f, 0.22f, mapWidth, mapHeight, onDistrictClick)
        DistrictHotspot("금정구", 0.60f, 0.31f, mapWidth, mapHeight, onDistrictClick)
        DistrictHotspot("북구", 0.48f, 0.41f, mapWidth, mapHeight, onDistrictClick)
        DistrictHotspot("동래구", 0.61f, 0.44f, mapWidth, mapHeight, onDistrictClick)
        DistrictHotspot("해운대구", 0.77f, 0.45f, mapWidth, mapHeight, onDistrictClick)
        DistrictHotspot("사상구", 0.41f, 0.56f, mapWidth, mapHeight, onDistrictClick)
        DistrictHotspot("부산진구", 0.53f, 0.55f, mapWidth, mapHeight, onDistrictClick)
        DistrictHotspot("연제구", 0.65f, 0.55f, mapWidth, mapHeight, onDistrictClick)
        DistrictHotspot("강서구", 0.20f, 0.63f, mapWidth, mapHeight, onDistrictClick)
        DistrictHotspot("수영구", 0.70f, 0.62f, mapWidth, mapHeight, onDistrictClick)
        DistrictHotspot("서구", 0.47f, 0.67f, mapWidth, mapHeight, onDistrictClick)
        DistrictHotspot("동구", 0.53f, 0.67f, mapWidth, mapHeight, onDistrictClick)
        DistrictHotspot("남구", 0.64f, 0.69f, mapWidth, mapHeight, onDistrictClick)
        DistrictHotspot("사하구", 0.38f, 0.74f, mapWidth, mapHeight, onDistrictClick)
        DistrictHotspot("중구", 0.51f, 0.74f, mapWidth, mapHeight, onDistrictClick)
        DistrictHotspot("영도구", 0.57f, 0.80f, mapWidth, mapHeight, onDistrictClick)
    }
}

/** 지도 위 구·군 클릭 영역 (투명). 디버그용으로 살짝 보이게 해둠. */
@Composable
fun DistrictHotspot(
    name: String,
    xRatio: Float,
    yRatio: Float,
    mapWidth: androidx.compose.ui.unit.Dp,
    mapHeight: androidx.compose.ui.unit.Dp,
    onClick: (String) -> Unit
) {
    val hotspotSize = 36.dp
    Box(
        modifier = Modifier
            .offset(
                x = mapWidth * xRatio - hotspotSize / 2,
                y = mapHeight * yRatio - hotspotSize / 2
            )
            .size(hotspotSize)
            .clip(CircleShape)
            .background(Color.Transparent)
            .clickable { onClick(name) }
    )
}

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

@Composable
fun OngoingMissionCard(
    mission: OngoingMission,
    state: MissionState,
    error: String? = null,
    onClick: () -> Unit = {},
    onPickPhoto: () -> Unit = {},
    onUseCurrentLocation: () -> Unit = {},
    onCaptureReceipt: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CardWhite)
            .clickableNoRipple { onClick() }
    ) {

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
                Text(mission.district, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Column(modifier = Modifier.padding(18.dp)) {

            Text(mission.title, fontWeight = FontWeight.Bold, fontSize = 17.sp)

            Spacer(modifier = Modifier.height(6.dp))

            Text(mission.region, color = TextSub, fontSize = 13.sp)

            Spacer(modifier = Modifier.height(4.dp))

            Text(missionTypeLabel(mission.type), color = IconBlue, fontSize = 12.sp)

            Spacer(modifier = Modifier.height(8.dp))

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

            Spacer(modifier = Modifier.height(14.dp))

            when (state) {
                MissionState.IN_PROGRESS -> {
                    Button(
                        onClick = {
                            when (mission.type) {
                                MissionType.PHOTO_LOCATION   -> onPickPhoto()
                                MissionType.CURRENT_LOCATION -> onUseCurrentLocation()
                                MissionType.RECEIPT          -> onCaptureReceipt()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(verifyButtonLabel(mission.type))
                    }
                    if (error != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(error, color = PointRed, fontSize = 12.sp)
                    }
                }
                MissionState.VERIFYING -> {
                    Button(
                        onClick = {},
                        enabled = false,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("인증 확인 중...")
                    }
                }
                MissionState.COMPLETED -> {
                    Button(
                        onClick = {},
                        enabled = false,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = IconGreen)
                    ) {
                        Text("✓ 미션 완료! +${mission.reward}P")
                    }
                }
                MissionState.NOT_STARTED -> {
                    // 홈에는 NOT_STARTED가 안 오지만, when을 완성하기 위해 비워둠
                }
            }
        }
    }
}
