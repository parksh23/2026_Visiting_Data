package com.example.busasnquest.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.busasnquest.data.model.MissionState
import com.example.busasnquest.data.repository.MissionWithState
import com.example.busasnquest.ui.mission.missionTypeLabel
import com.example.busasnquest.ui.theme.CardWhite
import com.example.busasnquest.ui.theme.Coral
import com.example.busasnquest.ui.theme.Dimens
import com.example.busasnquest.ui.theme.IconGreen
import com.example.busasnquest.ui.theme.InkBorder
import com.example.busasnquest.ui.theme.MissionFallbackGradients
import com.example.busasnquest.ui.theme.Motion
import com.example.busasnquest.ui.theme.TextMain
import com.example.busasnquest.ui.theme.TextSub
import com.example.busasnquest.ui.theme.pressable
import com.example.busasnquest.ui.theme.CoralDark
import com.example.busasnquest.ui.theme.CoralInk

// 폴백 그라데이션은 theme/Color.kt 의 MissionFallbackGradients 로 이관했다.

/**
 * 이미지 히어로 미션 카드 (에어비앤비 리스팅 카드 모티브).
 * 상단 60%: 사진(or 그라데이션) + 좌상단 타입 배지 + 우상단 하트 오버레이
 * 하단: 제목·보상 / 지역·인증방식 / 상태 버튼
 *
 * 미션 탭 종류별 리스트(세로·큰 카드)와 홈 추천(가로·작은 카드)에서 공용.
 * @param compact true 면 홈 캐러셀용 작은 비율
 */
@Composable
fun MissionHeroCard(
    item: MissionWithState,
    onClick: () -> Unit,
    onToggleSaved: () -> Unit,
    onAction: () -> Unit,          // 도전하기/인증하기 (상태에 따라)
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    savePending: Boolean = false   // 찜 요청 중 → 하트 잠금 (중복 요청 방지)
) {
    val mission = item.mission
    val imageHeight = if (compact) 96.dp else 150.dp
    val gradient = MissionFallbackGradients[
        (mission.district.hashCode().let { if (it < 0) -it else it }) % MissionFallbackGradients.size
    ]
    // 카드 안쪽 이미지의 위 모서리 = 카드 radius - 테두리 (모서리 틈 방지)
    val innerTopRadius = Dimens.radiusCard - Dimens.borderWidth

    Column(
        modifier = modifier
            // pressable 을 표면보다 먼저 — 카드 전체가 같이 눌린다
            .pressable(onClick = onClick)
            .clip(RoundedCornerShape(Dimens.radiusCard))
            .background(CardWhite)
            .border(Dimens.borderWidth, InkBorder, RoundedCornerShape(Dimens.radiusCard))
    ) {
        // ── 이미지 영역 ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(imageHeight)
                .clip(
                    RoundedCornerShape(
                        topStart = innerTopRadius,
                        topEnd = innerTopRadius,
                        bottomStart = 0.dp,
                        bottomEnd = 0.dp
                    )
                )
                .background(Brush.linearGradient(gradient))
        ) {
            if (mission.imageUrl != null) {
                AsyncImage(
                    model = mission.imageUrl,
                    contentDescription = mission.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // 좌상단: 인증 방식 배지 (흰 pill)
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.95f))
                    .padding(horizontal = 9.dp, vertical = 3.dp)
            ) {
                Text(
                    missionTypeLabel(mission.type),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMain   // 흰 pill 위 → 잉크 글자
                )
            }

            // 우상단: 하트(찜)
            // 20dp 아이콘을 그대로 누르게 하면 터치 타깃이 너무 작다 → 40dp 박스로 감싼다.
            val heartTint by animateColorAsState(
                targetValue = if (item.saved) Coral else Color.White,
                animationSpec = tween(Motion.DurRelease, easing = Motion.EaseOut),
                label = "heartTint"
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(2.dp)
                    .size(40.dp)
                    // 작은 타깃이라 눌림은 더 크게 줘야 읽힌다
                    .pressable(enabled = !savePending, scaleDown = 0.86f, onClick = onToggleSaved),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (item.saved) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = if (item.saved) "찜 해제" else "찜하기",
                    tint = if (savePending) heartTint.copy(alpha = 0.4f) else heartTint,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // ── 텍스트 영역 ──
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    mission.title,
                    fontSize = if (compact) 13.sp else 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMain,
                    modifier = Modifier.weight(1f)
                )
                // 보상 — 앱 공통 포인트 표시
                PointAmount(
                    value = mission.reward,
                    prefix = "+",
                    badgeSize = if (compact) 14.dp else 16.dp,
                    badgeFontSize = if (compact) 8.sp else 9.sp,
                    fontSize = if (compact) 11.sp else 13.sp,
                    gap = 4.dp
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                mission.region,
                fontSize = if (compact) 11.sp else 12.sp,
                color = TextSub
            )

            if (!compact) {
                Spacer(Modifier.height(10.dp))
                // 상태 버튼 (풀폭 알약)
                val (label, bg, enabled) = when (item.state) {
                    MissionState.NOT_STARTED -> Triple("도전하기", Coral, true)
                    MissionState.IN_PROGRESS -> Triple("인증하기", Coral, true)
                    MissionState.VERIFYING -> Triple("인증 확인 중...", TextSub, false)
                    MissionState.COMPLETED -> Triple("✓ 완료", IconGreen, false)
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pressable(enabled = enabled, onClick = onAction)
                        .clip(CircleShape)
                        .background(bg)
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(label, color = if (bg == Coral) CoralInk else Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                if (item.error != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(item.error, color = com.example.busasnquest.ui.theme.PointRed, fontSize = 12.sp)
                }
            }
        }
    }
}
