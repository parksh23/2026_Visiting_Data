package com.example.busasnquest.data.remote

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * 세션(로그인) 상태 전역 이벤트.
 *
 * 어떤 API 든 401 Unauthorized 가 내려오면 RetrofitInstance 의 인터셉터가
 * 저장된 JWT 를 지우고 이 이벤트를 발행한다.
 * BusanQuestApp(NavHost)이 이 이벤트를 구독하고 있다가 로그인 화면으로 보낸다.
 *
 * 화면마다 401 을 따로 처리하지 않기 위한 단일 통로.
 */
object SessionManager {

    // replay=0: 과거 이벤트를 다시 받지 않음
    // extraBufferCapacity=1: 구독자가 잠시 없어도 이벤트 1개는 버퍼에 보관
    private val _sessionExpired = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val sessionExpired: SharedFlow<Unit> = _sessionExpired

    // 인터셉터(OkHttp 스레드)에서 호출되므로 suspend 가 아닌 tryEmit 사용
    fun notifySessionExpired() {
        _sessionExpired.tryEmit(Unit)
    }
}
