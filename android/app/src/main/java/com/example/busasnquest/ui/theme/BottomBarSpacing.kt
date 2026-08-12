package com.example.busasnquest.ui.theme

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 스크롤 콘텐츠 맨 아래에 줘야 하는 여백.
 *
 * 하단 탭바는 화면 위에 떠 있는(오버레이) 구조라 콘텐츠 영역을 밀어내지 않는다.
 * 그래서 각 화면이 스스로 그만큼 여백을 확보해야 마지막 항목이 안 가린다.
 *
 * 필요한 높이 = 플로팅 탭바 높이(Dimens.bottomBarClearance ≈ 108dp)
 *             + 시스템 내비게이션 바 인셋(탭바가 navigationBarsPadding 으로 그만큼 위로 뜬다)
 *
 * ⚠️ 인셋은 기기·설정(제스처 내비 vs 3버튼)마다 달라서 고정 dp 로는 맞출 수 없다.
 * 예전에는 Dimens.bottomBarSpace(50dp) 를 썼는데, 실제 탭바 높이의 절반밖에 안 돼서
 * 모든 화면의 마지막 항목이 탭바에 가렸다.
 *
 * 사용:
 *   LazyColumn(contentPadding = PaddingValues(bottom = bottomBarSpacing()))
 *   Column(modifier = Modifier.padding(bottom = bottomBarSpacing()))
 *
 * @param extra 화면별로 더 띄우고 싶을 때만 사용.
 */
// ⚠️ @ReadOnlyComposable 을 붙이면 안 된다.
//    asPaddingValues() 가 일반 컴포저블이라 읽기 전용 컴포저블 안에서는 호출할 수 없다.
@Composable
fun bottomBarSpacing(extra: Dp = 0.dp): Dp {
    val navigationBarInset = WindowInsets.navigationBars
        .asPaddingValues()
        .calculateBottomPadding()
    return Dimens.bottomBarClearance + navigationBarInset + extra
}
