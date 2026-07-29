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
fun formatPoints(value: Int): String {
    return "%,d".format(value) + "P"
}
@Composable
fun ScreenHeader(
    title: String,
    highlight: String? = null,
    subtitle: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 18.dp)
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

        Spacer(modifier = Modifier.height(6.dp))

        // 부제 — 뮤트 그레이 + Medium
        Text(subtitle, color = TextSub, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun PointPill() {
    val points by UserRepository.points.collectAsStateWithLifecycle()
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(CardWhite)
            .border(1.dp, DividerGray, CircleShape)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Filled.Star,
            contentDescription = null,
            tint = Coral,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        // 숫자 — 액센트 폰트 (영문·숫자 전용)
        Text(formatPoints(points), style = accentStyle(15.sp), color = TextMain)
    }
}


