package com.example.busasnquest.ui.components

import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.MapView

/**
 * 생명주기를 붙인 카카오 벡터맵.
 *
 * ⚠️ 왜 백그라운드에서 "그냥 pause" 하지 않는가 (앱 먹통 이슈)
 *
 * 카카오맵은 내부적으로 KGLSurfaceView(SurfaceView) 를 쓴다. 앱이 백그라운드로 가면
 * 시스템이 그 GL 서피스를 파괴하는데, 엔진을 pause 상태로만 두면 복귀 시 서피스를
 * 다시 붙이지 못한다. 그러면 ViewRootImpl 이 첫 프레임 draw 보고를 영원히 기다리고
 * (mDrawsNeededToReport 가 0 으로 안 내려감), 창이 표시되지 않아 터치도 전달되지 않는다.
 * → 화면은 직전 스냅샷으로 멈추고, 입력 자체가 앱에 안 오니 ANR 다이얼로그도 안 뜬다.
 *
 * 반면 화면을 벗어날 때처럼 paused → stopped → released → destroyed 로 완전히 정리한 뒤
 * 새로 만드는 경로는 정상 동작한다. 그래서 여기서는:
 *
 *   ON_STOP  → MapView 를 컴포지션에서 내리고 finish() 로 완전 정리
 *   ON_START → MapView 를 새로 만들어 다시 start()
 *
 * 복귀할 때 지도가 다시 로드되는 짧은 순간이 있지만, 앱이 멈추는 것보다 낫다.
 *
 * 지도를 쓰는 화면은 MapView 를 직접 만들지 말고 이 컴포저블을 쓸 것.
 */
@Composable
fun KakaoMapView(
    modifier: Modifier = Modifier,
    onMapError: (Exception?) -> Unit = {},
    onMapReady: (KakaoMap) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    // 화면이 보이는 동안(STARTED 이상)에만 지도를 붙인다
    var mounted by remember {
        mutableStateOf(
            lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        )
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mounted = true
                Lifecycle.Event.ON_STOP -> mounted = false
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (mounted) {
        MapViewHost(modifier = modifier, onMapError = onMapError, onMapReady = onMapReady)
    } else {
        // 지도를 내린 동안에도 자리는 유지 (복귀 시 레이아웃이 튀지 않게)
        Box(modifier)
    }
}

@Composable
private fun MapViewHost(
    modifier: Modifier,
    onMapError: (Exception?) -> Unit,
    onMapReady: (KakaoMap) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // start() 는 한 번만 부르므로, 콜백은 항상 최신 것을 바라보게 한다
    val currentOnMapReady by rememberUpdatedState(onMapReady)
    val currentOnMapError by rememberUpdatedState(onMapError)

    val mapView = remember {
        MapView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            // 정리 시점을 아래 onDispose 에서 직접 제어한다
            setFinishManually(true)
        }
    }

    DisposableEffect(lifecycleOwner, mapView) {
        mapView.start(
            object : MapLifeCycleCallback() {
                override fun onMapDestroy() {}
                override fun onMapError(error: Exception?) {
                    currentOnMapError(error)
                }
            },
            object : KakaoMapReadyCallback() {
                override fun onMapReady(map: KakaoMap) {
                    currentOnMapReady(map)
                }
            }
        )

        // 화면이 잠깐 가려지는 정도(멀티윈도우·다이얼로그)는 pause/resume 으로 처리
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.resume()
                Lifecycle.Event.ON_PAUSE -> mapView.pause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.finish()   // paused → stopped → released → destroyed 까지 완전 정리
        }
    }

    AndroidView(modifier = modifier, factory = { mapView })
}
