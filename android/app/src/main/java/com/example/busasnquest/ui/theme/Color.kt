package com.example.busasnquest.ui.theme

import androidx.compose.ui.graphics.Color

val BgSoftBlue = Color(0xFFF6F4F1)
val NavyMain = Color(0xFF22314E)
val NavyLight = Color(0xFF3B4D70)
val CardWhite = Color.White
val TextMain = Color(0xFF1E1E1E)
val TextSub = Color(0xFF888888)
val DividerGray = Color(0xFFE5E5E5)
val TrackGray = Color(0xFFE9ECF1)
val PointOrange = Color(0xFFFF9800)
val PointRed = Color(0xFFE94F4F)

// 랭킹 메달 색
val MedalGold = Color(0xFFF4B400)
val MedalSilver = Color(0xFFAEB6C2)
val MedalBronze = Color(0xFFCD7F45)

// 구·군 진행률 바 색상
val BarYellow = Color(0xFFF4C534)
val BarCoral = Color(0xFFEF6F6F)
val BarOrange = Color(0xFFF39A3E)
val BarPurple = Color(0xFF9B8CE0)

// 메뉴 아이콘 배경/틴트
val IconBlueBg = Color(0xFFE4ECFB)
val IconBlue = Color(0xFF3E6DDc)
val IconPinkBg = Color(0xFFFCE4EC)
val IconPink = Color(0xFFE8638F)
val IconGreenBg = Color(0xFFE3F3E8)
val IconGreen = Color(0xFF49A86B)

// ── 리뉴얼 배색 (에어비앤비 모티브: 부산 바다 코럴 + 블루) ──
val Coral = Color(0xFFE8635F)       // 시그니처 강조색 (버튼/선택/강조) — 눈에 편한 소프트 코럴
val CoralDark = Color(0xFFCE504D)   // 코럴 위 세그먼트 트랙 등
val CoralTint = Color(0xFFFDF0EF)   // 내 행/로그아웃 등 옅은 배경
val SeaBlue = Color(0xFF0E7C86)     // 보조 강조 (바다)
val SeaBlueBg = Color(0xFFE1F5EE)   // 바다블루 옅은 배경
val SurfaceGray = Color(0xFFF7F7F7) // 통계 카드 등 섹션 배경

// ── 점령률 히트맵 스케일 (미션 탭 구·군 그리드) ──
// 점령률이 오를수록 코럴이 진해진다. 50% 초과부터는 흰 글자(대비 확보).
val Occupancy0 = CoralTint                  // 0%
val Occupancy25 = Color(0xFFF5C4B3)         // 1~25%
val Occupancy50 = Color(0xFFF0997B)         // 26~50%
val Occupancy75 = Coral                     // 51~75%
val Occupancy100 = CoralDark                // 76~100%

// 옅은 배경 위 텍스트 (같은 코럴 계열의 진한 톤)
val OccupancyTextDark = Color(0xFF993C1D)
val OccupancyTextDarker = Color(0xFF712B13)

/** 점령률(0f~1f) → 박스 배경색 */
fun occupancyColor(rate: Float): Color = when {
    rate <= 0f -> Occupancy0
    rate <= 0.25f -> Occupancy25
    rate <= 0.50f -> Occupancy50
    rate <= 0.75f -> Occupancy75
    else -> Occupancy100
}

/** 점령률(0f~1f) → 박스 텍스트색 (50% 초과부터 흰색) */
fun occupancyTextColor(rate: Float): Color = when {
    rate <= 0.25f -> OccupancyTextDark
    rate <= 0.50f -> OccupancyTextDarker
    else -> Color.White
}