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
import com.example.busasnquest.data.model.MissionType
import com.example.busasnquest.data.model.MissionState
import com.example.busasnquest.data.model.occupationRate
import com.example.busasnquest.data.model.completedDistrictCount
import com.example.busasnquest.data.model.totalDistrictCount
import com.example.busasnquest.ui.components.ProgressCard
import com.example.busasnquest.ui.components.ScreenHeader
import com.example.busasnquest.ui.components.SectionTitle
import com.example.busasnquest.ui.theme.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.net.Uri
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.busasnquest.util.createImageUri

@Composable
fun HomeScreen(
    navController: NavHostController,
    viewModel: HomeViewModel = viewModel()
) {
    val missions by viewModel.homeMissions.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // 어느 미션이 액션을 요청했는지 기억 (사진/카메라 결과가 올 때 필요)
    var activeId by remember { mutableStateOf(0) }
    var pendingReceiptUri by remember { mutableStateOf<Uri?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.onPhotoPicked(activeId, context, uri)
        }
    }

    val locationPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.onLocationPermissionGranted(activeId, context)
        } else {
            viewModel.onLocationPermissionDenied(activeId)
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        viewModel.onReceiptCaptured(activeId, success)
    }

    val cameraPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val uri = createImageUri(context)
            pendingReceiptUri = uri
            cameraLauncher.launch(uri)
        } else {
            viewModel.onCameraPermissionDenied(activeId)
        }
    }

    LazyColumn {

        item {
            ScreenHeader(
                title = "부산 ",
                highlight = "땅따먹기",
                subtitle = "숨은 보물을 찾고, 부산을 점령하자!"
            )

            ProgressCard(
                label = "나의 점령률",
                percentText = "${(occupationRate * 100).toInt()}%",
                caption = "$completedDistrictCount/$totalDistrictCount 구·군 점령",
                progress = occupationRate
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
                    onPickPhoto = {
                        activeId = id
                        photoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onUseCurrentLocation = {
                        activeId = id
                        val granted = ContextCompat.checkSelfPermission(
                            context, Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                        if (granted) {
                            viewModel.onLocationPermissionGranted(id, context)
                        } else {
                            locationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        }
                    },
                    onCaptureReceipt = {
                        activeId = id
                        val granted = ContextCompat.checkSelfPermission(
                            context, Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                        if (granted) {
                            val uri = createImageUri(context)
                            pendingReceiptUri = uri
                            cameraLauncher.launch(uri)
                        } else {
                            cameraPermission.launch(Manifest.permission.CAMERA)
                        }
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))

            SectionTitle("최근 점령 지역")
            Spacer(modifier = Modifier.height(12.dp))

            RecentCapturedCard("해운대구", "점령일 2024.05.16")

            Spacer(modifier = Modifier.height(120.dp))
        }
    }
}

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