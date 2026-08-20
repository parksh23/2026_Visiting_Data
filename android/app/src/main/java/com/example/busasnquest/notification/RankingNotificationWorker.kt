package com.example.busasnquest.notification

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
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.busasnquest.MainActivity
import com.example.busasnquest.R
import com.example.busasnquest.data.local.TokenStore
import com.example.busasnquest.data.local.NotificationKey
import com.example.busasnquest.data.local.SettingsStore
import com.example.busasnquest.data.remote.RetrofitInstance
import kotlinx.coroutines.flow.first
import retrofit2.HttpException
import java.io.IOException

class RankingNotificationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val token = TokenStore(applicationContext).tokenFlow.first()
        if (token.isNullOrBlank()) return Result.success()
        val settings = SettingsStore(applicationContext)
        if (!settings.isEnabled(NotificationKey.RANKING_CHANGE) || settings.isQuietNow()) {
            return Result.success()
        }
        if (!canPostNotifications(applicationContext)) return Result.success()

        RetrofitInstance.init(applicationContext)
        return try {
            val currentRank = RetrofitInstance.rankingApi.getRankings("all").myRank.rank
            RankingNotificationStore(applicationContext).updateRank(currentRank)?.let {
                showRankChangeNotification(applicationContext, it)
            }
            Result.success()
        } catch (error: HttpException) {
            if (error.code() in 500..599) Result.retry() else Result.success()
        } catch (_: IOException) {
            Result.retry()
        } catch (_: Exception) {
            Result.failure()
        }
    }
}

private fun showRankChangeNotification(context: Context, change: RankChange) {
    if (!canPostNotifications(context)) return

    val manager = context.getSystemService(NotificationManager::class.java)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "랭킹 변동",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "전체 랭킹 순위가 바뀌면 알려드립니다."
            }
        )
    }

    val content = when (change.direction) {
        RankChangeDirection.UP ->
            "${change.difference}계단 상승해 현재 ${change.currentRank}위예요!"
        RankChangeDirection.DOWN ->
            "${change.difference}계단 내려가 현재 ${change.currentRank}위예요. 미션으로 순위를 되찾아보세요."
    }
    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        putExtra(EXTRA_OPEN_RANKING, true)
    }
    val pendingIntent = PendingIntent.getActivity(
        context,
        0,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification_rank)
        .setContentTitle("랭킹 순위가 변동됐어요")
        .setContentText(content)
        .setStyle(NotificationCompat.BigTextStyle().bigText(content))
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        .setOnlyAlertOnce(true)
        .build()

    if (
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
        PackageManager.PERMISSION_GRANTED
    ) {
        return
    }
    try {
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    } catch (_: SecurityException) {
        // 권한이 확인 직후 시스템 설정에서 취소된 경쟁 상황은 조용히 무시한다.
    }
}

private fun canPostNotifications(context: Context): Boolean {
    if (
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
        PackageManager.PERMISSION_GRANTED
    ) {
        return false
    }
    return NotificationManagerCompat.from(context).areNotificationsEnabled()
}

const val EXTRA_OPEN_RANKING = "open_ranking"
private const val CHANNEL_ID = "ranking_changes"
private const val NOTIFICATION_ID = 2001
