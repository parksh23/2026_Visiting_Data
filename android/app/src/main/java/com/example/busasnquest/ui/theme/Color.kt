package com.example.busasnquest.ui.theme

import androidx.compose.ui.graphics.Color

// ═══════════════════════════════════════════════════
//  다크 단일 테마 (차콜 배경 + 코럴 강조)
//  ⚠️ 이름은 라이트 시절 그대로지만 값은 다크 기준.
//     전 화면이 이 시맨틱 이름을 참조하므로 여기만 바꾸면 앱 전체가 전환된다.
// ═══════════════════════════════════════════════════

val BgSoftBlue = Color(0xFF1C1B19)   // 화면 배경 (차콜)
val NavyMain = Color(0xFFD8DEE9)     // (구)네이비 제목 → 밝은 회백
val NavyLight = Color(0xFFAEB6C6)
val CardWhite = Color(0xFF262521)    // 카드/시트/탭바 표면
val TextMain = Color(0xFFEDEBE8)     // 제목/본문
val TextSub = Color(0xFFA5A29B)      // 보조 텍스트
val DividerGray = Color(0xFF3A3934)  // 경계선/칩 테두리
val TrackGray = Color(0xFF3A3934)    // 진행바 트랙
val PointOrange = Color(0xFFFFA726)
val PointRed = Color(0xFFEF6B67)     // 에러 텍스트 (다크에서 잘 보이게 약간 밝힘)

// 랭킹 메달 색 (다크에서도 그대로 잘 보임)
val MedalGold = Color(0xFFF4B400)
val MedalSilver = Color(0xFFAEB6C2)
val MedalBronze = Color(0xFFCD7F45)

// 구·군 진행률 바 색상
val BarYellow = Color(0xFFF4C534)
val BarCoral = Color(0xFFEF6F6F)
val BarOrange = Color(0xFFF39A3E)
val BarPurple = Color(0xFF9B8CE0)

// 메뉴 아이콘 배경/틴트 (어두운 틴트 + 밝은 아이콘)
val IconBlueBg = Color(0xFF243244)
val IconBlue = Color(0xFF7FA7F0)
val IconPinkBg = Color(0xFF3D2630)
val IconPink = Color(0xFFF08BAE)
val IconGreenBg = Color(0xFF223528)
val IconGreen = Color(0xFF6CC08B)

// ── 시그니처 배색 (코럴 유지 — 다크에서 더 살아남) ──
val Coral = Color(0xFFE8635F)       // 강조색 (버튼/선택/강조)
val CoralDark = Color(0xFFCE504D)
val CoralTint = Color(0xFFFDF0EF)   // 크림 pill 배지 — 다크 위 포인트로 유지
val SeaBlue = Color(0xFF2FA7B3)     // 보조 강조 (다크 대비 위해 약간 밝힘)
val SeaBlueBg = Color(0xFF20332F)   // 바다블루 어두운 틴트
val SurfaceGray = Color(0xFF2E2D29) // 카드 안 블록(통계 타일 등)

// ── 스켈레톤 로딩 ──
val SkeletonBase = Color(0xFF211F1C)      // 블록 기본
val SkeletonHighlight = Color(0xFF2B2925) // 맥동 시 밝아지는 값

// ── 입체감(Elevation) ──
// 떠 있는 표면: 위가 밝고 아래로 갈수록 어두운 그라데이션 + 가장자리 하이라이트
val RaisedTop = Color(0xFF32302B)      // 표면 윗부분 (빛 받는 쪽)
val RaisedBottom = Color(0xFF252420)   // 표면 아랫부분
val RaisedBorder = Color(0xFF3E3C36)   // 가장자리 하이라이트 선
// 눌린 표면 (세그먼트 트랙 등)
val SunkenTop = Color(0xFF1E1D1A)
val SunkenBottom = Color(0xFF272521)
// 지도에서 육지가 바다 위로 떠 보이게 하는 그림자
val MapLandShadow = Color(0xFF000000)

// ── 점령률 히트맵 스케일 (다크: 밝아질수록 점령) ──
// 0% = 배경에 가까운 어두운 브라운 → 100% = 가장 밝은 코럴
val Occupancy0 = Color(0xFF3A2E2B)
val Occupancy25 = Color(0xFF5C3F38)
val Occupancy50 = Color(0xFF8F5346)
val Occupancy75 = Color(0xFFC75B52)
val Occupancy100 = Coral

// 히트맵 라벨 텍스트 (다크 지도 위 공통 밝은 글자)
val OccupancyTextDark = Color(0xFFEDEBE8)
val OccupancyTextDarker = Color(0xFFEDEBE8)

/** 점령률(0f~1f) → 박스 배경색 */
fun occupancyColor(rate: Float): Color = when {
    rate <= 0f -> Occupancy0
    rate <= 0.25f -> Occupancy25
    rate <= 0.50f -> Occupancy50
    rate <= 0.75f -> Occupancy75
    else -> Occupancy100
}

/** 점령률(0f~1f) → 박스 텍스트색 (다크에선 전 구간 밝은 글자 + 글로우) */
fun occupancyTextColor(rate: Float): Color = OccupancyTextDark
