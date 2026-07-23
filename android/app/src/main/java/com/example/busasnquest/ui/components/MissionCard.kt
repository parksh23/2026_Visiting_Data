package com.example.busasnquest.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Image as ImageIcon   // ⚠️ foundation.Image와 이름 충돌 → 별칭 필수
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage                    // Coil (build.gradle에 이미 추가됨)
import com.example.busasnquest.data.model.MissionState
import com.example.busasnquest.data.repository.MissionWithState
import com.example.busasnquest.ui.theme.CardWhite
import com.example.busasnquest.ui.theme.Coral
import com.example.busasnquest.ui.theme.CoralTint
import com.example.busasnquest.ui.theme.IconGreen
import com.example.busasnquest.ui.theme.PointRed
import com.example.busasnquest.ui.theme.TextMain
import com.example.busasnquest.ui.theme.TextSub

/**
 * 미션 카드 (리스트형): 좌측 이미지 타일 + 제목/하트 + 위치 + 보상 + 상태별 버튼.
 * 종류별 탭 리스트에서 사용.
 */
@Composable
fun MissionCard(
    item: MissionWithState,
    onChallenge: () -> Unit,
    onClick: () -> Unit = {},
    onVerify: () -> Unit = {},
    onToggleSaved: () -> Unit = {}
) {
    val mission = item.mission

    Column(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardWhite)
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MissionCircleIcon(
                imageUrl = mission.imageUrl,
                size = 100.dp,
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        mission.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextMain,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = if (item.saved) Icons.Filled.Favorite
                        else Icons.Outlined.FavoriteBorder,
                        contentDescription = "찜하기",
                        tint = if (item.saved) Coral else TextSub,
                        modifier = Modifier
                            .size(22.dp)
                            .clickableNoRipple { onToggleSaved() }
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // 위치핀 + 지역
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = TextSub,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(mission.region, color = TextSub, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(6.dp))

                // 보상
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Star, contentDescription = null, tint = Coral,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "+${mission.reward}P", fontWeight = FontWeight.Bold,
                        color = Coral, fontSize = 14.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 상태별 버튼
        when (item.state) {
            MissionState.NOT_STARTED -> {
                Button(onClick = onChallenge, modifier = Modifier.fillMaxWidth()) {
                    Text("도전하기")
                }
            }
            MissionState.IN_PROGRESS -> {
                Button(onClick = onVerify, modifier = Modifier.fillMaxWidth()) {
                    Text("인증하기")
                }
                if (item.error != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(item.error, color = PointRed, fontSize = 12.sp)
                }
            }
            MissionState.VERIFYING -> {
                Button(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
                    Text("인증 확인 중...")
                }
            }
            MissionState.COMPLETED -> {
                Button(
                    onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = IconGreen)
                ) {
                    Text("✓ 완료")
                }
            }
        }
    }
}

/**
 * 미션 썸네일 타일.
 * 우선순위: 서버 이미지 URL(Coil) → 로컬 drawable(imageRes) → 플레이스홀더.
 */
@Composable
fun MissionCircleIcon(
    imageUrl: String? = null,
    imageRes: Int? = null,
    size: Dp = 52.dp,
    shape: Shape = CircleShape,
    modifier: Modifier = Modifier
) {
    when {
        // 1) 서버 이미지 (mission.imageUrl)
        imageUrl != null -> {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = modifier
                    .size(size)
                    .clip(shape)
            )
        }
        // 2) 로컬 리소스 이미지
        imageRes != null -> {
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = modifier
                    .size(size)
                    .clip(shape)
            )
        }
        // 3) 이미지 없음: 플레이스홀더
        else -> {
            Box(
                modifier = modifier
                    .size(size)
                    .clip(shape)
                    .background(CoralTint),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.ImageIcon,
                    contentDescription = null,
                    tint = Coral,
                    modifier = Modifier.size(size / 3)
                )
            }
        }
    }
}
