package kr.co.busanquest.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 알림을 눌렀을 때 열어야 할 화면을 앱 UI 로 전달하는 곳.
 *
 * 알림 탭은 Activity 의 Intent 로 들어오는데, 실제 화면 이동은 Compose 의
 * NavController 가 한다. 둘은 시점이 어긋난다 — 앱이 꺼져 있다가 알림으로 켜지면
 * Intent 가 먼저 오고 NavController 는 그 뒤에 만들어진다.
 * 그래서 목적지를 여기에 잠깐 담아 두고, 화면이 준비되면 꺼내 쓴다.
 *
 * 서버는 이미 data 에 type 을 실어 보내고 있으므로 백엔드는 건드릴 것이 없다.
 */
object PushNavigation {

    /** Notifier 가 PendingIntent 에 심는 키. FCM data 의 "type" 과는 별개다. */
    const val EXTRA_ROUTE = "bq_route"

    private val _pendingRoute = MutableStateFlow<String?>(null)
    val pendingRoute: StateFlow<String?> = _pendingRoute.asStateFlow()

    /**
     * 서버가 보내는 type 을 앱 라우트로 바꾼다.
     * (backend push_notifications.CHANNELS 의 키와 같은 값)
     */
    fun routeForType(type: String?): String? = when (type) {
        "RANKING_CHANGE" -> ROUTE_RANKING
        "NEW_MISSION" -> ROUTE_MISSION
        else -> null                       // 모르는 종류면 그냥 앱만 연다
    }

    /** 알림에서 목적지를 받았을 때 호출. */
    fun request(route: String?) {
        if (!route.isNullOrBlank()) _pendingRoute.value = route
    }

    /** 이동을 끝낸 뒤 호출. 비우지 않으면 화면이 다시 그려질 때마다 또 이동한다. */
    fun consume() {
        _pendingRoute.value = null
    }

    const val ROUTE_RANKING = "ranking"
    const val ROUTE_MISSION = "mission"
}
