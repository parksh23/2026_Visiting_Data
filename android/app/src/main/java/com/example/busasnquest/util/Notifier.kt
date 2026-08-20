package com.example.busasnquest.util

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.busasnquest.MainActivity
import com.example.busasnquest.R
import com.example.busasnquest.data.local.NotificationKey
import com.example.busasnquest.data.local.SettingsStore
import com.example.busasnquest.data.model.OngoingMission
import com.example.busasnquest.notification.RankingNotificationScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** 새 UI가 호출하는 알림 진입점을 현재 WorkManager 알림 구조에 연결한다. */
object Notifier {
    private const val MISSION_CHANNEL = "mission_result"
    private const val NEW_MISSION_CHANNEL = "new_mission"
    private const val MISSION_NOTIFICATION_BASE = 10_000
    private const val NEW_MISSION_NOTIFICATION_ID = 20_001

    private var appContext: Context? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun init(context: Context) {
        appContext = context.applicationContext
        createChannels(context.applicationContext)
    }

    fun missionResult(
        missionId: Int,
        title: String,
        reward: Int,
        success: Boolean,
        reason: String? = null
    ) {
        post(
            key = NotificationKey.MISSION_RESULT,
            channelId = MISSION_CHANNEL,
            notificationId = MISSION_NOTIFICATION_BASE + missionId,
            title = if (success) "인증 완료!" else "인증에 실패했어요",
            body = if (success) "‘$title’ 인증이 통과했어요. +${reward}P 적립!"
            else "‘$title’ — ${reason ?: "다시 시도해주세요."}"
        )
    }

    fun checkNewMissions(missions: List<OngoingMission>) {
        val context = appContext ?: return
        if (missions.isEmpty()) return
        scope.launch {
            val store = SettingsStore(context)
            val currentIds = missions.map(OngoingMission::id).toSet()
            val seenIds = store.seenMissionIds()
            if (seenIds.isEmpty()) {
                store.setSeenMissionIds(currentIds)
                return@launch
            }
            val newMissions = missions.filter { it.id !in seenIds }
            store.setSeenMissionIds(seenIds + currentIds)
            if (newMissions.isEmpty()) return@launch
            post(
                key = NotificationKey.NEW_MISSION,
                channelId = NEW_MISSION_CHANNEL,
                notificationId = NEW_MISSION_NOTIFICATION_ID,
                title = "새 미션이 열렸어요",
                body = if (newMissions.size == 1) "‘${newMissions.first().title}’ 미션이 추가됐어요."
                else "‘${newMissions.first().title}’ 외 ${newMissions.size - 1}개 미션이 추가됐어요."
            )
        }
    }

    /** 실제 순위 비교와 중복 방지는 기존 WorkManager가 담당한다. */
    fun checkRankChange(currentRank: Int) {
        val context = appContext ?: return
        if (currentRank > 0) RankingNotificationScheduler.checkNow(context)
    }

    private fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                MISSION_CHANNEL,
                "미션 인증 결과",
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )
        manager.createNotificationChannel(
            NotificationChannel(
                NEW_MISSION_CHANNEL,
                "새 미션·이벤트",
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )
    }

    @SuppressLint("MissingPermission")
    private fun post(
        key: NotificationKey,
        channelId: String,
        notificationId: Int,
        title: String,
        body: String
    ) {
        val context = appContext ?: return
        scope.launch {
            val store = SettingsStore(context)
            if (!store.isEnabled(key) || store.isQuietNow() || !canPost(context)) return@launch
            val pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_nav_flag)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()
            runCatching {
                NotificationManagerCompat.from(context).notify(notificationId, notification)
            }
        }
    }

    private fun canPost(context: Context): Boolean {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }
}
