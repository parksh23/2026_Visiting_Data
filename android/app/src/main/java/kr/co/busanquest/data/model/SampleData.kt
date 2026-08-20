package kr.co.busanquest.data.model

import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import kr.co.busanquest.R
import kr.co.busanquest.ui.theme.*


// ───────────────── SAMPLE DATA (스크린샷 기준) ─────────────────

// 내 정보 - 메뉴 카드
val profileMenuItems = listOf(
    MenuItem("미션 내역", "지금까지 완료한 미션을 확인해보세요", R.drawable.ic_nav_flag, IconBlue, IconBlueBg),
    MenuItem("찜한 미션", "찜해둔 미션을 모아볼 수 있어요", R.drawable.ic_menu_heart, IconPink, IconPinkBg),
)
