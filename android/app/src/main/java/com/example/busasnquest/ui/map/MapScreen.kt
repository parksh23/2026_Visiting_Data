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
                                // 지도 준비 완료 → 부산으로 카메라 이동
                                val busan = LatLng.from(35.1796, 129.0756)
                                kakaoMap.moveCamera(
                                    CameraUpdateFactory.newCenterPosition(busan, 10)
                                )
                            }
                        }
                    )
                }
            }
        )
    }
}