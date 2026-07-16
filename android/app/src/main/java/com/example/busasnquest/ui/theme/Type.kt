package com.example.busasnquest.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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

// 배민식 공통 자간(살짝 좁게) — 한글 가독성/밀도
private val Tracking = (-0.02).em

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
