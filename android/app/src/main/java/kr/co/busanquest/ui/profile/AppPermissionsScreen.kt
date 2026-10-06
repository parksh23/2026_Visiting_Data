package kr.co.busanquest.ui.profile

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import kr.co.busanquest.ui.theme.BgSoftBlue
import kr.co.busanquest.ui.theme.Dimens
import kr.co.busanquest.ui.theme.DividerGray
import kr.co.busanquest.ui.theme.TextSub
import kr.co.busanquest.ui.theme.bottomBarSpacing

/** 이용자가 접근권한 항목·목적·거부 영향을 언제든 다시 확인하는 화면. */
@Composable
fun AppPermissionsScreen(navController: NavHostController) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgSoftBlue)
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = bottomBarSpacing()),
    ) {
        SubPageHeader("앱 접근권한 안내", navController)

        SettingsCard("필수적 접근권한") {
            PermissionDescription("없음", "필수 권한 없이 앱의 기본 기능을 이용할 수 있습니다.")
        }

        Spacer(Modifier.height(20.dp))

        SettingsCard("선택적 접근권한") {
            PermissionDescription(
                "위치",
                "현재 위치 및 사진 미션에서 방문 장소와의 거리를 확인하기 위해 사용합니다. 좌표와 정확도는 기기에서만 판정하며 서버로 전송하거나 저장하지 않습니다.",
            )
            HorizontalDivider(color = DividerGray, modifier = Modifier.padding(horizontal = 16.dp))
            PermissionDescription(
                "알림",
                "미션 인증 결과, 새로운 미션과 랭킹 변동 등의 소식을 알려드리기 위해 사용합니다.",
            )
        }

        Spacer(Modifier.height(16.dp))
        Text(
            "선택적 접근권한을 허용하지 않아도 앱을 이용할 수 있지만, 해당 권한이 필요한 일부 기능은 제한될 수 있습니다. 영수증 촬영은 기본 카메라 앱을 연결하므로 카메라 접근권한을 요청하지 않습니다.",
            color = TextSub,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 24.dp),
        )

        Spacer(Modifier.height(20.dp))

        SettingsCard("권한 변경") {
            ValueRow("기기 앱 권한 설정 열기") {
                val intent = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    android.net.Uri.fromParts("package", context.packageName, null),
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                runCatching { context.startActivity(intent) }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun PermissionDescription(title: String, description: String) {
    Column(Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Spacer(Modifier.height(4.dp))
        Text(description, color = TextSub, fontSize = 13.sp, lineHeight = 19.sp)
    }
}
