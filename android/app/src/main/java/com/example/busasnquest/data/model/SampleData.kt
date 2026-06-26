package com.example.busasnquest.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import com.example.busasnquest.ui.theme.*

// ───────────────── 공통 데이터 ─────────────────

const val USER_POINT = "2,450P"
const val USER_NAME = "부산갈매기"

// ───────────────── SAMPLE DATA (스크린샷 기준) ─────────────────

// 홈 - 진행 중인 미션
// 진행 중인 미션 목록 (타입별로 다양하게)
val ongoingMissions = listOf(
    OngoingMission(
        title = "오록도 해안길 걷기",
        region = "남구 용호동",
        reward = 100,
        current = 0,
        total = 1,
        type = MissionType.CURRENT_LOCATION
    ),
    OngoingMission(
        title = "광안리 해변에서 인증샷 찍기",
        region = "수영구 광안동",
        reward = 80,
        current = 0,
        total = 1,
        type = MissionType.PHOTO_LOCATION
    ),
    OngoingMission(
        title = "남포동 맛집에서 식사하기",
        region = "중구 남포동",
        reward = 150,
        current = 0,
        total = 1,
        type = MissionType.RECEIPT
    )
)

// 미션 - 구·군별 진행 현황 (16개)
val districtProgressList = listOf(
    DistrictProgress("중구", 3, 3, BarYellow),
    DistrictProgress("동구", 2, 2, BarCoral),
    DistrictProgress("해운대구", 2, 2, BarPurple),
    DistrictProgress("북구", 1, 2, BarCoral),
    DistrictProgress("동래구", 1, 2, BarOrange),
    DistrictProgress("수영구", 1, 2, BarCoral),
    DistrictProgress("남구", 0, 2, TrackGray),
    DistrictProgress("부산진구", 0, 2, TrackGray),
    DistrictProgress("금정구", 0, 2, TrackGray),
    DistrictProgress("연제구", 0, 2, TrackGray),
    DistrictProgress("사상구", 0, 1, TrackGray),
    DistrictProgress("사하구", 0, 1, TrackGray),
    DistrictProgress("서구", 0, 1, TrackGray),
    DistrictProgress("영도구", 0, 1, TrackGray),
    DistrictProgress("기장군", 0, 1, TrackGray),
    DistrictProgress("강서구", 0, 1, TrackGray),
)

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
