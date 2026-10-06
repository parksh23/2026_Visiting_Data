package kr.co.busanquest.util

import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kr.co.busanquest.data.local.TokenStore
import kr.co.busanquest.data.remote.PushTokenDeleteRequestDto
import kr.co.busanquest.data.remote.PushTokenRequestDto
import kr.co.busanquest.data.remote.RetrofitInstance
import kotlin.coroutines.resume

/**
 * FCM 토큰을 서버에 등록/해제한다.
 *
 * 서버는 PUSH_TOKENS 에 쌓인 토큰으로만 푸시를 보낼 수 있으므로,
 * 로그인할 때마다 등록하고 로그아웃할 때 지운다.
 *
 * ⚠️ google-services.json 이 아직 없으면 FirebaseApp 초기화가 실패한다.
 *    그때는 예외를 삼키고 조용히 넘어간다 — 푸시만 안 올 뿐 앱은 정상 동작해야 한다.
 *
 * ⚠️ 로그인 상태가 아닐 때는 호출하지 않는다.
 *    토큰 없이 부르면 서버가 401 을 주고, 인터셉터가 "세션 만료"로 오해해
 *    로그인 화면으로 튕겨 버린다.
 */
object PushRegistrar {

    private var appContext: Context? = null

    /**
     * MainActivity 에서 한 번 호출한다.
     * 앱이 꺼진 상태에서 FCM 서비스만 깨어나는 경우도 있어, 서비스 쪽에서도 다시 부른다.
     */
    fun init(context: Context) {
        appContext = context.applicationContext
    }

    /** 로그인 직후 호출. 실패해도 앱 흐름을 막지 않는다. */
    suspend fun register(): Result<Unit> = runCatching {
        val context = appContext ?: return@runCatching
        if (!isLoggedIn(context)) return@runCatching
        val token = currentToken() ?: return@runCatching
        RetrofitInstance.api.registerPushToken(PushTokenRequestDto(token = token))
        Unit
    }

    /**
     * 로그아웃 직전 호출.
     *
     * ⚠️ 반드시 로컬 JWT 를 지우기 **전에** 불러야 한다. 토큰이 없으면 인증이 안 되고,
     *    서버에는 이 기기 토큰이 남아 로그아웃한 기기로 계속 푸시가 간다.
     *
     * 회원 탈퇴는 서버가 PUSH_TOKENS 를 함께 지우므로 따로 부르지 않는다.
     */
    suspend fun unregister(): Result<Unit> = runCatching {
        val context = appContext ?: return@runCatching
        if (!isLoggedIn(context)) return@runCatching
        val token = currentToken() ?: return@runCatching
        RetrofitInstance.api.unregisterPushToken(PushTokenDeleteRequestDto(token = token))
        Unit
    }

    private suspend fun isLoggedIn(context: Context): Boolean =
        runCatching { !TokenStore(context).tokenFlow.first().isNullOrBlank() }.getOrDefault(false)

    /**
     * 현재 기기의 FCM 등록 토큰.
     * google-services.json 이 없거나 Play 서비스가 없으면 null.
     */
    private suspend fun currentToken(): String? = suspendCancellableCoroutine { continuation ->
        runCatching {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (!continuation.isActive) return@addOnCompleteListener
                val token = if (task.isSuccessful) task.result else null

                // 디버그 빌드에서만 토큰을 찍는다.
                // Firebase 콘솔 > Messaging > "테스트 메시지 전송" 에 붙여넣어
                // 서버 없이도 푸시 수신을 확인할 수 있다.
                // Logcat 필터: FCM_TOKEN
                if (isDebugBuild()) {
                    if (token != null) {
                        Log.d("FCM_TOKEN", token)
                    } else {
                        Log.w("FCM_TOKEN", "토큰 발급 실패", task.exception)
                    }
                }

                continuation.resume(token)
            }
        }.onFailure {
            if (isDebugBuild()) Log.w("FCM_TOKEN", "FirebaseApp 초기화 실패 — 푸시 비활성", it)
            if (continuation.isActive) continuation.resume(null)
        }
    }

    /** google-services.json 유무와 무관하게, 디버그 빌드인지만 본다. */
    private fun isDebugBuild(): Boolean {
        val flags = appContext?.applicationInfo?.flags ?: return false
        return (flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }
}
