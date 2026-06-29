package com.example.busasnquest.ui.detail


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.busasnquest.data.repository.MissionRepository
import com.example.busasnquest.ui.theme.*
import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.busasnquest.ui.home.HomeViewModel
import com.example.busasnquest.util.createImageUri
import com.example.busasnquest.ui.components.rememberMissionVerifier
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder

@Composable
fun MissionDetailScreen(
    navController: NavHostController,
    missionId: Int,
    viewModel: HomeViewModel = viewModel()
) {
    // Repository에서 이 미션을 실시간으로 가져옴
    val missions by MissionRepository.missions.collectAsStateWithLifecycle()
    val item = missions.firstOrNull { it.mission.id == missionId }

    // 인증 헬퍼 (사진/위치/영수증 런처를 다 담고 있음)
    val verify = rememberMissionVerifier(viewModel)
    // 미션을 못 찾으면 (이론상 거의 없음) 빈 화면
    if (item == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("미션을 찾을 수 없어요.", color = TextSub)
        }
        return
    }

    val mission = item.mission

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgSoftBlue)
    ) {
        // 상단 바: 뒤로가기 + 찜하기
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로", tint = NavyMain)
            }
            Text("미션 상세", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = NavyMain)

            Spacer(modifier = Modifier.weight(1f))   // 가운데 공간 밀어내기

            IconButton(onClick = { MissionRepository.toggleSaved(mission.id) }) {
                Icon(
                    imageVector = if (item.saved) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "찜하기",
                    tint = if (item.saved) PointRed else TextSub,
                    modifier = Modifier
                        .size(28.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { MissionRepository.toggleSaved(mission.id) }
                )
            }
        }

        // 미션 이미지 자리표시자
        Box(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFFCFE0F2), Color(0xFFB7D0EA)))),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Image, contentDescription = null, tint = Color.White.copy(0.8f), modifier = Modifier.size(56.dp))
        }

        Spacer(modifier = Modifier.height(20.dp))

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Text(mission.title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextMain)
            Spacer(modifier = Modifier.height(8.dp))
            Text(mission.region, color = TextSub, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(missionTypeLabelDetail(mission.type), color = IconBlue, fontSize = 13.sp)

            Spacer(modifier = Modifier.height(16.dp))

            // 보상
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Star, contentDescription = null, tint = PointOrange, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("+${mission.reward}P", fontWeight = FontWeight.Bold, color = PointOrange, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 인증 방법 안내
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CardWhite)
                    .padding(16.dp)
            ) {
                Text(missionGuide(mission.type), color = TextSub, fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 인증 버튼 자리 (3단계에서 실제 기능 연결)
            when (item.state) {
                MissionState.NOT_STARTED -> {
                    Button(
                        onClick = { MissionRepository.startMission(mission.id) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("도전하기")
                    }
                }
                MissionState.IN_PROGRESS -> {
                    Button(
                        onClick = { verify(mission.id, mission.type) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(verifyButtonLabelDetail(mission.type))
                    }
                    if (item.error != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(item.error, color = PointRed, fontSize = 12.sp)
                    }
                }
                MissionState.VERIFYING -> {
                    Button(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
                        Text("인증 확인 중...")
                    }
                }
                MissionState.COMPLETED -> {
                    Button(
                        onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = IconGreen)
                    ) {
                        Text("✓ 완료한 미션")
                    }
                }
            }
        }
    }
}

// 상세 화면용 라벨/안내 (홈의 것과 이름 겹치지 않게 Detail 붙임)
fun missionTypeLabelDetail(type: MissionType): String = when (type) {
    MissionType.PHOTO_LOCATION   -> "📷 사진 위치 인증"
    MissionType.CURRENT_LOCATION -> "📍 현재 위치 인증"
    MissionType.RECEIPT          -> "🧾 결제 영수증 인증"
}

fun missionGuide(type: MissionType): String = when (type) {
    MissionType.PHOTO_LOCATION   -> "이 미션은 사진의 위치정보로 인증합니다. 미션 장소에서 위치 기록을 켜고 촬영한 사진을 올려주세요."
    MissionType.CURRENT_LOCATION -> "이 미션은 현재 위치로 인증합니다. 미션 장소에 도착해서 '인증하기'를 눌러주세요."
    MissionType.RECEIPT          -> "이 미션은 결제 영수증으로 인증합니다. 해당 장소에서 결제 후 영수증을 촬영해주세요."
}

fun verifyButtonLabelDetail(type: MissionType): String = when (type) {
    MissionType.PHOTO_LOCATION   -> "📷 사진 올려서 인증하기"
    MissionType.CURRENT_LOCATION -> "📍 현재 위치로 인증하기"
    MissionType.RECEIPT          -> "🧾 영수증 올려서 인증하기"
}