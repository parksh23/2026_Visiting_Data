package com.example.busasnquest.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextGeometricTransform
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.busasnquest.R

/**
 * 앱 공통 폰트.
 *
 * 지금은 시스템 산세리프(FontFamily.Default)를 쓴다. 배민처럼 Pretendard를 쓰려면:
 *   1) res/font/ 에 pretendard_regular.ttf, pretendard_medium.ttf, pretendard_bold.ttf 추가
 *   2) 아래 val 을 다음으로 교체:
 *
 *   val AppFontFamily = FontFamily(
 *       Font(R.font.pretendard_regular, FontWeight.Normal),
 *       Font(R.font.pretendard_medium,  FontWeight.Medium),
 *       Font(R.font.pretendard_bold,    FontWeight.Bold)
 *   )
 *   (import androidx.compose.ui.text.font.Font, com.example.busasnquest.R 추가)
 */
val AppFontFamily: FontFamily = FontFamily(
    Font(R.font.pretendard_regular,  FontWeight.Normal),
    Font(R.font.pretendard_medium,   FontWeight.Medium),
    Font(R.font.pretendard_semibold, FontWeight.SemiBold),
    Font(R.font.pretendard_bold,     FontWeight.Bold)
)

/**
 * 손글씨(학교안심 둥근미소) 폰트 — R/B 2 weight.
 * 일러스트 톤에 맞춰 "제목류" 디스플레이 폰트로 사용한다.
 */
val HandFontFamily: FontFamily = FontFamily(
    Font(R.font.hakgyoansim_dunggeunmiso,      FontWeight.Normal),
    Font(R.font.hakgyoansim_dunggeunmiso_bold, FontWeight.Bold)
)

/**
 * 디스플레이(헤딩) 전용 폰트.
 * 리뉴얼: 손글씨(둥근미소)로 교체 → 워드마크·화면 제목·섹션 제목·카드 제목 등
 * displayStyle()을 쓰는 "큰 글자"가 전부 손글씨로 전환된다. (숫자는 accentStyle 유지)
 */
val DisplayFontFamily: FontFamily = HandFontFamily

/**
 * 영문·숫자 액센트 폰트.
 * Anton 을 res/font/anton.ttf 로 추가하면 아래 한 줄만 바꾸면 된다:
 *   val AccentFontFamily = FontFamily(Font(R.font.anton))
 * (Anton 은 한글 글리프가 없으므로 절대 한글에 쓰지 말 것)
 * 지금은 같은 콘덴스드 톤의 Black Han Sans 라틴 글리프로 대체.
 */
val AccentFontFamily: FontFamily = FontFamily(Font(R.font.black_han_sans))

// 배민식 공통 자간(살짝 좁게) — 한글 가독성/밀도
private val Tracking = (-0.02).em

/**
 * 디스플레이 헤딩 스타일 — 가로 80% 압축 + 좁은 자간으로 콘덴스드 느낌.
 * 사용: Text("제목", style = displayStyle(24.sp), color = TextMain)
 *
 * @param size 글자 크기
 * @param scaleX 가로 압축률 (0.75~0.85 사이에서 조절)
 */
fun displayStyle(
    size: TextUnit,
    scaleX: Float = 0.94f,          // 손글씨는 과압축 방지 (기존 0.8 → 0.94)
    letterSpacing: TextUnit = 0.sp  // 손글씨는 자간 0
): TextStyle = TextStyle(
    fontFamily = DisplayFontFamily,
    fontWeight = FontWeight.Bold,   // 둥근미소 Bold 사용
    fontSize = size,
    letterSpacing = letterSpacing,
    textGeometricTransform = TextGeometricTransform(scaleX = scaleX)
)

/**
 * 영문·숫자 액센트 스타일 (통계 숫자, "MY"/"RECENT" 같은 마이크로 라벨).
 * 한글에는 사용 금지.
 */
fun accentStyle(
    size: TextUnit,
    letterSpacing: TextUnit = 0.03.em
): TextStyle = TextStyle(
    fontFamily = AccentFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = size,
    letterSpacing = letterSpacing
)

private fun TextStyle.brand(): TextStyle =
    copy(fontFamily = AppFontFamily, letterSpacing = Tracking)

private val d = Typography()

/**
 * 배민 스타일 타이포 스케일 (색상 제외).
 * 굵기 대비 뚜렷 + 넉넉한 행간 + 살짝 좁은 자간.
 */
val AppTypography = Typography(
    displayLarge  = d.displayLarge.brand(),
    displayMedium = d.displayMedium.brand(),
    displaySmall  = d.displaySmall.brand(),

    headlineLarge  = d.headlineLarge.brand().copy(fontWeight = FontWeight.Bold, lineHeight = 1.35.em),
    headlineMedium = d.headlineMedium.brand().copy(fontWeight = FontWeight.Bold, lineHeight = 1.35.em),
    headlineSmall  = d.headlineSmall.brand().copy(fontWeight = FontWeight.Bold, lineHeight = 1.4.em),

    titleLarge  = d.titleLarge.brand().copy(fontSize = 20.sp, fontWeight = FontWeight.Bold, lineHeight = 1.4.em),
    titleMedium = d.titleMedium.brand().copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, lineHeight = 1.4.em),
    titleSmall  = d.titleSmall.brand().copy(fontWeight = FontWeight.SemiBold, lineHeight = 1.4.em),

    bodyLarge  = d.bodyLarge.brand().copy(fontSize = 15.sp, lineHeight = 1.55.em),
    bodyMedium = d.bodyMedium.brand().copy(fontSize = 14.sp, lineHeight = 1.55.em),
    bodySmall  = d.bodySmall.brand().copy(fontSize = 13.sp, lineHeight = 1.5.em),

    labelLarge  = d.labelLarge.brand().copy(fontWeight = FontWeight.Medium),
    labelMedium = d.labelMedium.brand().copy(fontWeight = FontWeight.Medium),
    labelSmall  = d.labelSmall.brand().copy(fontWeight = FontWeight.Medium)
)

/**
 * 앱 전역 기본 텍스트 스타일.
 * MainActivity에서 LocalTextStyle 로 제공하면, 화면들이 fontSize만 지정하고
 * 폰트·자간을 안 지정한 Text 들도 자동으로 이 폰트·자간을 상속한다. → 전 탭 공통 적용.
 */
val BaeminBaseTextStyle: TextStyle = AppTypography.bodyLarge
