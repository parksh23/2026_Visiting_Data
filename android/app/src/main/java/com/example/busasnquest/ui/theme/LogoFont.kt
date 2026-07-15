package com.example.busasnquest.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.example.busasnquest.R

/**
 * 로고("부산 땅따먹기") 전용 폰트.
 *
 * 롯데 자이언츠 '투혼투지' 같은 임팩트/붓글씨 느낌을 내려면 아래처럼
 * 무료 폰트 ttf 를 넣고 이 val 한 줄만 교체하면 됩니다.
 *
 *   ● 붓글씨 느낌(가장 유사): 나눔손글씨 붓
 *       https://fonts.google.com/specimen/Nanum+Brush+Script  → ttf 다운로드
 *       app/src/main/res/font/nanum_brush.ttf 로 저장 (파일명은 소문자·언더스코어)
 *
 *   ● 임팩트/포스터 느낌: Black Han Sans (검은고딕)
 *       https://fonts.google.com/specimen/Black+Han+Sans
 *       app/src/main/res/font/black_han_sans.ttf
 *
 *   넣은 뒤 아래 한 줄을 교체:
 *
 *     import androidx.compose.ui.text.font.Font
 *     import com.example.busasnquest.R
 *
 *     val LogoFontFamily = FontFamily(Font(R.font.nanum_brush))
 *     // 또는 val LogoFontFamily = FontFamily(Font(R.font.black_han_sans))
 *
 * 폰트를 넣기 전까지는 시스템 산세리프(굵게)로 표시됩니다.
 */

// ⚠️ 선택: Black Han Sans (임팩트/포스터 느낌)
//    res/font/black_han_sans.ttf 를 넣어야 빌드됩니다.
//    (파일이 없으면 R.font.black_han_sans 미해결로 빌드 에러가 나요)
val LogoFontFamily: FontFamily = FontFamily(Font(R.font.black_han_sans))
