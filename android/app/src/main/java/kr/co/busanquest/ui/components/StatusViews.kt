package kr.co.busanquest.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.animation.animateColor
import androidx.compose.runtime.getValue
import kr.co.busanquest.ui.theme.Coral
import kr.co.busanquest.ui.theme.Dimens
import kr.co.busanquest.ui.theme.OnWarnTint
import kr.co.busanquest.ui.theme.SkeletonBase
import kr.co.busanquest.ui.theme.SkeletonHighlight
import kr.co.busanquest.ui.theme.TextSub
import kr.co.busanquest.ui.theme.WarnTint
import kr.co.busanquest.ui.theme.pressable
import kr.co.busanquest.ui.theme.CoralDark
import kr.co.busanquest.ui.theme.CoralInk

/**
 * API 로딩/에러 상태를 모든 화면에서 같은 모양으로 보여주는 공통 컴포넌트.
 *
 * 사용 예:
 *   is UiState.Loading -> LoadingView()
 *   is UiState.Error   -> ErrorView(message = s.message, onRetry = viewModel::retry)
 */
@Composable
fun LoadingView(message: String? = null) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(color = Coral)
        if (message != null) {
            Spacer(Modifier.height(12.dp))
            Text(message, color = TextSub, fontSize = 13.sp)
        }
    }
}

/**
 * 스켈레톤 블록 — 로딩 중 콘텐츠 자리를 어두운 블록 + 맥동으로 표시.
 * 크기·모양은 호출부의 modifier/shape 로 지정.
 *
 * 사용 예: SkeletonBox(Modifier.width(160.dp).height(16.dp))
 */
@Composable
fun SkeletonBox(
    modifier: Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(10.dp)
) {
    val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "skeleton")
    val color by transition.animateColor(
        initialValue = SkeletonBase,
        targetValue = SkeletonHighlight,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(600),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "skeletonColor"
    )
    Box(modifier.clip(shape).background(color))
}

/**
 * 화면 전체를 덮지 않는 인라인 에러 배너.
 * 로컬(샘플) 데이터를 보여주면서 "서버에서 못 불러왔다"는 사실만 알릴 때 사용.
 */
@Composable
fun InlineErrorBanner(
    message: String,
    onRetry: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .padding(horizontal = Dimens.screenPadding)
            .clip(RoundedCornerShape(Dimens.radiusChip))
            .background(WarnTint)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(message, color = OnWarnTint, fontSize = 12.sp)
        if (onRetry != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                "다시 시도",
                color = CoralDark,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.pressable(scaleDown = 0.96f, onClick = onRetry)
            )
        }
    }
}

@Composable
fun ErrorView(
    message: String,
    onRetry: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Outlined.WifiOff,
            contentDescription = null,
            tint = TextSub,
            modifier = Modifier
        )
        Spacer(Modifier.height(12.dp))
        Text(
            message,
            color = TextSub,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
        if (onRetry != null) {
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    // 버튼은 표면보다 pressable 이 먼저 와야 알약 전체가 눌린다
                    .pressable(onClick = onRetry)
                    .clip(CircleShape)
                    .background(Coral)
                    .padding(horizontal = 24.dp, vertical = 10.dp)
            ) {
                Text(
                    "다시 시도",
                    color = CoralInk,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
