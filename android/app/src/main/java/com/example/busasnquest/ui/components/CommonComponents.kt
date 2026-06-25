package com.example.busasnquest.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

@Composable
fun SectionTitle(text: String) {
    Text(
        text,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = TextMain,
        modifier = Modifier.padding(horizontal = 20.dp)
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
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(NavyMain)
            .padding(24.dp)
    ) {
        Column {
            Text(label, color = Color.White.copy(0.75f), fontSize = 14.sp)

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                percentText,
                fontSize = 42.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(caption, color = Color.White.copy(0.65f), fontSize = 13.sp)

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
    val spectrum = listOf(
        Color(0xFFFF5A5A),
        Color(0xFFFF9800),
        Color(0xFFF4D03F),
        Color(0xFF8BC34A),
        Color(0xFF4FC3F7)
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
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(Brush.horizontalGradient(spectrum))
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
    Row(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardWhite)
            .padding(4.dp)
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(13.dp))
                    .background(if (selected) NavyMain else Color.Transparent)
                    .clickable { onSelect(index) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    color = if (selected) Color.White else TextSub,
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
            .clip(RoundedCornerShape(12.dp))
            .background(CardWhite)
            .border(1.dp, DividerGray, RoundedCornerShape(12.dp))
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
