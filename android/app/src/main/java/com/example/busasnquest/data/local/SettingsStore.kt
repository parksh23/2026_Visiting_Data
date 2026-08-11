package com.example.busasnquest.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

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
}
