package kr.co.busanquest.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

// ═══════════════════════════════════════════════════
//  라이트 단일 테마 — "웜 페이퍼 + 코럴 잉크"
//
//  컨셉: 부산 가봤나 = 종이 지도 위에 도장을 찍어 땅을 넓혀가는 느낌.
//        배경은 살짝 크림빛 종이(#F7F3EA), 카드는 순백(#FFFFFF),
//        글자는 검정이 아닌 웜 잉크(#2B2320), 강조는 시그니처 코럴(#E8635F).
//        손글씨 폰트 + 플랫 잉크 아웃라인과 톤이 맞는 조합.
//
//  ⚠️ 시맨틱 이름은 다크 시절 그대로 유지(BgSoftBlue, CardWhite …).
//     전 화면이 이 이름을 참조하므로 여기 값만 바꾸면 앱 전체가 라이트로 전환된다.
//     ※ Elevation.kt / MainActivity 2곳 + 하드코딩 색 몇 군데는 별도 패치 필요.
// ═══════════════════════════════════════════════════

// ── 베이스: 종이와 잉크 ──────────────────────────────
val BgSoftBlue = Color(0xFFF7F3EA)   // 화면 배경 (크림 페이퍼). 순백 카드와 확실히 구분되는 톤차
val CardWhite = Color(0xFFFFFFFF)    // 카드/시트/탭바 표면 (종이 위에 올린 흰 카드)
val SurfaceGray = Color(0xFFF4EEE4)  // 카드 "안"의 블록(통계 타일 등) — 카드보다 한 단 눌린 면

val TextMain = Color(0xFF2B2320)     // 제목/본문 (순검정 대신 웜 잉크. 흰 배경 대비 15.2:1)
val TextSub = Color(0xFF766B62)      // 보조 텍스트 (흰 배경 5.2:1 / 페이퍼 배경 4.7:1 — AA 통과)
val NavyMain = Color(0xFF3B322C)     // (구)네이비 제목 → 진한 잉크 브라운
val NavyLight = Color(0xFF6E635B)    // (구)네이비 보조

val DividerGray = Color(0xFFE8DFD1)  // 경계선/칩 테두리 (웜 그레이. 회색 대신 종이 톤 유지)
val TrackGray = Color(0xFFEEE7DB)    // 진행바 빈 트랙

// ── 시그니처 배색 ────────────────────────────────────
// Coral: "채움(fill)" 전용 — 버튼 배경, 선택 알약, 배지, 진행바 채움, 점령 100%
// CoralDark: "글자·아이콘" 전용 — 흰 배경 위 코럴 텍스트는 대비 3.3:1로 미달이라 이걸 쓴다 (5.3:1)
// CoralInk:  코럴 채움 위의 작은 글자용 (흰 글자는 코럴 위에서 3.3:1이라 부족)
val Coral = Color(0xFFE8635F)        // 강조색 (채움)
val CoralDark = Color(0xFFBC403A)    // 코럴 텍스트/아이콘, 눌림(pressed) 상태 (흰 배경 5.3:1)
val CoralTint = Color(0xFFFCE8E5)    // 연한 코럴 틴트 — 탭바 활성 알약, 배지 배경
val CoralInk = Color(0xFF4A1B0C)     // 코럴 채움 위에 얹는 진한 글자 (버튼/세그먼트 라벨)

// 보조 강조 — 부산의 바다. 흰 배경 대비 4.7:1이라 글자·아이콘에도 쓸 수 있다.
val SeaBlue = Color(0xFF17808B)
val SeaBlueBg = Color(0xFFE3F1F1)    // 바다블루 연한 틴트 배경

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

val PointOrange = Color(0xFFB25F08)  // 포인트/보상 강조 (흰 배경 4.6:1 — 글자·아이콘·채움 모두 안전)
val PointRed = Color(0xFFCC3B3B)     // 에러 텍스트 (흰 배경 대비 4.9:1)

// ── 랭킹 메달 ────────────────────────────────────────
// 메달 원 안에 흰 글자를 쓰면 라이트에서 대비가 부족하다 → 글자는 CoralInk/TextMain 권장.
val MedalGold = Color(0xFFE3A11B)
val MedalSilver = Color(0xFF9AA3B0)
val MedalBronze = Color(0xFFB9743A)

// ── 구·군 진행률 바 색상 (흰 카드 위에서 서로 구분되도록 채도·명도 조정) ──
val BarYellow = Color(0xFFC89108)
val BarCoral = Color(0xFFE2605F)
val BarOrange = Color(0xFFD97F1E)
val BarPurple = Color(0xFF7E6BD1)

// ── 메뉴 아이콘 배경/틴트 (연한 틴트 배경 + 진한 아이콘 = 다크의 정반대) ──
val IconBlueBg = Color(0xFFE8EFFC)
val IconBlue = Color(0xFF3A6FD8)
val IconPinkBg = Color(0xFFFCE9F0)
val IconPink = Color(0xFFD6497E)
val IconGreenBg = Color(0xFFE4F4E9)
val IconGreen = Color(0xFF2E8B57)

// ── 잉크 아웃라인 (플랫 일러스트 테두리) ─────────────
// 다크에선 "밝은 선"이 잉크였다면, 라이트에선 "어두운 선"이 잉크다.
// 다만 라이트 배경 위의 진한 선은 다크보다 훨씬 무겁게 읽히므로,
// 일반 표면은 모래빛 선, 강조 표면만 진짜 잉크선으로 이원화한다.
val InkBorder = Color(0xFFD3C3AA)        // 기본 잉크 라인 (카드·탭바·칩 — 모래빛 펜선)
val InkBorderStrong = Color(0xFF2B2320)  // 강조 라인 (선택된 세그먼트, 히어로 카드 — 만화 톤 진한 선)
// 더 또렷한 만화 톤을 원하면 InkBorder를 0xFFCBB99C 로, 더 담백하게 가려면 0xFFE0D5C3 로 조절.

// ── 스켈레톤 로딩 ────────────────────────────────────
val SkeletonBase = Color(0xFFEDE7DC)      // 블록 기본 (카드보다 살짝 어둡게)
val SkeletonHighlight = Color(0xFFF8F4EC) // 맥동 시 밝아지는 값

// ── 입체감(Elevation) ───────────────────────────────
// 현재 raisedSurface는 플랫(단색 + 아웃라인)이라 Top/Bottom은 실사용되지 않지만,
// 시그니처 호환을 위해 유지. 값은 라이트 기준으로 맞춰둔다.
val RaisedTop = Color(0xFFFFFFFF)      // 표면 윗부분
val RaisedBottom = Color(0xFFFDFBF7)   // 표면 아랫부분
val RaisedBorder = Color(0xFFD3C3AA)   // 가장자리 선 (= InkBorder와 동일 값)
// 눌린 표면 (세그먼트 트랙 등) — 라이트에선 "카드보다 어두운 면"이 눌린 느낌
val SunkenTop = Color(0xFFEFE9DE)
val SunkenBottom = Color(0xFFF5F0E7)
// 지도에서 육지가 바다 위로 떠 보이게 하는 그림자 (검정 대신 웜 브라운)
val MapLandShadow = Color(0xFF6B5B49)

// ── 점령률 히트맵 스케일 (라이트: 진해질수록 점령) ──
// 0% = 아직 안 밟은 빈 종이 → 100% = 코럴로 완전히 칠해진 땅
val Occupancy0 = Color(0xFFF3EADD)
val Occupancy25 = Color(0xFFF7CDBB)
val Occupancy50 = Color(0xFFEE9A85)
val Occupancy75 = Color(0xFFDC5F56)
val Occupancy100 = CoralDark   // 완전 점령 = 코럴 도장을 꾹 찍은 진한 톤

// 히트맵 라벨 텍스트 — 라이트에선 한 색으로 못 덮는다.
// 연한 칸(0~50%)은 잉크 글자, 진한 칸(75~100%)은 흰 글자.
val OccupancyTextDark = Color(0xFF4A2E28)    // 연한 칸용 (잉크 브라운)
val OccupancyTextDarker = Color(0xFFFFFFFF)  // 진한 칸용 (흰 글자)

/** 점령률(0f~1f) → 박스 배경색 */
fun occupancyColor(rate: Float): Color = when {
    rate <= 0f -> Occupancy0
    rate <= 0.25f -> Occupancy25
    rate <= 0.50f -> Occupancy50
    rate <= 0.75f -> Occupancy75
    else -> Occupancy100
}

/**
 * 점령률(0f~1f) → 박스 텍스트색.
 * 라이트 테마에선 배경이 밝→진으로 넘어가므로 50%를 경계로 글자색을 뒤집는다.
 * (시그니처 동일 — 호출부 수정 불필요)
 */
fun occupancyTextColor(rate: Float): Color =
    if (rate <= 0.50f) OccupancyTextDark else OccupancyTextDarker
