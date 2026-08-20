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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.example.busasnquest.R
import com.example.busasnquest.data.model.MissionState
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
import androidx.compose.ui.platform.LocalContext
import com.example.busasnquest.ui.components.KakaoMapView
import com.example.busasnquest.ui.theme.bottomBarSpacing

/**
 * 지도 깃발의 기준점 — 이미지에서 "깃대 밑동"이 있는 위치 비율.
 * (0,0)=좌상단, (1,1)=우하단. 깃대가 이미지 왼쪽에 치우쳐 있어 0.5 가 아니다.
 *
 * ⚠️ 이 값을 지정하지 않으면 SDK 기본 기준점이 적용돼, 깃발이 실제 미션 좌표에서
 *    화면상 일정 픽셀만큼 어긋난 채 그려진다. 화면 픽셀 오차는 확대할수록
 *    지도상 거리로는 작아지므로 "줌할 때마다 가리키는 곳이 달라지는" 것처럼 보인다.
 */
private const val FLAG_ANCHOR_X = 0.108f
private const val FLAG_ANCHOR_Y = 1.0f

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
    // 깃발 비트맵을 만들 때 사용 (예전에는 AndroidView factory 의 context 였다)
    val context = LocalContext.current

    LaunchedEffect(focusSearch) {
        if (focusSearch) {
            searchFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    // region 내 미션 중 제목/구군명이 검색어와 일치하는 목록
    val missionsList by MissionRepository.missions.collectAsStateWithLifecycle()

    /**
     * ── 미션 깃발 꽂기 ──
     * 진행중(하늘색) · 찜한(코럴) 미션만 표시하고, 둘 다면 진행중을 우선한다.
     *
     * ⚠️ onMapReady 안에서 한 번만 그리면 안 된다.
     *    지도가 준비되는 시점엔 서버 미션이 아직 안 왔을 수 있고(그땐 좌표 없는 샘플 데이터다),
     *    이후 미션을 불러오거나 찜을 바꿔도 깃발이 갱신되지 않는다.
     *    → 지도·미션 목록이 바뀔 때마다 다시 그린다.
     */
    LaunchedEffect(kakaoMap, missionsList, region) {
        val map = kakaoMap ?: return@LaunchedEffect
        val layer = map.labelManager?.layer ?: return@LaunchedEffect

        layer.removeAll()   // 이전 깃발 제거 후 다시 그린다

        val inRegion = missionsList.filter {
            region == "부산" || it.mission.district == region
        }
        fun isOngoing(item: MissionWithState) =
            item.state == MissionState.IN_PROGRESS ||
                    item.state == MissionState.VERIFYING

        val targets = inRegion.filter { isOngoing(it) || it.saved }
        val withCoordinate = targets.filter { it.mission.lat != 0.0 || it.mission.lng != 0.0 }

        // 깃발이 안 보일 때 원인을 바로 알 수 있게 남긴다
        if (com.example.busasnquest.BuildConfig.DEBUG) {
            android.util.Log.d(
                "KakaoMap",
                "깃발 갱신 region=$region 지역미션=${inRegion.size} " +
                    "대상(진행중·찜)=${targets.size} 좌표있음=${withCoordinate.size}"
            )
        }

        if (withCoordinate.isEmpty()) return@LaunchedEffect

        fun flagBitmap(resId: Int): android.graphics.Bitmap? =
            androidx.core.content.ContextCompat
                .getDrawable(context, resId)
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

        val ongoingStyles = map.labelManager?.addLabelStyles(
            LabelStyles.from(
                LabelStyle.from(flagBitmap(R.drawable.ic_map_flag_progress))
                    .setAnchorPoint(FLAG_ANCHOR_X, FLAG_ANCHOR_Y)
            )
        )
        val savedStyles = map.labelManager?.addLabelStyles(
            LabelStyles.from(
                LabelStyle.from(flagBitmap(R.drawable.ic_map_flag_saved))
                    .setAnchorPoint(FLAG_ANCHOR_X, FLAG_ANCHOR_Y)
            )
        )

        withCoordinate.forEach { item ->
            val m = item.mission
            layer.addLabel(
                LabelOptions.from(LatLng.from(m.lat, m.lng))
                    .setStyles(if (isOngoing(item)) ongoingStyles else savedStyles)
                    .setTag(m.id.toString())
            )
        }

        map.setOnLabelClickListener { _, _, label ->
            val id = label.tag?.toString()?.toIntOrNull()
            selectedMission = withCoordinate.firstOrNull { it.mission.id == id }?.mission
            true
        }
    }

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
            // (MapView 를 직접 만들면 백그라운드 복귀 시 첫 프레임을 못 그려 앱이 멈춘다)
            KakaoMapView(
                modifier = Modifier.fillMaxSize(),
                onMapError = { error ->
                    if (com.example.busasnquest.BuildConfig.DEBUG) {
                        android.util.Log.e("KakaoMap", "지도 에러: ${error?.message}")
                    }
                },
                onMapReady = { map ->
                    kakaoMap = map

                    val center = districtCenters[region] ?: districtCenters["부산"]!!
                    val zoom = if (region == "부산") 10 else 14
                    map.moveCamera(
                        CameraUpdateFactory.newCenterPosition(center, zoom)
                    )
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
                        .padding(bottom = bottomBarSpacing(), top = 16.dp)
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
