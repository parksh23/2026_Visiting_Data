package kr.co.busanquest.data.repository

import android.content.Context
import kr.co.busanquest.data.local.NotificationKey
import kr.co.busanquest.data.local.SettingsStore
import kr.co.busanquest.data.remote.NotificationSettingsDto
import kr.co.busanquest.data.remote.NotificationSettingsUpdateDto
import kr.co.busanquest.data.remote.RetrofitInstance

/**
 * 알림 설정 저장소 — 서버(USER_SETTINGS)가 원본, 기기 DataStore 는 캐시.
 *
 * 서버를 원본으로 두는 이유는 **서버가 푸시를 보낼지 말지를 그 값으로 판단**하기 때문이다.
 * 기기에만 저장하면 스위치를 꺼도 서버는 계속 FCM 을 쏘게 된다.
 *
 * 기기 캐시를 그대로 두는 이유는 Notifier 가 알림을 띄우기 직전에
 * 스위치와 야간 방해 금지를 동기적으로 확인해야 하고, 그때 네트워크를 탈 수 없기 때문이다.
 */
object NotificationSettingsRepository {

    /**
     * 로그인 직후 서버 값을 받아 기기 캐시에 덮어쓴다.
     *
     * 서버가 원본이므로 "다른 기기에서 바꾼 설정"이 이 기기에도 그대로 반영된다.
     * 실패하면 캐시를 건드리지 않는다 — 마지막으로 알던 값으로 계속 동작한다.
     */
    suspend fun refresh(context: Context): Result<Unit> = runCatching {
        val remote = RetrofitInstance.api.getNotificationSettings()
        SettingsStore(context).setAllNotifications(remote.toMap())
    }

    /**
     * 스위치 하나를 바꾼다.
     *
     * 화면이 바로 반응하도록 기기 캐시를 먼저 쓰고(낙관적 갱신) 서버에 PATCH 한다.
     * 서버가 실패하면 캐시를 되돌린다 — 화면에는 켜져 있는데 서버는 꺼져 있는
     * 어긋난 상태가 남지 않도록.
     */
    suspend fun set(
        context: Context,
        key: NotificationKey,
        value: Boolean
    ): Result<Unit> {
        val store = SettingsStore(context)
        val previous = store.isEnabled(key)
        store.setNotification(key, value)

        return runCatching {
            val applied = RetrofitInstance.api.updateNotificationSettings(key.toUpdate(value))
            // 서버가 최종 상태를 돌려주므로 그대로 반영해 둔다
            store.setAllNotifications(applied.toMap())
        }.onFailure {
            store.setNotification(key, previous)
        }
    }

    private fun NotificationSettingsDto.toMap(): Map<NotificationKey, Boolean> = mapOf(
        NotificationKey.MISSION_RESULT to missionResult,
        NotificationKey.NEW_MISSION to newMission,
        NotificationKey.RANKING_CHANGE to rankingChange,
        NotificationKey.NIGHT_MUTE to nightMute,
        NotificationKey.MARKETING to marketing
    )

    /** 바꾼 항목 하나만 채운 PATCH 본문 (나머지는 null 이라 서버가 건드리지 않는다) */
    private fun NotificationKey.toUpdate(value: Boolean): NotificationSettingsUpdateDto =
        when (this) {
            NotificationKey.MISSION_RESULT -> NotificationSettingsUpdateDto(missionResult = value)
            NotificationKey.NEW_MISSION -> NotificationSettingsUpdateDto(newMission = value)
            NotificationKey.RANKING_CHANGE -> NotificationSettingsUpdateDto(rankingChange = value)
            NotificationKey.NIGHT_MUTE -> NotificationSettingsUpdateDto(nightMute = value)
            NotificationKey.MARKETING -> NotificationSettingsUpdateDto(marketing = value)
        }
}
