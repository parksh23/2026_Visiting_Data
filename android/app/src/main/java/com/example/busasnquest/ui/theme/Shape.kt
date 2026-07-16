package com.example.busasnquest.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * 배민식 큰 라운드 형태 토큰.
 * Material3 컴포넌트(Button/Card 등)에 자동 적용된다.
 * (Box 기반 커스텀 카드는 개별적으로 아래 Dimens 라운드 값을 참고해 맞추면 됨)
 */
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),  // 배지/작은 태그
    small      = RoundedCornerShape(12.dp),  // 칩
    medium     = RoundedCornerShape(16.dp),  // 버튼/작은 카드
    large      = RoundedCornerShape(18.dp),  // 일반 카드
    extraLarge = RoundedCornerShape(24.dp)   // 큰 카드/시트
)

/** 화면에서 재사용할 형태·여백 상수 (배민 톤) */
object Dimens {
    // Corner radius
    val radiusChip = 12.dp
    val radiusCard = 18.dp     // 표준 카드/행
    val radiusHero = 20.dp     // 히어로/요약 큰 카드
    val radiusTile = 22.dp
    val radiusButton = 14.dp

    // Spacing (4pt 기준)
    val screenPadding = 20.dp
    val sectionGap = 28.dp
    val cardPadding = 18.dp
    val cardGap = 12.dp

    val bottomBarSpace = 50.dp   // 플로팅 탭바에 안 가려지게 스크롤 콘텐츠 맨 아래 여백

    // Elevation (부드러운 그림자)
    val elevationFloating = 8.dp
    val elevationCard = 2.dp
}
