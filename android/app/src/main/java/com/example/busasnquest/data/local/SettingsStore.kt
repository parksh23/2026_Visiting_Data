package com.example.busasnquest.data.local

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
 * 알림 설정 저장소.
 *
 * 지금은 기기 로컬(DataStore)에만 저장한다. 서버에 USER_SETTINGS 가 생기면
 * 이 클래스 내부만 서버 호출로 바꾸면 되고 화면 코드는 그대로 둘 수 있다.
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

    companion object {
        const val QUIET_START_HOUR = 21
        const val QUIET_END_HOUR = 8
    }
}
