package com.example.busasnquest.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * 다크 테마용 "떠 있는 표면" 효과.
 *
 * 라이트 테마는 흰 배경 + 회색 그림자로 입체감을 내지만,
 * 다크에서는 그림자가 배경에 묻히므로 3가지를 조합한다:
 *   1) 아래쪽 드롭 섀도 (검정) — 바닥에서 떠 있는 느낌
 *   2) 위→아래 미세 그라데이션 (위가 밝음) — 빛이 위에서 오는 느낌
 *   3) 얇은 테두리 (윗면 밝은 선) — 가장자리 하이라이트
 *
 * 사용: Modifier.raisedSurface(RoundedCornerShape(28.dp))
 */
fun Modifier.raisedSurface(
    shape: Shape,
    elevation: androidx.compose.ui.unit.Dp = 6.dp,
    top: Color = RaisedTop,
    bottom: Color = RaisedBottom,
    borderColor: Color = RaisedBorder
): Modifier = this
    .shadow(
        elevation = elevation,
        shape = shape,
        clip = false,
        ambientColor = Color.Black,
        spotColor = Color.Black
    )
    .clip(shape)
    .background(Brush.verticalGradient(listOf(top, bottom)))
    .border(1.dp, borderColor, shape)

/**
 * 눌린(선택된) 상태 — 반대로 안으로 들어간 느낌.
 * 세그먼트의 비선택 트랙 등 배경으로 쓴다.
 */
fun Modifier.sunkenSurface(
    shape: Shape,
    top: Color = SunkenTop,
    bottom: Color = SunkenBottom
): Modifier = this
    .clip(shape)
    .background(Brush.verticalGradient(listOf(top, bottom)))
