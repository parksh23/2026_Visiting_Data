package com.example.busasnquest.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.busasnquest.ui.components.ScreenHeader
import com.example.busasnquest.ui.theme.*

@Composable
fun MapScreen(region: String) {
    Column(modifier = Modifier.fillMaxSize()) {

        ScreenHeader(
            title = "지도",
            subtitle = if (region == "부산") "구·군을 선택해 상세 정보를 확인하세요"
            else "$region 상세 정보를 확인하세요"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.verticalGradient(listOf(Color(0xFFE8F0FF), Color(0xFFDDE7F5)))
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Map,
                    contentDescription = null,
                    tint = NavyMain,
                    modifier = Modifier.size(80.dp)
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    if (region == "부산") "부산 전체 지도" else "$region 지도 확대",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = NavyMain
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("여기에 부산 지도 이미지 삽입", color = TextSub)
            }
        }
    }
}
