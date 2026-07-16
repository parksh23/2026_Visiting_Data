package com.example.busasnquest.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import com.example.busasnquest.ui.theme.*


// ───────────────── SAMPLE DATA (스크린샷 기준) ─────────────────

// 랭킹 리스트
val rankingList = listOf(
    RankEntry(1, "바다사랑이", "5,620P"),
    RankEntry(2, "해운대모험가", "4,320P"),
    RankEntry(3, "광안리러버", "3,150P"),
    RankEntry(4, "부산산책자", "2,850P"),
    RankEntry(5, "푸른바다탐험가", "2,450P"),
    RankEntry(12, "부산갈매기 (나)", "2,450P", isMe = true),
    RankEntry(13, "송도바람", "2,340P"),
    RankEntry(14, "남포동여행자", "2,220P"),
    RankEntry(15, "영도나그네", "2,100P"),
    RankEntry(16, "자갈치사랑", "1,980P"),
)

// 내 정보 - 메뉴 카드
val profileMenuItems = listOf(
    MenuItem("미션 내역", "지금까지 완료한 미션을 확인해보세요", Icons.Outlined.Flag, IconBlue, IconBlueBg),
    MenuItem("찜한 미션", "찜해둔 미션을 모아볼 수 있어요", Icons.Filled.Favorite, IconPink, IconPinkBg),
    MenuItem("사진 관리", "미션 인증 사진을 관리하세요", Icons.Outlined.PhotoCamera, IconGreen, IconGreenBg),
)

// 내 정보 - 설정 리스트
val settingItems = listOf(
    SettingItem("알림 설정", Icons.Outlined.Notifications),
    SettingItem("계정 설정", Icons.Outlined.Person),
    SettingItem("고객센터", Icons.Outlined.HelpOutline),
    SettingItem("이용약관", Icons.Outlined.Description),
    SettingItem("개인정보처리방침", Icons.Outlined.Shield),
)