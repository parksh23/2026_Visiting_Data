package kr.co.busanquest.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.Calendar

// 알림 설정 전용 DataStore (파일명: "settings") — 토큰용 "auth" 와 분리
private val Context.settingsDataStore by preferencesDataStore(name = "settings")

/** 알림 항목. key 는 DataStore 저장 키, default 는 최초 기본값. */
enum class NotificationKey(val storeKey: String, val default: Boolean) {
    MISSION_RESULT("noti_mission_result", true),
    NEW_MISSION("noti_new_mission", true),
    RANKING_CHANGE("noti_ranking_change", false),
    NIGHT_MUTE("noti_night_mute", true),
    MARKETING("noti_marketing", false)
}

/**
 * 알림 설정의 기기 캐시 (DataStore).
 *
 * 원본은 서버 USER_SETTINGS 이고 읽기/쓰기는 NotificationSettingsRepository 를 거친다.
 * 이 클래스를 남겨 둔 이유는, Notifier 가 알림을 띄우기 직전에
 * 스위치와 야간 방해 금지를 확인해야 하는데 그때 네트워크를 탈 수 없기 때문이다.
 */
class SettingsStore(private val context: Context) {

    val notificationFlow: Flow<Map<NotificationKey, Boolean>> =
        context.settingsDataStore.data.map { prefs ->
            NotificationKey.entries.associateWith { key ->
                prefs[booleanPreferencesKey(key.storeKey)] ?: key.default
            }
        }

    suspend fun setNotification(key: NotificationKey, value: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[booleanPreferencesKey(key.storeKey)] = value
        }
    }

    /**
     * 서버에서 받은 설정을 한 번에 덮어쓴다.
     *
     * 항목별로 setNotification 을 반복하면 그 사이사이 값이 화면에 보여
     * 스위치가 차례로 튀는 것처럼 보인다. 한 번의 edit 으로 묶는다.
     */
    suspend fun setAllNotifications(values: Map<NotificationKey, Boolean>) {
        context.settingsDataStore.edit { prefs ->
            values.forEach { (key, value) ->
                prefs[booleanPreferencesKey(key.storeKey)] = value
            }
        }
    }

    /** 해당 알림 종류가 켜져 있는지 (한 번만 읽는 suspend 버전) */
    suspend fun isEnabled(key: NotificationKey): Boolean =
        notificationFlow.first()[key] ?: key.default

    /**
     * 야간 방해 금지(21:00~08:00) 시간대인지.
     * NIGHT_MUTE 가 꺼져 있으면 항상 false.
     */
    suspend fun isQuietNow(): Boolean {
        if (!isEnabled(NotificationKey.NIGHT_MUTE)) return false
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return hour >= QUIET_START_HOUR || hour < QUIET_END_HOUR
    }

    // ── 새 미션 감지용: 이미 사용자에게 알린 미션 ID ──
    private val seenMissionKey = stringSetPreferencesKey("seen_mission_ids")

    suspend fun seenMissionIds(): Set<Int> =
        context.settingsDataStore.data.first()[seenMissionKey]
            ?.mapNotNull { it.toIntOrNull() }?.toSet() ?: emptySet()

    suspend fun setSeenMissionIds(ids: Set<Int>) {
        context.settingsDataStore.edit { prefs ->
            prefs[seenMissionKey] = ids.map { it.toString() }.toSet()
        }
    }

    // ── 랭킹 변동 감지용: 마지막으로 알린 내 순위 (0 = 아직 없음) ──
    private val lastRankKey = intPreferencesKey("last_known_rank")

    suspend fun lastKnownRank(): Int =
        context.settingsDataStore.data.first()[lastRankKey] ?: 0

    suspend fun setLastKnownRank(rank: Int) {
        context.settingsDataStore.edit { prefs -> prefs[lastRankKey] = rank }
    }

    // ── POST_NOTIFICATIONS 권한을 이미 한 번 물어봤는지 ──
    // Android 13+ 는 권한 없이는 알림이 아예 안 뜬다. 그래서 로그인 직후 한 번 물어보는데,
    // 로그인할 때마다 반복해서 띄우면 성가시고, 두 번 거절당하면 시스템이 영구 차단해 버린다.
    // (그 뒤에는 내 정보 > 알림 설정에서 "시스템 설정 열기"로만 되돌릴 수 있다)
    private val pushPermissionAskedKey = booleanPreferencesKey("push_permission_asked")

    suspend fun pushPermissionAsked(): Boolean =
        context.settingsDataStore.data.first()[pushPermissionAskedKey] ?: false

    suspend fun setPushPermissionAsked() {
        context.settingsDataStore.edit { prefs -> prefs[pushPermissionAskedKey] = true }
    }

    companion object {
        const val QUIET_START_HOUR = 21
        const val QUIET_END_HOUR = 8
    }
}
