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

    val radiusPill = 28.dp     // 검색바 등 알약 형태

    // Spacing (4pt 기준)
    // 값을 세 단계로만 쓴다: 붙은 요소(gapTight) / 블록 내부(gapBlock) / 섹션 사이(sectionGap).
    // 12·16·20·24 를 섞어 쓰면 계층이 읽히지 않는다.
    val screenPadding = 20.dp
    val gapTight = 8.dp        // 라벨-값처럼 한 덩어리로 읽혀야 하는 요소 사이
    val gapBlock = 16.dp       // 같은 섹션 안의 블록 사이
    val sectionGap = 28.dp     // 서로 다른 섹션 사이
    val cardPadding = 18.dp
    val cardGap = 12.dp

    /** 중첩 라운드: 바깥 radius - 테두리/여백 = 안쪽 radius (안 맞으면 모서리에 틈이 보인다) */
    val borderWidth = 1.5.dp

    val bottomBarSpace = 50.dp   // 플로팅 탭바에 안 가려지게 스크롤 콘텐츠 맨 아래 여백
    // 플로팅 탭바 실제 높이(테두리+패딩+아이콘+라벨 ≈ 96dp)를 넘기는 값.
    // 스크롤 화면 맨 아래 콘텐츠가 탭바에 가리지 않게 하려면 이 값을 쓴다.
    val bottomBarClearance = 108.dp

    // Elevation (부드러운 그림자)
    val elevationFloating = 8.dp
    val elevationCard = 2.dp
}
