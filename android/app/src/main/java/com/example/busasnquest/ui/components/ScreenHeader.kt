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
import com.example.busasnquest.data.model.USER_POINT
import com.example.busasnquest.ui.theme.*

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
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {

            // 제목 (강조 단어가 있으면 색을 다르게)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    title,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = NavyMain
                )
                if (highlight != null) {
                    Text(
                        highlight,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = PointRed
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                PointPill()
                Spacer(modifier = Modifier.width(10.dp))
                BellButton()
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(subtitle, color = TextSub, fontSize = 13.sp)
    }
}

@Composable
fun PointPill() {
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
            tint = PointOrange,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(USER_POINT, fontWeight = FontWeight.Bold, color = NavyMain, fontSize = 14.sp)
    }
}

@Composable
fun BellButton() {
    Box(
        modifier = Modifier
            .size(42.dp)
            .background(CardWhite, CircleShape)
            .border(1.dp, DividerGray, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Outlined.Notifications, contentDescription = "알림", tint = NavyMain)
    }
}
