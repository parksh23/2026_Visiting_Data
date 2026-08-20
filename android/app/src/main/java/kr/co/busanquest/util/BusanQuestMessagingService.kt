package kr.co.busanquest.util

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kr.co.busanquest.data.remote.RetrofitInstance

/**
 * 서버(FCM)에서 오는 푸시를 받는 곳.
 *
 * 서버는 notification 블록과 data 를 함께 보낸다. 그래서 동작이 두 갈래다.
 *   · 앱이 백그라운드/종료 상태 → 시스템이 알림을 자동으로 띄운다. 여기는 호출되지 않는다.
 *   · 앱이 화면에 떠 있는 상태  → onMessageReceived 만 호출되고 알림은 자동으로 안 뜬다.
 *                                 그래서 이 클래스가 직접 Notifier 로 띄운다.
 *
 * 이 서비스는 앱이 완전히 꺼진 상태에서도 깨어날 수 있다.
 * 그때는 MainActivity 의 init 들이 실행된 적이 없으므로 여기서 다시 초기화한다.
 */
class BusanQuestMessagingService : FirebaseMessagingService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * 토큰은 앱 재설치·데이터 삭제·주기적 갱신으로 바뀐다.
     * 바뀐 토큰을 서버에 다시 올리지 않으면 그 기기로는 푸시가 끊긴다.
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        prepare()
        // 로그인 상태가 아니면 PushRegistrar 가 알아서 아무것도 하지 않는다.
        // (로그인할 때 BusanQuestApp 이 다시 등록한다)
        scope.launch { PushRegistrar.register() }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        prepare()

        // 알림 문구는 notification 블록이 원본이고, 없으면 data 쪽을 쓴다
        val title = message.notification?.title ?: message.data["title"]
        val body = message.notification?.body ?: message.data["body"]

        // type = NEW_MISSION | RANKING_CHANGE (서버 push_notifications.CHANNELS 와 맞춘다)
        Notifier.showRemote(
            type = message.data["type"],
            title = title,
            body = body
        )
    }

    private fun prepare() {
        RetrofitInstance.init(applicationContext)
        Notifier.init(applicationContext)
        PushRegistrar.init(applicationContext)
    }
}
