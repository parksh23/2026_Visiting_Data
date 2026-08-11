package com.example.busasnquest.ui.theme

import androidx.compose.ui.graphics.Color

// ═══════════════════════════════════════════════════
//  라이트 단일 테마 — "웜 페이퍼 + 코럴 잉크"
//
//  컨셉: 부산 땅따먹기 = 종이 지도 위에 도장을 찍어 땅을 넓혀가는 느낌.
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
