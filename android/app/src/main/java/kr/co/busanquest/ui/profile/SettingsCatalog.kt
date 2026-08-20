package kr.co.busanquest.ui.profile

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 내 정보 탭 하단 "설정" 목록의 정의.
 *
 * 기존에는 SampleData.kt 에 제목+아이콘만 있고 클릭이 빈 람다였다.
 * 각 항목에 이동할 route 를 붙여 실제 화면으로 연결한다.
 */
enum class SettingAction(val route: String) {
    NOTIFICATION("settings/notification"),
    ACCOUNT("settings/account"),
    SUPPORT("support"),
    TERMS("doc/terms"),
    PRIVACY("doc/privacy"),
    LOCATION("doc/location")
}

data class SettingItem(
    val title: String,
    val icon: ImageVector,
    val action: SettingAction
)

/** 한 카드에 이어지는 단일 목록 */
val settingItems: List<SettingItem> = listOf(
    SettingItem("알림 설정", Icons.Outlined.Notifications, SettingAction.NOTIFICATION),
    SettingItem("계정 설정", Icons.Outlined.Person, SettingAction.ACCOUNT),
    SettingItem("문의하기", Icons.Outlined.MailOutline, SettingAction.SUPPORT),
    SettingItem("이용약관", Icons.Outlined.Description, SettingAction.TERMS),
    SettingItem("개인정보처리방침", Icons.Outlined.Shield, SettingAction.PRIVACY),
    SettingItem("위치기반서비스 이용약관", Icons.Outlined.LocationOn, SettingAction.LOCATION)
)
