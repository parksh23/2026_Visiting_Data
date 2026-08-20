package kr.co.busanquest.util

import com.kakao.sdk.user.UserApiClient
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * 카카오 SDK 세션 정리.
 *
 * 우리 서버 JWT 를 지우는 것만으로는 카카오 쪽 세션이 남는다.
 * 그 상태로 카카오 버튼을 다시 누르면 계정 선택 없이 곧바로 이전 계정으로 다시 들어가서,
 * 사용자는 "로그아웃이 안 됐다"고 느낀다. 그래서 로그아웃 시 카카오 토큰도 함께 만료시킨다.
 *
 * 탈퇴는 한 단계 더 나아가 연결 끊기(unlink)를 한다.
 * 우리 앱에 준 동의(카카오 계정 연결)까지 회수해야 "탈퇴했다"는 말과 실제 상태가 맞다.
 *
 * 두 함수 모두 실패를 삼킨다. 이메일로 가입한 사용자는 애초에 카카오 세션이 없어 에러가 나고,
 * 네트워크가 끊겨도 로컬 로그아웃 자체는 진행되어야 하기 때문이다.
 */
object KakaoSession {

    /** 카카오 액세스 토큰 만료 — 로그아웃 시 호출. */
    suspend fun logout(): Unit = suspendCancellableCoroutine { continuation ->
        runCatching {
            UserApiClient.instance.logout { _ ->
                if (continuation.isActive) continuation.resume(Unit)
            }
        }.onFailure {
            if (continuation.isActive) continuation.resume(Unit)
        }
    }

    /** 카카오 계정 연결 끊기 — 회원 탈퇴 시 호출. */
    suspend fun unlink(): Unit = suspendCancellableCoroutine { continuation ->
        runCatching {
            UserApiClient.instance.unlink { _ ->
                if (continuation.isActive) continuation.resume(Unit)
            }
        }.onFailure {
            if (continuation.isActive) continuation.resume(Unit)
        }
    }
}
