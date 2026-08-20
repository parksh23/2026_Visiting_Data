package com.example.busasnquest.ui.profile

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import com.example.busasnquest.notification.RankingNotificationScheduler
import com.example.busasnquest.notification.RankingNotificationStore
import com.example.busasnquest.ui.theme.BgSoftBlue
import com.example.busasnquest.ui.theme.TextSub

@Composable
fun RankingNotificationSettingsScreen(navController: NavHostController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var enabled by remember { mutableStateOf(context.rankingNotificationsEnabled()) }

    fun refreshStatus() {
        val wasEnabled = enabled
        enabled = context.rankingNotificationsEnabled()
        if (!wasEnabled && enabled) {
            RankingNotificationStore(context).clearRank()
            RankingNotificationScheduler.checkNow(context)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        refreshStatus()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshStatus()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgSoftBlue)
            .navigationBarsPadding()
    ) {
        SubPageHeader("랭킹 알림", navController)
        Spacer(Modifier.height(8.dp))
        SettingsCard(label = "알림 설정") {
            ToggleRow(
                title = "순위 변동 알림",
                description = "전체 랭킹이 오르거나 내려가면 시스템 알림으로 알려드려요.",
                checked = enabled,
                onCheckedChange = { requested ->
                    if (
                        requested &&
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        RankingNotificationStore(context).markPermissionRequested()
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        context.startActivity(
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            }
                        )
                    }
                }
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "Android의 배터리 정책에 따라 순위 확인 시점은 조금 늦어질 수 있으며, " +
                "네트워크에 연결된 상태에서 주기적으로 확인합니다.",
            color = TextSub,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}

private fun android.content.Context.rankingNotificationsEnabled(): Boolean {
    val permissionGranted =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    return permissionGranted && NotificationManagerCompat.from(this).areNotificationsEnabled()
}
