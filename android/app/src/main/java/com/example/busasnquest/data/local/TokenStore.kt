package com.example.busasnquest.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// 앱 전체에서 하나만 존재하는 DataStore (파일명: "auth")
private val Context.authDataStore by preferencesDataStore(name = "auth")

/**
 * 로그인 토큰을 기기에 저장/삭제/조회한다.
 * 서버가 생기면 여기에 저장하는 값만 실제 JWT 토큰으로 바뀐다.
 */
class TokenStore(private val context: Context) {

    private val tokenKey = stringPreferencesKey("auth_token")

    // 저장된 토큰을 흘려보내는 Flow (없으면 null)
    val tokenFlow: Flow<String?> = context.authDataStore.data
        .map { prefs -> prefs[tokenKey] }

    suspend fun saveToken(token: String) {
        context.authDataStore.edit { prefs -> prefs[tokenKey] = token }
    }

    suspend fun clear() {
        context.authDataStore.edit { prefs -> prefs.remove(tokenKey) }
    }
}
