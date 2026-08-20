package com.example.busasnquest.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.util.Calendar
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

enum class NotificationKey(val storeKey: String, val default: Boolean) {
    MISSION_RESULT("noti_mission_result", true),
    NEW_MISSION("noti_new_mission", true),
    RANKING_CHANGE("noti_ranking_change", false),
    NIGHT_MUTE("noti_night_mute", true),
    MARKETING("noti_marketing", false)
}

/** 새 UI의 알림 스위치를 실제 알림 처리 계층과 연결하는 기기 로컬 저장소. */
class SettingsStore(private val context: Context) {
    val notificationFlow: Flow<Map<NotificationKey, Boolean>> =
        context.settingsDataStore.data.map { preferences ->
            NotificationKey.entries.associateWith { key ->
                preferences[booleanPreferencesKey(key.storeKey)] ?: key.default
            }
        }

    suspend fun setNotification(key: NotificationKey, value: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[booleanPreferencesKey(key.storeKey)] = value
        }
    }

    suspend fun isEnabled(key: NotificationKey): Boolean =
        notificationFlow.first()[key] ?: key.default

    suspend fun isQuietNow(): Boolean {
        if (!isEnabled(NotificationKey.NIGHT_MUTE)) return false
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return hour >= QUIET_START_HOUR || hour < QUIET_END_HOUR
    }

    private val seenMissionKey = stringSetPreferencesKey("seen_mission_ids")

    suspend fun seenMissionIds(): Set<Int> =
        context.settingsDataStore.data.first()[seenMissionKey]
            ?.mapNotNull(String::toIntOrNull)
            ?.toSet()
            ?: emptySet()

    suspend fun setSeenMissionIds(ids: Set<Int>) {
        context.settingsDataStore.edit { preferences ->
            preferences[seenMissionKey] = ids.map(Int::toString).toSet()
        }
    }

    private companion object {
        const val QUIET_START_HOUR = 21
        const val QUIET_END_HOUR = 8
    }
}
