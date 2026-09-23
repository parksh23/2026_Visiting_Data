package kr.co.busanquest.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** 접근권한 고지와 Android 시스템 권한 동의를 분리하기 위한 공통 안내창. */
@Composable
fun AccessPermissionOverviewDialog(
    onConfirm: () -> Unit,
    onLater: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onLater,
        title = { Text("앱 접근권한 안내") },
        text = {
            Column {
                Text("[필수적 접근권한]", fontWeight = FontWeight.Bold)
                Text("없음")
                Spacer(Modifier.height(12.dp))
                Text("[선택적 접근권한]", fontWeight = FontWeight.Bold)
                Text("• 위치: 현재 위치 및 사진 미션의 방문 장소 인증")
                Text("• 알림: 새로운 미션과 활동 알림 제공")
                Spacer(Modifier.height(12.dp))
                Text("선택적 접근권한을 허용하지 않아도 앱을 이용할 수 있지만, 해당 권한이 필요한 일부 기능은 제한될 수 있습니다.")
                Spacer(Modifier.height(8.dp))
                Text("영수증 촬영은 기본 카메라 앱을 연결하며, 부산 가봤나는 카메라 접근권한을 요청하지 않습니다.")
            }
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("확인") } },
        dismissButton = { TextButton(onClick = onLater) { Text("나중에") } },
    )
}

@Composable
fun LocationPermissionDisclosureDialog(
    onAllow: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("위치 권한 안내") },
        text = {
            Column {
                Text("[선택] 정확한 위치", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text("현재 위치 및 사진 미션에서 방문 장소와의 거리를 확인하기 위해 사용합니다.")
                Spacer(Modifier.height(8.dp))
                Text("허용하지 않아도 앱의 다른 기능은 이용할 수 있지만, 위치·사진 미션 인증은 제한됩니다.")
                Spacer(Modifier.height(8.dp))
                Text("위치 좌표와 정확도는 기기에서만 판정하며 서버로 전송하거나 저장하지 않습니다.")
            }
        },
        confirmButton = { TextButton(onClick = onAllow) { Text("권한 허용") } },
        dismissButton = { TextButton(onClick = onCancel) { Text("취소") } },
    )
}

@Composable
fun NotificationPermissionDisclosureDialog(
    onAllow: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("알림 권한 안내") },
        text = {
            Column {
                Text("[선택] 알림", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text("미션 인증 결과, 새로운 미션과 랭킹 변동 등의 소식을 알려드리기 위해 사용합니다.")
                Spacer(Modifier.height(8.dp))
                Text("허용하지 않아도 앱의 다른 기능은 이용할 수 있으며, 알림만 받을 수 없습니다.")
            }
        },
        confirmButton = { TextButton(onClick = onAllow) { Text("권한 허용") } },
        dismissButton = { TextButton(onClick = onCancel) { Text("취소") } },
    )
}

@Composable
fun PermissionSettingsDialog(
    title: String,
    message: String,
    onOpenSettings: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onOpenSettings) { Text("설정으로 이동") } },
        dismissButton = { TextButton(onClick = onCancel) { Text("취소") } },
    )
}
