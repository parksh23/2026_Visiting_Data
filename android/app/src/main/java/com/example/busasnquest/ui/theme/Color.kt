package com.example.busasnquest.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

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

// ── 크림(CoralTint) 표면 위 전경색 ──
// 다크 테마의 밝은 글자색을 크림 카드 위에 그대로 쓰면 읽히지 않으므로,
// 화면마다 헥사를 박아 넣는 대신 여기서 시맨틱 이름으로 관리한다.
val OnCoralTint = Color(0xFF4A2E28)      // 크림 위 제목/본문 (대비 ≈ 11:1)
val OnCoralTintSub = Color(0xFF7A5C53)   // 크림 위 보조 텍스트 (대비 ≈ 6:1, 기존 8C6F66 대비 강화)
val CoralTrack = Color(0xFFF0DEDE)       // 크림 위 진행바 트랙
val OnMedalGold = Color(0xFF5A4300)      // 골드 배지 위 글자

/**
 * 코럴 표면 위 잉크 글자 (선택된 세그먼트/칩 등).
 * 기존 0xFF4A1B0C 는 코럴(#E8635F) 위 대비 4.27:1 로 AA 미달이라 5.2:1 로 깊게 조정.
 */
val OnCoral = Color(0xFF33110A)

/**
 * 채워진 색 위에 얹을 글자색을 자동으로 고른다 — 흰색 vs 잉크 중 **대비가 큰 쪽**.
 *
 * 이 앱의 채움색(코럴·민트·골드·실버·브론즈·씨블루)은 전부 중간 밝기라
 * 관행대로 흰 글자를 얹으면 대부분 AA(4.5:1)에 미달한다. 예: 흰색 on 코럴 = 3.29:1.
 * 같은 색에 잉크 글자를 얹으면 5.21:1 로 통과한다.
 *
 * 채움색이 바뀌어도 글자색이 따라오므로, 호출부에서 색을 손으로 짝지을 필요가 없다.
 *   Text(label, color = onFilled(badgeBg))
 */
fun onFilled(background: Color): Color {
    val l = background.luminance()
    val vsWhite = 1.05f / (l + 0.05f)
    val vsInk = (l + 0.05f) / (OnCoral.luminance() + 0.05f)
    return if (vsInk >= vsWhite) OnCoral else Color.White
}

// ── 경고/에러 인라인 배너 (크림 배경 + 벽돌색 글자, 대비 ≈ 6.3:1) ──
val WarnTint = CoralTint
val OnWarnTint = Color(0xFF993C1D)

// ── 미션 상세 히어로 (하늘/바다 톤) ──
val DetailHeroTop = Color(0xFFCFE0F2)
val DetailHeroBottom = Color(0xFFB7D0EA)

// ── 카카오 로그인 브랜드 색 (브랜드 규정값이라 임의 변경 금지) ──
val KakaoYellow = Color(0xFFFEE500)
val OnKakaoYellow = Color(0xFF191919)

/**
 * 진행률 스펙트럼 바 색상 (0% → 100%).
 * 흰 배경이 아니라 코럴 카드 위에 얹히므로 채도를 낮추지 말 것.
 */
val SpectrumBar = listOf(
    Color(0xFFFF5A5A),
    Color(0xFFFF9800),
    Color(0xFFF4D03F),
    Color(0xFF8BC34A),
    Color(0xFF4FC3F7)
)

/**
 * 미션 이미지가 없을 때 쓰는 폴백 그라데이션 (부산 바다·하늘·노을·해안 4종).
 * district 해시로 고르므로 같은 구는 항상 같은 색이 나온다.
 */
val MissionFallbackGradients = listOf(
    listOf(Color(0xFF5A9BBF), Color(0xFF2C5F7C)), // 바다
    listOf(Color(0xFF7FB8D4), Color(0xFF3A7CA5)), // 하늘
    listOf(Color(0xFFE8B4A0), Color(0xFFB5651D)), // 노을
    listOf(Color(0xFF9FE1CB), Color(0xFF0E7C86))  // 해안
)

// ── 잉크 아웃라인 (일러스트 톤 플랫 테두리) ──
// 부드러운 그림자 대신 표면 경계를 그리는 "펜 라인" 색.
val InkBorder = Color(0xFFCFCBC2)        // 기본 잉크 라인 (밝은 웜 그레이)
val InkBorderStrong = Color(0xFFEDEBE8)  // 강조 카드용 굵은 잉크 라인

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
