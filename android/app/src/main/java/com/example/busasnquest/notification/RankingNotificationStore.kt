package com.example.busasnquest.notification

import android.content.Context
import androidx.core.content.edit

class RankingNotificationStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        FILE_NAME,
        Context.MODE_PRIVATE
    )

    @Synchronized
    fun updateRank(currentRank: Int): RankChange? {
        if (currentRank <= 0) return null
        val previous = if (preferences.contains(KEY_LAST_RANK)) {
            preferences.getInt(KEY_LAST_RANK, 0)
        } else {
            null
        }
        preferences.edit { putInt(KEY_LAST_RANK, currentRank) }
        return detectRankChange(previous, currentRank)
    }

    fun wasPermissionRequested(): Boolean =
        preferences.getBoolean(KEY_PERMISSION_REQUESTED, false)

    fun markPermissionRequested() {
        preferences.edit { putBoolean(KEY_PERMISSION_REQUESTED, true) }
    }

    fun clearRank() {
        preferences.edit { remove(KEY_LAST_RANK) }
    }

    private companion object {
        const val FILE_NAME = "ranking_notifications"
        const val KEY_LAST_RANK = "last_all_rank"
        const val KEY_PERMISSION_REQUESTED = "permission_requested"
    }
}
