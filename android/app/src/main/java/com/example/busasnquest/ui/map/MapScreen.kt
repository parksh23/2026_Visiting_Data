package com.example.busasnquest.ui.map

import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.busasnquest.ui.components.ScreenHeader
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.MapView
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.example.busasnquest.R
import com.example.busasnquest.data.repository.MissionRepository
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles

@Composable
fun MapScreen(region: String) {
    Column(modifier = Modifier.fillMaxSize()) {

        ScreenHeader(
            title = "지도",
            subtitle = if (region == "부산") "구·군을 선택해 상세 정보를 확인하세요"
            else "$region 상세 정보를 확인하세요"
        )

        // 카카오맵 (Compose ↔ View 다리: AndroidView)
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                MapView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    start(
                        object : MapLifeCycleCallback() {
                            override fun onMapDestroy() {}
                            override fun onMapError(error: Exception?) {
                                // 지도 에러 (키 문제 등) → 로그로 확인
                                android.util.Log.e("KakaoMap", "지도 에러: ${error?.message}")
                            }
                        },
                        object : KakaoMapReadyCallback() {
                            override fun onMapReady(kakaoMap: KakaoMap) {
                                // 넘어온 region의 좌표로 카메라 이동
                                val center = districtCenters[region] ?: districtCenters["부산"]!!
                                val zoom = if (region == "부산") 10 else 14
                                kakaoMap.moveCamera(
                                    CameraUpdateFactory.newCenterPosition(center, zoom)
                                )

                                // ── 미션 핀 꽂기 ──
                                val missions = MissionRepository.missions.value.filter {
                                    region == "부산" || it.mission.district == region
                                }


                                if (missions.isEmpty()) return

                                // 핀 스타일 (아이콘)
                                // 벡터 아이콘을 비트맵으로 변환해서 사용
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

                                val styles = kakaoMap.labelManager?.addLabelStyles(
                                    LabelStyles.from(
                                        LabelStyle.from(pinBitmap)
                                    )
                                )

                                // 라벨 레이어에 미션마다 핀 추가
                                val layer = kakaoMap.labelManager?.layer
                                missions.forEach { item ->
                                    val m = item.mission
                                    if (m.lat == 0.0 && m.lng == 0.0) return@forEach
                                    layer?.addLabel(
                                        LabelOptions.from(LatLng.from(m.lat, m.lng))
                                            .setStyles(styles)
                                            .setTag(m.id.toString())
                                    )
                                }
                            }
                        }
                    )
                }
            }
        )
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