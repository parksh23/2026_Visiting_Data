package kr.co.busanquest.util

import android.content.Intent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kr.co.busanquest.data.local.NotificationKey

/**
 * "알림을 눌러서 앱이 열렸다" 는 사실을 화면까지 전달하는 통로.
 *
 * 알림 클릭은 두 갈래로 들어온다. 둘 다 결국 MainActivity 의 Intent 로 도착한다.
 *
 *  1) 앱이 화면에 떠 있을 때 — Notifier 가 직접 알림을 만들었으므로
 *     PendingIntent 에 EXTRA_ROUTE 를 넣어 둔다.
 *  2) 앱이 백그라운드/종료 상태일 때 — 알림을 만든 것은 시스템(FCM)이고
 *     Notifier 는 호출조차 되지 않는다. 이때 시스템은 런처 인텐트를 열면서
 *     푸시의 data 페이로드를 그대로 extras 에 실어 준다.
 *     그래서 서버가 보내는 data["type"] 을 두 번째 단서로 함께 본다.
 *
 * Activity 는 화면(NavHost)보다 먼저 만들어지므로 값을 바로 넘길 수 없다.
 * 여기에 잠깐 담아 두고, BusanQuestApp 이 NavHost 를 만든 뒤 꺼내 쓴다.
 * 로그인 전이면 소비하지 않고 남겨 둬서, 로그인 직후 해당 화면으로 이어진다.
 */
object NotificationRoute {

    /** 앱이 직접 만든 알림에 심는 키 */
    const val EXTRA_ROUTE = "bq_route"

    /** 서버 푸시 data 페이로드의 종류 키 (backend push_notifications.CHANNELS 와 동일) */
    private const val EXTRA_TYPE = "type"

    private val _pending = MutableStateFlow<String?>(null)
    val pending: StateFlow<String?> = _pending

    /** 서버 푸시 타입 → 이동할 화면 */
    fun routeForType(type: String?): String? = when (type) {
        "RANKING_CHANGE" -> "ranking"
        "NEW_MISSION" -> "mission"
        "MISSION_RESULT" -> "missionHistory"
        else -> null
    }

    /** 앱 내부 알림 종류 → 이동할 화면 */
    fun routeForKey(key: NotificationKey): String? = when (key) {
        NotificationKey.RANKING_CHANGE -> "ranking"
        NotificationKey.NEW_MISSION -> "mission"
        NotificationKey.MISSION_RESULT -> "missionHistory"
        else -> null            // NIGHT_MUTE / MARKETING 은 알림을 띄우지 않는다
    }

    /**
     * MainActivity 가 인텐트를 받을 때마다 호출한다(onCreate + onNewIntent).
     * 알림에서 온 인텐트가 아니면 아무것도 하지 않는다.
     */
    fun capture(intent: Intent?) {
        val extras = intent?.extras ?: return
        val route = extras.getString(EXTRA_ROUTE)
            ?: routeForType(extras.getString(EXTRA_TYPE))
            ?: return
        _pending.value = route
    }

    /** 화면 이동을 끝낸 뒤 호출. 안 부르면 화면을 다시 그릴 때마다 같은 곳으로 튄다. */
    fun consume() {
        _pending.value = null
    }
}
