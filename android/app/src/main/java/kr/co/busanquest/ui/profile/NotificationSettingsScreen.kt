package kr.co.busanquest.ui.profile

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import kr.co.busanquest.data.local.NotificationKey
import kr.co.busanquest.data.local.SettingsStore
import kr.co.busanquest.data.repository.NotificationSettingsRepository
import kr.co.busanquest.ui.theme.*
import kr.co.busanquest.util.Notifier
import kotlinx.coroutines.launch

/**
 * 알림 설정.
 *
 * 저장 위치: 서버(USER_SETTINGS). 기기 DataStore 는 화면 표시와
 * Notifier 의 즉시 확인을 위한 캐시다.
 *
 * 서버에 저장해야 하는 이유 — 서버가 FCM 을 보낼지 말지를 이 값으로 판단한다.
 * 기기에만 두면 스위치를 꺼도 푸시가 계속 온다.
 */
@Composable
fun NotificationSettingsScreen(navController: NavHostController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember { SettingsStore(context) }

    val prefs by store.notificationFlow.collectAsStateWithLifecycle(
        initialValue = NotificationKey.entries.associateWith { it.default }
    )

    // 화면에 들어올 때 서버 값을 한 번 받아온다 (다른 기기에서 바꿨을 수 있다).
    // 실패하면 캐시에 있던 값을 그대로 보여준다.
    LaunchedEffect(Unit) { NotificationSettingsRepository.refresh(context) }

    // 스위치 하나를 바꾼다. 화면은 즉시 반응하고, 서버 저장이 실패하면 되돌아온다.
    fun toggle(key: NotificationKey, value: Boolean) {
        scope.launch { NotificationSettingsRepository.set(context, key, value) }
    }

    // 기기 알림이 꺼져 있으면 앱 내 스위치는 의미가 없다 → 배너로 안내
    var systemEnabled by remember {
        mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled())
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            // 설정 앱에 다녀오면 다시 확인
            if (event == Lifecycle.Event.ON_RESUME) {
                systemEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Android 13+ 는 알림 권한을 별도로 받아야 한다
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { systemEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled() }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !systemEnabled) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgSoftBlue)
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = bottomBarSpacing())
    ) {
        SubPageHeader("알림 설정", navController)

        if (!systemEnabled) {
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Dimens.radiusCard))
                    .background(CoralTint)
                    .border(1.5.dp, InkBorder, RoundedCornerShape(Dimens.radiusCard))
                    .clickable { context.openAppNotificationSettings() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.NotificationsOff,
                    contentDescription = null,
                    tint = CoralDark,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "기기 알림이 꺼져 있어요",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = CoralDark
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "아래 설정을 켜도 알림이 오지 않아요. 눌러서 기기 설정을 열어주세요.",
                        fontSize = 12.sp,
                        color = TextSub
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        } else {
            Spacer(Modifier.height(8.dp))
        }

        SettingsCard("미션") {
            ToggleRow(
                title = "미션 인증 결과",
                description = "사진·위치 인증이 통과했는지 알려드려요",
                checked = prefs[NotificationKey.MISSION_RESULT] ?: true
            ) { toggle(NotificationKey.MISSION_RESULT, it) }
            SettingsDivider()
            ToggleRow(
                title = "새 미션·이벤트",
                description = "내 주변에 새 미션이 열리면 알려드려요",
                checked = prefs[NotificationKey.NEW_MISSION] ?: true
            ) { toggle(NotificationKey.NEW_MISSION, it) }
            SettingsDivider()
            ToggleRow(
                title = "랭킹 변동",
                description = "내 순위가 바뀌면 알려드려요",
                checked = prefs[NotificationKey.RANKING_CHANGE] ?: false
            ) { toggle(NotificationKey.RANKING_CHANGE, it) }
        }

        Spacer(Modifier.height(20.dp))

        SettingsCard("방해 금지") {
            ToggleRow(
                title = "야간 방해 금지",
                description = "21:00 ~ 08:00 에는 알림을 보내지 않아요",
                checked = prefs[NotificationKey.NIGHT_MUTE] ?: true
            ) { toggle(NotificationKey.NIGHT_MUTE, it) }
        }

        Spacer(Modifier.height(20.dp))

        SettingsCard("선택 동의") {
            ToggleRow(
                title = "마케팅 정보 수신",
                description = "혜택·이벤트 소식을 받아볼게요 (선택)",
                checked = prefs[NotificationKey.MARKETING] ?: false
            ) { toggle(NotificationKey.MARKETING, it) }
        }

        Spacer(Modifier.height(20.dp))

        // 실제로 알림이 오는지 바로 확인해보는 용도
        SettingsCard("확인") {
            ValueRow(
                title = "테스트 알림 보내기",
                onClick = {
                    Notifier.missionResult(
                        missionId = 0,
                        title = "테스트 미션",
                        reward = 100,
                        success = true
                    )
                }
            )
        }

        Spacer(Modifier.height(16.dp))
        Text(
            "테스트 알림은 위의 '미션 인증 결과' 스위치와 야간 방해 금지 설정을 그대로 따라요.\n" +
                "알림 설정은 계정에 저장돼요. 다른 기기에서 로그인해도 그대로 적용됩니다.",
            fontSize = 12.sp,
            color = TextSub,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        Spacer(Modifier.height(24.dp))
    }
}

/** 앱의 알림 설정 화면으로 이동 (OS 버전별 분기) */
private fun android.content.Context.openAppNotificationSettings() {
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
    } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData(android.net.Uri.fromParts("package", packageName, null))
    }
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { startActivity(intent) }
}
