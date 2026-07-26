package com.example.busasnquest.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.busasnquest.ui.theme.TextMain
import com.example.busasnquest.ui.theme.TextSub

// image_url 이 없을 때 쓰는 구별 그라데이션 폴백 (부산 바다·노을 톤 4종)
private val FallbackGradients = listOf(
    listOf(Color(0xFF5A9BBF), Color(0xFF2C5F7C)), // 바다
    listOf(Color(0xFF7FB8D4), Color(0xFF3A7CA5)), // 하늘
    listOf(Color(0xFFE8B4A0), Color(0xFFB5651D)), // 노을
    listOf(Color(0xFF9FE1CB), Color(0xFF0E7C86))  // 해안
)

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
    compact: Boolean = false
) {
    val mission = item.mission
    val imageHeight = if (compact) 96.dp else 150.dp
    val gradient = FallbackGradients[
        (mission.district.hashCode().let { if (it < 0) -it else it }) % FallbackGradients.size
    ]

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(Dimens.radiusCard))
            .background(CardWhite)
            .clickable { onClick() }
    ) {
        // ── 이미지 영역 ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(imageHeight)
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
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.White.copy(alpha = 0.95f))
                    .padding(horizontal = 9.dp, vertical = 3.dp)
            ) {
                Text(
                    missionTypeLabel(mission.type),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMain
                )
            }

            // 우상단: 하트(찜)
            Icon(
                imageVector = if (item.saved) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = "찜하기",
                tint = if (item.saved) Coral else Color.White,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(20.dp)
                    .clickableNoRipple { onToggleSaved() }
            )
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
                Text(
                    "+${mission.reward}P",
                    fontSize = if (compact) 11.sp else 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Coral
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
                        .clip(RoundedCornerShape(999.dp))
                        .background(bg)
                        .clickable(enabled = enabled) { onAction() }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                if (item.error != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(item.error, color = com.example.busasnquest.ui.theme.PointRed, fontSize = 12.sp)
                }
            }
        }
    }
}
