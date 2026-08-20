package kr.co.busanquest.ui.theme

import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode

/**
 * 앱 공통 모션 토큰.
 *
 * 원칙 (.claude/skills/animate, review-animations 기준)
 *  1. UI 모션은 300ms 미만. 값 변화 리드아웃(진행률 등)만 예외적으로 더 길게.
 *  2. 진입/퇴장은 ease-out. ease-in 은 UI 에서 금지 — 사용자가 가장 집중하는 첫 순간을 늦춘다.
 *  3. 내장 easing 은 약하다. 아래 커스텀 커브를 쓴다.
 *  4. 애니메이션 대상은 transform/opacity 상당(=scale/alpha)만. 레이아웃 값(높이·패딩)은 건드리지 않는다.
 *  5. 접근성(애니메이션 끄기)은 애니메이션과 같이 출고한다. "끄기"가 아니라 "더 약하게".
 */
object Motion {
    /** 강한 ease-out — UI 진입/퇴장/터치 피드백 기본값 */
    val EaseOut: Easing = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)

    /** 강한 ease-in-out — 화면 안에서 이동/변형할 때 */
    val EaseInOut: Easing = CubicBezierEasing(0.77f, 0f, 0.175f, 1f)

    /** 누르는 순간: 즉시 반응해야 하므로 짧게 */
    const val DurPress = 120

    /** 떼는 순간: 시스템 응답이므로 조금 느긋하게 (비대칭 타이밍) */
    const val DurRelease = 220

    /**
     * 진행률·수치 리드아웃.
     * 300ms 초과지만 "인터페이스 응답"이 아니라 "값이 바뀐 것을 읽히게 하는" 모션이라 허용.
     */
    const val DurValue = 420

    /** 눌렀을 때 줄어드는 기본 배율. scale(0) 은 절대 쓰지 않는다. */
    const val PressScale = 0.97f
}

/**
 * 시스템 "애니메이션 배율 끄기"(개발자 옵션 / 접근성) 상태.
 * 켜져 있으면 위치가 바뀌는 모션은 빼고 색·투명도 변화만 남긴다.
 */
@Composable
fun rememberReducedMotion(): Boolean {
    if (LocalInspectionMode.current) return false
    val context = LocalContext.current
    return remember(context) {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        ) == 0f
    }
}

/**
 * 터치 피드백이 있는 클릭.
 *
 * ripple 대신 "누르는 즉시" 살짝 줄어드는 스케일로 반응한다.
 * 손가락을 떼는 순간이 아니라 **누르는 순간(press-down)** 에 반응하는 것이 핵심 —
 * 릴리즈에서야 반응하면 인터페이스가 죽은 것처럼 느껴진다.
 *
 * 체이닝 주의: **배경/테두리보다 먼저** 붙여야 표면 전체가 같이 눌린다.
 *   Modifier.fillMaxWidth().pressable { … }.clip(shape).background(…)
 */
fun Modifier.pressable(
    enabled: Boolean = true,
    scaleDown: Float = Motion.PressScale,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val reduceMotion = rememberReducedMotion()

    val target = if (pressed && enabled && !reduceMotion) scaleDown else 1f
    val scale by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(
            durationMillis = if (pressed) Motion.DurPress else Motion.DurRelease,
            easing = Motion.EaseOut
        ),
        label = "pressScale"
    )

    this
        .scale(scale)
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = onClick
        )
}

/**
 * 리스트 행 전용 터치 피드백.
 *
 * 카드 **안에 들어있는 행**을 scale 로 줄이면 행만 쪼그라들어 구분선과 어긋나 보인다.
 * 이런 자리에서는 크기 대신 배경을 잠깐 밝히는 쪽이 맞다.
 *
 * 체이닝: 행의 배경/구분선은 부모 카드가 그리므로 이 modifier 는 padding 앞에 붙인다.
 *   Modifier.fillMaxWidth().pressableRow { … }.padding(18.dp)
 */
fun Modifier.pressableRow(
    enabled: Boolean = true,
    highlight: Color = SurfaceGray,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    val bg by animateColorAsState(
        targetValue = if (pressed && enabled) highlight else Color.Transparent,
        animationSpec = tween(
            durationMillis = if (pressed) Motion.DurPress else Motion.DurRelease,
            easing = Motion.EaseOut
        ),
        label = "pressRowBg"
    )

    this
        .background(bg)
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = onClick
        )
}

/**
 * pressable() 을 쓸 수 없는 경우용 — **누르는 곳과 반응하는 곳이 다를 때**.
 *
 * 예: 미니맵. 터치를 받는 것은 지도 위 투명 오버레이지만,
 * 눌린 느낌은 지도 카드 전체에 걸려야 한다.
 *
 *   val press = rememberPressState()
 *   Box(Modifier.scale(press.scale)) {          // 반응하는 쪽
 *       AndroidView(…)
 *       Box(Modifier.matchParentSize().clickable(press.interactionSource, null) { … })  // 누르는 쪽
 *   }
 */
class PressState(
    val interactionSource: MutableInteractionSource,
    val scale: Float
)

@Composable
fun rememberPressState(scaleDown: Float = Motion.PressScale): PressState {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val reduceMotion = rememberReducedMotion()

    val scale by animateFloatAsState(
        targetValue = if (pressed && !reduceMotion) scaleDown else 1f,
        animationSpec = tween(
            durationMillis = if (pressed) Motion.DurPress else Motion.DurRelease,
            easing = Motion.EaseOut
        ),
        label = "pressScale"
    )
    return PressState(interactionSource, scale)
}
