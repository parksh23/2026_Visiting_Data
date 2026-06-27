package com.example.busasnquest.ui.mission

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.busasnquest.data.model.DistrictProgress
import com.example.busasnquest.data.model.MissionState
import com.example.busasnquest.data.model.completedDistrictCount
import com.example.busasnquest.data.model.occupationRate
import com.example.busasnquest.data.model.totalDistrictCount
import com.example.busasnquest.data.repository.MissionWithState
import com.example.busasnquest.ui.components.ProgressCard
import com.example.busasnquest.ui.components.ScreenHeader
import com.example.busasnquest.ui.components.SegmentedToggle
import com.example.busasnquest.ui.theme.*
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.clickable
import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.busasnquest.data.model.MissionType
import com.example.busasnquest.ui.home.HomeViewModel
import com.example.busasnquest.util.createImageUri

@Composable
fun MissionScreen(
    navController: NavHostController,
    viewModel: MissionViewModel = viewModel(),
    homeViewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // 어느 미션이 인증을 요청했는지 기억
    var activeId by remember { mutableStateOf(0) }
    var pendingReceiptUri by remember { mutableStateOf<Uri?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) homeViewModel.onPhotoPicked(activeId, context, uri)
    }
    val locationPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) homeViewModel.onLocationPermissionGranted(activeId, context)
        else homeViewModel.onLocationPermissionDenied(activeId)
    }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        homeViewModel.onReceiptCaptured(activeId, success)
    }
    val cameraPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val uri = createImageUri(context)
            pendingReceiptUri = uri
            cameraLauncher.launch(uri)
        } else homeViewModel.onCameraPermissionDenied(activeId)
    }

    // 인증 시작 동작을 한 곳에 모음 (타입별 분기)
    fun startVerify(id: Int, type: MissionType) {
        activeId = id
        when (type) {
            MissionType.PHOTO_LOCATION -> photoPicker.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
            MissionType.CURRENT_LOCATION -> {
                val granted = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
                if (granted) homeViewModel.onLocationPermissionGranted(id, context)
                else locationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            MissionType.RECEIPT -> {
                val granted = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
                if (granted) {
                    val uri = createImageUri(context)
                    pendingReceiptUri = uri
                    cameraLauncher.launch(uri)
                } else cameraPermission.launch(Manifest.permission.CAMERA)
            }
        }
    }

    LazyColumn {

        item {
            ScreenHeader(
                title = "미션",
                subtitle = "다양한 미션을 완료하고 포인트를 모아보세요!"
            )

            ProgressCard(
                label = "전체 진행률",
                percentText = "${(occupationRate * 100).toInt()}%",
                caption = "$completedDistrictCount/$totalDistrictCount 구·군 점령",
                progress = occupationRate
            )

            Spacer(modifier = Modifier.height(20.dp))

            SegmentedToggle(
                options = listOf("전체", "지역"),
                selectedIndex = uiState.selectedTab,
                onSelect = { viewModel.selectTab(it) }
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                if (uiState.selectedTab == 0) "전체 미션" else "지역별 미션",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextMain,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))
        }

        if (uiState.selectedTab == 0) {
            // ───── 전체 탭: 모든 미션 카드 ─────
            items(uiState.allMissions, key = { it.mission.id }) { item ->
                MissionCard(
                    item = item,
                    onChallenge = { viewModel.startMission(item.mission.id) },
                    onClick = { navController.navigate("missionDetail/${item.mission.id}") },
                    onVerify = { startVerify(item.mission.id, item.mission.type) }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        } else {
            // ───── 지역 탭: 구·군 목록 + 펼침 ─────
            items(uiState.districts, key = { it.name }) { district ->
                DistrictProgressRow(
                    district = district,
                    expanded = uiState.expandedDistrict == district.name,
                    onClick = { viewModel.toggleDistrict(district.name) }
                )
                // 펼쳐졌으면 그 지역 미션을 바로 아래에
                if (uiState.expandedDistrict == district.name) {
                    val missions = uiState.allMissions.filter { it.mission.district == district.name }
                    if (missions.isEmpty()) {
                        Text(
                            "이 지역엔 아직 미션이 없어요.",
                            color = TextSub,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                        )
                    } else {
                        missions.forEach { item ->
                            MissionCard(
                                item = item,
                                onChallenge = { viewModel.startMission(item.mission.id) },
                                onClick = { navController.navigate("missionDetail/${item.mission.id}") },
                                onVerify = { startVerify(item.mission.id, item.mission.type) }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(120.dp)) }
    }
}

/** 미션 카드 (미션 탭용): 제목 + 보상 + 상태별 버튼. */
@Composable
fun MissionCard(
    item: MissionWithState,
    onChallenge: () -> Unit,
    onClick: () -> Unit = {},
    onVerify: () -> Unit = {}
) {
    val mission = item.mission

    Column(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardWhite)
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Text(mission.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextMain)
        Spacer(modifier = Modifier.height(4.dp))
        Text(mission.region, color = TextSub, fontSize = 12.sp)

        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Star, contentDescription = null, tint = PointOrange, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("+${mission.reward}P", fontWeight = FontWeight.Bold, color = PointOrange, fontSize = 14.sp)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 상태별 버튼
        when (item.state) {
            MissionState.NOT_STARTED -> {
                Button(onClick = onChallenge, modifier = Modifier.fillMaxWidth()) {
                    Text("도전하기")
                }
            }
            MissionState.IN_PROGRESS -> {
                Button(
                    onClick = onVerify,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("인증하기")
                }
                if (item.error != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(item.error, color = PointRed, fontSize = 12.sp)
                }
            }
            MissionState.VERIFYING -> {
                Button(
                    onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()
                ) {
                    Text("인증 확인 중...")
                }
            }
            MissionState.COMPLETED -> {
                Button(
                    onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = IconGreen)
                ) {
                    Text("✓ 완료")
                }
            }
        }
    }
}

/** 구·군 한 줄: 이름 + 진행 바 + 퍼센트 + 개수 + 화살표(펼침 표시). */
@Composable
fun DistrictProgressRow(
    district: DistrictProgress,
    expanded: Boolean,
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

        // 펼침 상태에 따라 화살표 방향
        Icon(
            if (expanded) Icons.Default.KeyboardArrowDown else Icons.Default.ChevronRight,
            contentDescription = null,
            tint = TextSub,
            modifier = Modifier.size(20.dp)
        )
    }
}