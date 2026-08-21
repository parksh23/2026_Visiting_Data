package kr.co.busanquest.util

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
import kr.co.busanquest.MainActivity
import kr.co.busanquest.R
import kr.co.busanquest.data.local.NotificationKey
import kr.co.busanquest.data.local.SettingsStore
import kr.co.busanquest.data.model.OngoingMission
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
     * 서버(FCM) 푸시를 앱이 떠 있는 동안 받았을 때 띄운다.
     *
     * 앱이 백그라운드면 시스템이 알아서 띄우므로 이 함수는 호출되지 않는다.
     * type 은 서버 push_notifications.CHANNELS 의 키와 같다.
     *
     * post() 가 스위치·야간 방해 금지를 다시 확인하지만, 서버도 같은 검사를 이미 했다.
     * 설정이 서버와 동기화되어 있어 두 판단이 어긋나지 않는다.
     */
    fun showRemote(type: String?, title: String?, body: String?) {
        if (title.isNullOrBlank() && body.isNullOrBlank()) return
        val target = when (type) {
            "NEW_MISSION" -> Triple(NotificationKey.NEW_MISSION, CH_NEW_MISSION, ID_NEW_MISSION)
            "RANKING_CHANGE" -> Triple(NotificationKey.RANKING_CHANGE, CH_RANKING, ID_RANKING)
            else -> return          // 모르는 종류는 무시 (서버가 새 타입을 추가한 경우)
        }
        post(
            key = target.first,
            channelId = target.second,
            notificationId = target.third,
            title = title.orEmpty(),
            body = body.orEmpty()
        )
    }

    /**
     * 새 미션 감지.
     *
     * ⚠️ 서버 FCM 푸시로 일원화하면서 호출부를 제거했다. 지금은 쓰이지 않는다.
     *    (남겨 둔 이유: 푸시 없이 동작하는 로컬 전용 빌드로 되돌릴 때 필요하다)
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
     *
     * ⚠️ 서버 FCM 푸시로 일원화하면서 호출부를 제거했다. 지금은 쓰이지 않는다.
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

    // canPost() 안에서 POST_NOTIFICATIONS 를 확인하지만, 검사가 별도 함수라 린트가 그걸 못 본다.
    // (CurrentLocation.kt 의 위치 권한도 같은 이유로 같은 처리를 해 두었다)
    @SuppressLint("MissingPermission")
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

            // 알림을 누르면 종류에 맞는 화면으로 바로 보낸다.
            // (랭킹 변동 → 랭킹 탭, 새 미션 → 미션 탭, 인증 결과 → 미션 기록)
            // 화면 이름은 MainActivity 가 NotificationRoute 로 넘겨 준다.
            val intent = Intent(ctx, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                NotificationRoute.routeForKey(key)?.let {
                    putExtra(NotificationRoute.EXTRA_ROUTE, it)
                }
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
