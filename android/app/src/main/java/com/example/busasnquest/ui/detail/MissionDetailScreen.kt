package com.example.busasnquest.ui.detail


import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
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
import androidx.compose.runtime.LaunchedEffect
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
    val savedMissions by MissionRepository.savedMissions.collectAsStateWithLifecycle()
    // 전체 목록에 없으면(찜 목록에서 바로 들어온 경우) 찜 목록에서 찾는다
    val item = missions.firstOrNull { it.mission.id == missionId }
        ?: savedMissions.firstOrNull { it.mission.id == missionId }

    // 찜 요청 중 / 실패 메시지
    val savePending by viewModel.savePending.collectAsStateWithLifecycle()
    val saveError by viewModel.saveError.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(saveError) {
        saveError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSaveError()
        }
    }

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
    val isSavePending = savePending.contains(mission.id)

    Box(modifier = Modifier.fillMaxSize()) {
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

            // ⚠️ 기존에는 IconButton 과 Icon 양쪽에 토글이 걸려 한 번 누르면 두 번 토글돼
            //    찜이 안 되는 것처럼 보였다 → 클릭 주체를 하나로 통일한다.
            val heartTint by animateColorAsState(
                targetValue = if (item.saved) Coral else TextSub,
                animationSpec = tween(Motion.DurRelease, easing = Motion.EaseOut),
                label = "detailHeart"
            )
            Box(
                modifier = Modifier
                    .size(44.dp)
                    // 요청 중에는 잠금 → 연속 클릭으로 중복 요청이 나가지 않는다
                    .pressable(enabled = !isSavePending, scaleDown = 0.86f) {
                        viewModel.toggleSaved(mission.id)
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (item.saved) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = if (item.saved) "찜 해제" else "찜하기",
                    tint = if (isSavePending) heartTint.copy(alpha = 0.4f) else heartTint,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // 미션 이미지 (전체미션 카드와 동일하게 mission.imageUrl 연동, 없으면 자리표시자)
        Box(
            modifier = Modifier
                .padding(horizontal = Dimens.screenPadding)
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(Dimens.radiusHero))
                .background(Brush.verticalGradient(listOf(DetailHeroTop, DetailHeroBottom))),
            contentAlignment = Alignment.Center
        ) {
            if (mission.imageUrl != null) {
                AsyncImage(
                    model = mission.imageUrl,
                    contentDescription = mission.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(Icons.Filled.Image, contentDescription = null, tint = SeaBlue.copy(0.7f), modifier = Modifier.size(56.dp))
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Column(modifier = Modifier.padding(horizontal = Dimens.screenPadding)) {
            // 상세 제목 — 디스플레이 헤딩
            Text(mission.title, style = displayStyle(24.sp), color = TextMain)
            Spacer(modifier = Modifier.height(8.dp))
            Text(mission.region, color = TextSub, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(missionTypeLabelDetail(mission.type), color = IconBlue, fontSize = 13.sp)

            Spacer(modifier = Modifier.height(Dimens.gapBlock))

            // 보상 — 앱 공통 포인트 표시 (별 아이콘 → P 뱃지로 통일)
            com.example.busasnquest.ui.components.PointAmount(
                value = mission.reward,
                prefix = "+",
                badgeSize = 20.dp,
                badgeFontSize = 11.sp,
                fontSize = 16.sp,
                gap = 6.dp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 인증 방법 안내
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Dimens.radiusButton))
                    .background(CardWhite)
                    .padding(Dimens.gapBlock)
            ) {
                Text(missionGuide(mission.type), color = TextSub, fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 인증 버튼 자리 (3단계에서 실제 기능 연결)
            when (item.state) {
                MissionState.NOT_STARTED -> {
                    Button(
                        onClick = { viewModel.startMission(mission.id) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("도전하기")
                    }
                }
                MissionState.IN_PROGRESS -> {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { viewModel.cancelMission(mission.id) },
                            modifier = Modifier.weight(0.38f)
                        ) {
                            Text("도전 취소")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { verify(mission.id, mission.type) },
                            modifier = Modifier.weight(0.62f)
                        ) {
                            Text(verifyButtonLabelDetail(mission.type))
                        }
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

        // 찜 실패 안내 (401/404/500 등)
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

// 상세 화면용 라벨/안내 (홈의 것과 이름 겹치지 않게 Detail 붙임)
fun missionTypeLabelDetail(type: MissionType): String = when (type) {
    MissionType.IMAGE_LOCATION   -> "📷 사진 위치 인증"
    MissionType.CURRENT_LOCATION -> "📍 현재 위치 인증"
    MissionType.RECEIPT          -> "🧾 결제 영수증 인증"
}

fun missionGuide(type: MissionType): String = when (type) {
    MissionType.IMAGE_LOCATION   -> "이 미션은 사진 내용과 제출 시점의 현재 위치로 인증합니다. 미션 장소에서 촬영하거나 사진을 선택해주세요."
    MissionType.CURRENT_LOCATION -> "이 미션은 현재 위치로 인증합니다. 미션 장소에 도착해서 '인증하기'를 눌러주세요."
    MissionType.RECEIPT          -> "이 미션은 결제 영수증으로 인증합니다. 해당 장소에서 결제 후 영수증을 촬영해주세요."
}

fun verifyButtonLabelDetail(type: MissionType): String = when (type) {
    MissionType.IMAGE_LOCATION   -> "📷 사진 올려서 인증하기"
    MissionType.CURRENT_LOCATION -> "📍 현재 위치로 인증하기"
    MissionType.RECEIPT          -> "🧾 영수증 올려서 인증하기"
}
