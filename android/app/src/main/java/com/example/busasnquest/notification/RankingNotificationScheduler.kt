package com.example.busasnquest.notification

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

object RankingNotificationScheduler {
    private const val PERIODIC_WORK = "ranking-change-periodic"
    private const val IMMEDIATE_WORK = "ranking-change-immediate"

    fun start(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val periodic = PeriodicWorkRequestBuilder<RankingNotificationWorker>(
            15,
            TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            periodic
        )
        checkNow(context)
    }

    fun checkNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<RankingNotificationWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            IMMEDIATE_WORK,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun stopAndClearRank(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_WORK)
        WorkManager.getInstance(context).cancelUniqueWork(IMMEDIATE_WORK)
        RankingNotificationStore(context).clearRank()
    }
}

/** 알림을 눌렀을 때 실행 중이거나 새로 시작된 화면 모두 랭킹 탭으로 이동시킨다. */
object RankingNotificationNavigation {
    private val destinations = Channel<Unit>(capacity = Channel.BUFFERED)
    val openRanking = destinations.receiveAsFlow()

    fun requestOpenRanking() {
        destinations.trySend(Unit)
    }
}
