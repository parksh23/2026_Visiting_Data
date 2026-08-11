package com.example.busasnquest.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.busasnquest.ui.theme.*

/**
 * 앱 공통 포인트 뱃지 — 코럴 원형 'P'.
 * 홈 헤더·미션 보상·프로필 통계 등 포인트가 보이는 모든 곳에서 동일하게 사용해 통일.
 */
@Composable
fun PointBadge(
    size: androidx.compose.ui.unit.Dp = 17.dp,
    fontSize: androidx.compose.ui.unit.TextUnit = 9.sp
) {
    Box(
        modifier = Modifier.size(size).clip(CircleShape).background(Coral),
        contentAlignment = Alignment.Center
    ) {
        // 흰 P 는 코럴 위 3.29:1 로 미달 → 채움색에 맞는 전경색을 자동 선택
        Text("P", color = onFilled(Coral), fontSize = fontSize, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text,
        // 홈 섹션 제목(displayStyle 20sp)과 톤을 맞춘다 — 같은 위계인데 폰트가 달라 따로 놀았다
        style = displayStyle(20.sp),
        color = TextMain,
        modifier = Modifier.padding(horizontal = Dimens.screenPadding)
    )
}

/**
 * 점령률/진행률 카드 (네이비 배경 + 무지개 그라데이션 바).
 */
@Composable
fun ProgressCard(
    label: String,
    percentText: String,
    caption: String,
    progress: Float
) {
    Box(
        modifier = Modifier
            .padding(horizontal = Dimens.screenPadding)
            .fillMaxWidth()
            .shadow(Dimens.elevationFloating, AppShapes.extraLarge)
            .clip(AppShapes.extraLarge)
            .background(Coral)
            .padding(Dimens.cardPadding + 6.dp)
    ) {
        // 코럴 위 흰 글자는 3.29:1 로 본문 크기에서 미달 → 잉크 계열로 자동 전환.
        // 알파를 낮춘 보조 텍스트는 대비가 더 떨어지므로 0.75 대신 0.85 로 올린다.
        val fg = onFilled(Coral)
        Column {
            Text(label, color = fg.copy(0.85f), fontSize = 14.sp)

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                percentText,
                fontSize = 42.sp,
                color = fg,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(caption, color = fg.copy(0.8f), fontSize = 13.sp)

            Spacer(modifier = Modifier.height(18.dp))

            GradientProgressBar(progress)
        }
    }
}

/**
 * 무지개 그라데이션 진행 바 + 0% / 50% / 100% 눈금.
 */
@Composable
fun GradientProgressBar(progress: Float) {
    // (기존의 spectrum 리스트는 실제로 그려지지 않는 죽은 코드라 제거 —
    //  색이 필요해지면 theme/Color.kt 의 SpectrumBar 를 쓰면 된다)

    // 값이 바뀔 때만 차오른다 (첫 컴포지션에서는 목표값에서 시작)
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(Motion.DurValue, easing = Motion.EaseOut),
        label = "gradientProgress"
    )

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(CircleShape)
                .background(Color.White.copy(0.18f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animated)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("0%", color = Color.White.copy(0.6f), fontSize = 11.sp)
            Text("50%", color = Color.White.copy(0.6f), fontSize = 11.sp)
            Text("100%", color = Color.White.copy(0.6f), fontSize = 11.sp)
        }
    }
}

/** 흰 배경 위의 둥근 세그먼트 토글 (전체 / 지역). */
@Composable
fun SegmentedToggle(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    val trackShape = RoundedCornerShape(Dimens.radiusPill)
    val thumbShape = RoundedCornerShape(Dimens.radiusPill - 4.dp)   // 트랙 안쪽 여백 4dp 만큼 뺀다

    Row(
        modifier = Modifier
            .padding(horizontal = Dimens.screenPadding)
            .fillMaxWidth()
            // 트랙은 안으로 들어간 느낌
            .sunkenSurface(trackShape)
            .border(Dimens.borderWidth, InkBorder, trackShape)
            .padding(4.dp)
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            // 선택 배경색을 애니메이션 — 세그먼트는 하루에도 여러 번 누르는 요소라
            // 위치 이동 없이 색만 짧게 넘긴다 (전환 자체를 읽히게만)
            val bg by animateColorAsState(
                targetValue = if (selected) Coral else Color.Transparent,
                animationSpec = tween(Motion.DurRelease, easing = Motion.EaseOut),
                label = "segmentBg"
            )
            val fg by animateColorAsState(
                targetValue = if (selected) OnCoral else TextSub,
                animationSpec = tween(Motion.DurRelease, easing = Motion.EaseOut),
                label = "segmentFg"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(thumbShape)
                    .background(bg)
                    .then(
                        if (selected) Modifier.border(Dimens.borderWidth, InkBorderStrong, thumbShape)
                        else Modifier
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onSelect(index) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    color = fg,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
fun FilterChipBox(label: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(Dimens.radiusChip))
            .background(CardWhite)
            .border(1.dp, DividerGray, RoundedCornerShape(Dimens.radiusChip))
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TextMain, fontSize = 13.sp)
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = TextSub,
            modifier = Modifier.size(16.dp)
        )
    }
}
