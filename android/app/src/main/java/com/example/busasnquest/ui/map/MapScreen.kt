package com.example.busasnquest.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalContext
import com.example.busasnquest.ui.components.KakaoMapView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.example.busasnquest.R
import com.example.busasnquest.data.repository.MissionRepository
import com.example.busasnquest.data.repository.MissionWithState
import com.example.busasnquest.ui.theme.CardWhite
import com.example.busasnquest.ui.theme.Coral
import com.example.busasnquest.ui.theme.Dimens
import com.example.busasnquest.ui.theme.InkBorder
import com.example.busasnquest.ui.theme.InkBorderStrong
import com.example.busasnquest.ui.theme.TextMain
import com.example.busasnquest.ui.theme.TextSub
import com.example.busasnquest.ui.theme.pressable
import com.example.busasnquest.ui.theme.pressableRow
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles

@Composable
fun MapScreen(
    region: String,
    navController: androidx.navigation.NavHostController,
    focusSearch: Boolean = false
) {
    // 검색 결과 선택 시 카메라 이동에 쓰기 위해 지도 인스턴스를 보관
    var kakaoMap by remember { mutableStateOf<KakaoMap?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedMission by remember { mutableStateOf<com.example.busasnquest.data.model.OngoingMission?>(null) }

    val searchFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    // 미션 핀 비트맵을 만들 때 사용 (예전에는 AndroidView factory 의 context 였다)
    val context = LocalContext.current

    LaunchedEffect(focusSearch) {
        if (focusSearch) {
            searchFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    // region 내 미션 중 제목/구군명이 검색어와 일치하는 목록
    val missionsList by MissionRepository.missions.collectAsStateWithLifecycle()

    val searchResults: List<MissionWithState> =
        if (searchQuery.isBlank()) emptyList()
        else missionsList
            .filter { region == "부산" || it.mission.district == region }
            .filter {
                it.mission.title.contains(searchQuery, ignoreCase = true) ||
                        it.mission.district.contains(searchQuery, ignoreCase = true)
            }

    Column(modifier = Modifier.fillMaxSize()) {

        // 지도 + 검색창을 겹쳐서 보여주는 영역
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            // 카카오맵 — 생명주기가 붙은 공용 컴포저블
            // (MapView 를 직접 만들면 복귀 시 먹통/렌더러 누수가 생긴다)
            KakaoMapView(
                modifier = Modifier.fillMaxSize(),
                onMapError = { error ->
                    android.util.Log.e("KakaoMap", "지도 에러: ${error?.message}")
                },
                onMapReady = { map ->
                    kakaoMap = map

                    val center = districtCenters[region] ?: districtCenters["부산"]!!
                    val zoom = if (region == "부산") 10 else 14
                    map.moveCamera(
                        CameraUpdateFactory.newCenterPosition(center, zoom)
                    )

                    // ── 미션 핀 꽂기 ──
                    val missions = MissionRepository.missions.value.filter {
                        region == "부산" || it.mission.district == region
                    }

                    // 람다 안이라 그냥 return 은 쓸 수 없다 → 라벨 붙인 return
                    if (missions.isNotEmpty()) {
                        val pinBitmap = androidx.core.content.ContextCompat
                            .getDrawable(context, R.drawable.ic_mission_pin)
                            ?.let { drawable ->
                                val bmp = android.graphics.Bitmap.createBitmap(
                                    drawable.intrinsicWidth.coerceAtLeast(1),
                                    drawable.intrinsicHeight.coerceAtLeast(1),
                                    android.graphics.Bitmap.Config.ARGB_8888
                                )
                                val canvas = android.graphics.Canvas(bmp)
                                drawable.setBounds(0, 0, canvas.width, canvas.height)
                                drawable.draw(canvas)
                                bmp
                            }

                        val styles = map.labelManager?.addLabelStyles(
                            LabelStyles.from(
                                LabelStyle.from(pinBitmap)
                            )
                        )

                        val layer = map.labelManager?.layer
                        missions.forEach { item ->
                            val m = item.mission
                            if (m.lat == 0.0 && m.lng == 0.0) return@forEach
                            layer?.addLabel(
                                LabelOptions.from(LatLng.from(m.lat, m.lng))
                                    .setStyles(styles)
                                    .setTag(m.id.toString())
                            )
                        }
                        map.setOnLabelClickListener { _, _, label ->
                            val id = label.tag?.toString()?.toIntOrNull()
                            selectedMission = missions.firstOrNull { it.mission.id == id }?.mission
                            true
                        }
                    }
                }
            )

            // ── 검색창 (지도 위 오버레이) ──
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // 홈 탭 검색바(SearchPill)와 동일한 알약 스타일 (다크 표면 + 테두리 없음)
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("미션 장소 검색", fontSize = 14.sp) },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "지우기")
                            }
                        }
                    },
                    shape = RoundedCornerShape(Dimens.radiusPill),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CardWhite,
                        unfocusedContainerColor = CardWhite,
                        focusedBorderColor = InkBorderStrong,
                        unfocusedBorderColor = InkBorder,
                        focusedTextColor = TextMain,
                        unfocusedTextColor = TextMain,
                        focusedPlaceholderColor = TextSub,
                        unfocusedPlaceholderColor = TextSub,
                        focusedLeadingIconColor = TextSub,
                        unfocusedLeadingIconColor = TextSub,
                        focusedTrailingIconColor = TextSub,
                        unfocusedTrailingIconColor = TextSub,
                        cursorColor = Coral
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(searchFocusRequester)
                )

                // ── 검색 결과 드롭다운 ──
                if (searchResults.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                            .heightIn(max = 240.dp),
                        colors = CardDefaults.cardColors(containerColor = CardWhite)
                    ) {
                        LazyColumn {
                            items(searchResults) { item ->
                                val m = item.mission
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        // 카드 안 리스트 행 → scale 대신 배경 하이라이트
                                        .pressableRow {
                                            kakaoMap?.moveCamera(
                                                CameraUpdateFactory.newCenterPosition(
                                                    LatLng.from(m.lat, m.lng),
                                                    16
                                                )
                                            )
                                            searchQuery = ""
                                        }
                                        .padding(horizontal = 16.dp, vertical = 12.dp)
                                ) {
                                    Column {
                                        Text(m.title, fontWeight = FontWeight.Medium)
                                        Text(m.district, color = TextSub, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── 선택한 미션 미리보기 카드 (핀 클릭 시 하단에 표시) ──
            selectedMission?.let { mission ->
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .navigationBarsPadding()
                        .padding(bottom = 100.dp, top = 16.dp)
                        .pressable { navController.navigate("missionDetail/${mission.id}") },
                    colors = CardDefaults.cardColors(containerColor = CardWhite)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(mission.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextMain)
                            Spacer(Modifier.height(4.dp))
                            Text(mission.district, color = TextSub, fontSize = 13.sp)
                            Spacer(Modifier.height(4.dp))
                            // 보상 — 앱 공통 포인트 표시
                            com.example.busasnquest.ui.components.PointAmount(
                                value = mission.reward,
                                badgeSize = 15.dp,
                                badgeFontSize = 8.sp,
                                fontSize = 13.sp,
                                gap = 4.dp
                            )
                        }
                        IconButton(onClick = { selectedMission = null }) {
                            Icon(Icons.Default.Close, contentDescription = "닫기")
                        }
                    }
                }
            }
        }
    }
}

// 구·군별 지도 중심 좌표
val districtCenters = mapOf(
    "해운대구" to LatLng.from(35.1631, 129.1635),
    "수영구" to LatLng.from(35.1455, 129.1131),
    "중구" to LatLng.from(35.1041, 129.0323),
    "부산진구" to LatLng.from(35.1631, 129.0533),
    "동래구" to LatLng.from(35.1969, 129.0839),
    "남구" to LatLng.from(35.1366, 129.0844),
    "북구" to LatLng.from(35.1975, 128.9903),
    "사하구" to LatLng.from(35.1045, 128.9749),
    "금정구" to LatLng.from(35.2429, 129.0921),
    "강서구" to LatLng.from(35.2122, 128.9808),
    "연제구" to LatLng.from(35.1763, 129.0797),
    "사상구" to LatLng.from(35.1525, 128.9910),
    "동구" to LatLng.from(35.1295, 129.0453),
    "서구" to LatLng.from(35.0979, 129.0243),
    "영도구" to LatLng.from(35.0911, 129.0679),
    "기장군" to LatLng.from(35.2445, 129.2223),
    "부산" to LatLng.from(35.1796, 129.0756)  // 전체
)