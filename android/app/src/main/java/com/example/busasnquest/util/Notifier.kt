package com.example.busasnquest.util

import android.Manifest
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 앱 알림을 실제로 띄우는 곳.
 *
 * 내 정보 > 알림 설정의 스위치를 여기서 전부 존중한다.
 *   · 해당 종류 스위치가 꺼져 있으면 보내지 않음
 *   · 야간 방해 금지(21:00~08:00)면 보내지 않음
 *   · 기기 알림이 꺼져 있거나 Android 13+ 권한이 없으면 보내지 않음
 *
 * ⚠️ 지금은 "앱이 살아 있을 때" 감지해서 띄우는 로컬 알림이다.
 *    앱이 완전히 종료된 상태에서도 받으려면 FCM(서버 푸시) 또는
 *    WorkManager 주기 확인이 추가로 필요하다.
 *
 * RetrofitInstance 와 같은 방식으로 MainActivity 에서 init() 한 번 호출한다.
 */
object Notifier {

    private const val CH_MISSION = "mission_result"
    private const val CH_NEW_MISSION = "new_mission"
    private const val CH_RANKING = "ranking_change"

    // 알림 ID — 같은 종류는 덮어쓰도록 고정값 사용 (미션 결과만 미션별로 분리)
    private const val ID_NEW_MISSION = 20_001
    private const val ID_RANKING = 20_002
    private const val ID_MISSION_BASE = 10_000

    private var appContext: Context? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun init(context: Context) {
        val ctx = context.applicationContext
        appContext = ctx
        createChannels(ctx)
    }

    private fun createChannels(ctx: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = ctx.getSystemService(NotificationManager::class.java) ?: return
        listOf(
            Triple(CH_MISSION, "미션 인증 결과", "사진·위치 인증이 통과했는지 알려줍니다."),
            Triple(CH_NEW_MISSION, "새 미션·이벤트", "새로 열린 미션을 알려줍니다."),
            Triple(CH_RANKING, "랭킹 변동", "내 순위가 바뀌면 알려줍니다.")
        ).forEach { (id, name, desc) ->
            manager.createNotificationChannel(
                NotificationChannel(id, name, NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = desc
                }
            )
        }
    }

    // ── 공개 API ──────────────────────────────────────────

    /** 미션 인증 결과 (성공/실패) */
    fun missionResult(missionId: Int, title: String, reward: Int, success: Boolean, reason: String? = null) {
        post(
            key = NotificationKey.MISSION_RESULT,
            channelId = CH_MISSION,
            notificationId = ID_MISSION_BASE + missionId,
            title = if (success) "인증 완료!" else "인증에 실패했어요",
            body = if (success) "‘$title’ 인증이 통과했어요. +${reward}P 적립!"
            else "‘$title’ — ${reason ?: "다시 시도해주세요."}"
        )
    }

    /**
     * 새 미션 감지.
     * 서버에서 받은 미션 목록과 "이미 알린 목록"을 비교해 새 것만 알린다.
     * 첫 실행에는 전부 새 미션이므로 알리지 않고 기준만 저장한다.
     */
    fun checkNewMissions(missions: List<OngoingMission>) {
        val ctx = appContext ?: return
        if (missions.isEmpty()) return
        scope.launch {
            val store = SettingsStore(ctx)
            val current = missions.map { it.id }.toSet()
            val seen = store.seenMissionIds()

            if (seen.isEmpty()) {          // 첫 실행 — 기준만 세우고 조용히 넘어감
                store.setSeenMissionIds(current)
                return@launch
            }
            val fresh = missions.filter { it.id !in seen }
            store.setSeenMissionIds(seen + current)
            if (fresh.isEmpty()) return@launch

            post(
                key = NotificationKey.NEW_MISSION,
                channelId = CH_NEW_MISSION,
                notificationId = ID_NEW_MISSION,
                title = "새 미션이 열렸어요",
                body = if (fresh.size == 1) "‘${fresh.first().title}’ 미션이 추가됐어요."
                else "‘${fresh.first().title}’ 외 ${fresh.size - 1}개 미션이 추가됐어요."
            )
        }
    }

    /**
     * 랭킹 변동 감지.
     * 마지막으로 알린 순위와 다를 때만 알린다. 첫 조회는 기준만 저장.
     */
    fun checkRankChange(currentRank: Int) {
        val ctx = appContext ?: return
        if (currentRank <= 0) return
        scope.launch {
            val store = SettingsStore(ctx)
            val previous = store.lastKnownRank()
            store.setLastKnownRank(currentRank)
            if (previous == 0 || previous == currentRank) return@launch

            val up = currentRank < previous       // 숫자가 작아지면 순위 상승
            val gap = kotlin.math.abs(previous - currentRank)
            post(
                key = NotificationKey.RANKING_CHANGE,
                channelId = CH_RANKING,
                notificationId = ID_RANKING,
                title = if (up) "순위가 올랐어요!" else "순위가 내려갔어요",
                body = if (up) "${previous}위 → ${currentRank}위 (${gap}계단 상승)"
                else "${previous}위 → ${currentRank}위 (${gap}계단 하락)"
            )
        }
    }

    // ── 내부 ──────────────────────────────────────────────

    private fun post(
        key: NotificationKey,
        channelId: String,
        notificationId: Int,
        title: String,
        body: String
    ) {
        val ctx = appContext ?: return
        scope.launch {
            val store = SettingsStore(ctx)
            if (!store.isEnabled(key)) return@launch      // 해당 스위치 OFF
            if (store.isQuietNow()) return@launch         // 야간 방해 금지
            if (!canPost(ctx)) return@launch              // 기기 알림/권한 없음

            val intent = Intent(ctx, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val pending = PendingIntent.getActivity(
                ctx, notificationId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(ctx, channelId)
                // 상태바 아이콘은 알파 채널만 쓰이므로 깃발 실루엣이 흰색으로 그려진다
                .setSmallIcon(R.drawable.ic_nav_flag)
                .setColor(0xFFE8635F.toInt())             // Coral
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pending)
                .build()

            runCatching {
                NotificationManagerCompat.from(ctx).notify(notificationId, notification)
            }
        }
    }

    private fun canPost(ctx: Context): Boolean {
        if (!NotificationManagerCompat.from(ctx).areNotificationsEnabled()) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(
                ctx, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        }
        return true
    }
}
