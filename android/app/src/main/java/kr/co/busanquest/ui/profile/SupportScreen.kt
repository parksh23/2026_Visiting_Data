package kr.co.busanquest.ui.profile

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.MailOutline
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import kr.co.busanquest.ui.theme.*

private const val SUPPORT_EMAIL = "sihyunb88@gmail.com"

private data class Faq(val q: String, val a: String)

private val faqs = listOf(
    Faq(
        "사진 인증이 자꾸 실패해요",
        "사진 인증은 사진에 기록된 촬영 위치(EXIF)를 확인합니다.\n" +
            "· 카메라 앱에서 '위치 정보 저장'을 켠 뒤 촬영해주세요.\n" +
            "· 카카오톡·인스타그램 등으로 전송받은 사진은 위치 정보가 지워져 인증할 수 없어요.\n" +
            "· 스크린샷·갤러리에서 편집한 사진도 위치 정보가 사라질 수 있어요."
    ),
    Faq(
        "'이 사진에는 위치정보가 없어요'라고 나와요",
        "위와 같은 이유입니다. 미션 장소에서 직접 촬영한 원본 사진을 올려주세요.\n" +
            "이미 찍은 사진이라면 갤러리에서 사진 상세정보에 위치가 표시되는지 확인해보세요."
    ),
    Faq(
        "현재 위치 인증이 안 돼요",
        "· 위치 권한을 '앱 사용 중 허용' 이상으로 설정해주세요.\n" +
            "· 실내나 지하에서는 GPS 정확도가 떨어집니다. 야외에서 다시 시도해주세요.\n" +
            "· 기기의 위치 서비스(GPS)가 켜져 있는지 확인해주세요."
    ),
    Faq(
        "미션을 완료했는데 포인트가 안 들어왔어요",
        "인증 결과는 서버에서 확인 후 반영됩니다. 잠시 뒤 앱을 다시 열면 최신 점수가 표시돼요.\n" +
            "10분이 지나도 반영되지 않으면 문의하기로 알려주세요."
    ),
    Faq(
        "미션 목록이 비어 있어요",
        "네트워크 연결을 확인해주세요. 서버와 통신이 안 되면 임시 목록이 보일 수 있어요.\n" +
            "로그인이 만료된 경우 자동으로 로그인 화면으로 이동합니다."
    ),
    Faq(
        "닉네임을 바꾸고 싶어요",
        "내 정보 > 계정 설정에서 변경할 수 있어요.\n2~12자, 공백 없이 입력해야 하고 다른 사용자와 겹칠 수 없습니다."
    ),
    Faq(
        "계정을 삭제하고 싶어요",
        "내 정보 > 계정 설정 > 회원 탈퇴에서 직접 처리할 수 있어요.\n" +
            "모든 정보가 삭제되며 복구할 수 없으니 신중히 결정해주세요."
    )
)

@Composable
fun SupportScreen(
    navController: NavHostController,
    viewModel: ProfileViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var openIndex by remember { mutableStateOf(-1) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgSoftBlue)
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = bottomBarSpacing())
    ) {
        SubPageHeader("문의하기", navController)
        Spacer(Modifier.height(8.dp))

        SettingsCard("자주 묻는 질문") {
            faqs.forEachIndexed { index, faq ->
                FaqRow(
                    faq = faq,
                    expanded = openIndex == index,
                    onToggle = { openIndex = if (openIndex == index) -1 else index }
                )
                if (index != faqs.lastIndex) SettingsDivider()
            }
        }

        Spacer(Modifier.height(20.dp))

        // 문의하기 — 메일 앱으로 넘긴다 (앱 버전·기기 정보 자동 첨부)
        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(Dimens.radiusCard))
                .background(CoralTint)
                .border(1.5.dp, InkBorder, RoundedCornerShape(Dimens.radiusCard))
                .clickable { context.sendSupportMail(uiState.name) }
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Outlined.MailOutline,
                contentDescription = null,
                tint = CoralDark,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("메일로 문의하기", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = CoralDark)
                Spacer(Modifier.height(2.dp))
                Text(
                    "메일 앱이 열리고, 확인에 필요한 정보가 자동으로 입력돼요",
                    fontSize = 12.sp,
                    color = TextSub
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        SettingsCard("앱 정보") {
            ValueRow("앱 버전", context.appVersion(), showChevron = false)
            SettingsDivider()
            ValueRow("기기", "${Build.MANUFACTURER} ${Build.MODEL}", showChevron = false)
            SettingsDivider()
            ValueRow("Android", Build.VERSION.RELEASE ?: "-", showChevron = false)
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun FaqRow(faq: Faq, expanded: Boolean, onToggle: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .animateContentSize()
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                faq.q,
                fontSize = 15.sp,
                fontWeight = if (expanded) FontWeight.Bold else FontWeight.Medium,
                color = TextMain,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(10.dp))
            Icon(
                if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = TextSub,
                modifier = Modifier.size(20.dp)
            )
        }
        if (expanded) {
            Spacer(Modifier.height(10.dp))
            Text(faq.a, fontSize = 13.sp, color = TextSub, lineHeight = 20.sp)
        }
    }
}

private fun Context.appVersion(): String = runCatching {
    packageManager.getPackageInfo(packageName, 0).versionName ?: "-"
}.getOrDefault("-")

/** 문의 메일 — 대응에 필요한 환경 정보를 본문에 미리 채워 보낸다 */
private fun Context.sendSupportMail(nickname: String) {
    val body = buildString {
        append("\n\n\n──────────────────\n")
        append("아래 정보는 문의 확인용이에요. 지우지 말아주세요.\n")
        append("앱 버전: ${appVersion()}\n")
        append("기기: ${Build.MANUFACTURER} ${Build.MODEL}\n")
        append("Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})\n")
        append("닉네임: ${nickname.ifBlank { "(불러오지 못함)" }}\n")
    }
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:")
        putExtra(Intent.EXTRA_EMAIL, arrayOf(SUPPORT_EMAIL))
        putExtra(Intent.EXTRA_SUBJECT, "[부산 땅따먹기] 문의")
        putExtra(Intent.EXTRA_TEXT, body)
    }
    runCatching { startActivity(Intent.createChooser(intent, "문의 메일 보내기")) }
}
