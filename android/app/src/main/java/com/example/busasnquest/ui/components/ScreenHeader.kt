package com.example.busasnquest.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.busasnquest.ui.theme.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.busasnquest.data.repository.UserRepository
import androidx.compose.runtime.getValue
/**
 * 상단 헤더: 좌측에 제목 + (선택) 강조 단어 + 부제,
 * 우측에 포인트 배지 + 알림 벨.
 */
@Composable
fun ScreenHeader(
    title: String,
    highlight: String? = null,
    subtitle: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.screenPadding, vertical = Dimens.gapBlock)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {

            // 제목 — 디스플레이 헤딩(가로 80% 압축 콘덴스드)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    title,
                    style = displayStyle(28.sp),
                    color = TextMain
                )
                if (highlight != null) {
                    Text(
                        highlight,
                        style = displayStyle(28.sp),
                        color = Coral
                    )
                }
            }

            PointPill()
        }

        // 제목과 부제는 한 덩어리로 읽혀야 하므로 gapTight
        Spacer(modifier = Modifier.height(Dimens.gapTight))

        // 부제 — 뮤트 그레이 + Medium
        Text(subtitle, color = TextSub, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

/**
 * 상단 우측 포인트 표시.
 * 홈 탭과 동일한 모양(P 뱃지 + 액센트 숫자)으로 통일 — 예전의 별 아이콘 + 흰 알약은 제거.
 */
@Composable
fun PointPill() {
    val points by UserRepository.points.collectAsStateWithLifecycle()
    PointAmount(value = points)
}


