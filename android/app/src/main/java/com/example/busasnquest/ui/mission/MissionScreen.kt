package com.example.busasnquest.ui.mission

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.busasnquest.data.model.DistrictProgress
import com.example.busasnquest.data.model.districtProgressList
import com.example.busasnquest.ui.components.FilterChipBox
import com.example.busasnquest.ui.components.ProgressCard
import com.example.busasnquest.ui.components.ScreenHeader
import com.example.busasnquest.ui.components.SegmentedToggle
import com.example.busasnquest.ui.theme.*

@Composable
fun MissionScreen(navController: NavHostController) {

    var selectedTab by remember { mutableIntStateOf(0) }

    LazyColumn {

        item {
            ScreenHeader(
                title = "미션",
                subtitle = "다양한 미션을 완료하고 포인트를 모아보세요!"
            )

            ProgressCard(
                label = "전체 진행률",
                percentText = "35%",
                caption = "5/16 구·군 점령",
                progress = 0.35f
            )

            Spacer(modifier = Modifier.height(20.dp))

            SegmentedToggle(
                options = listOf("전체", "지역"),
                selectedIndex = selectedTab,
                onSelect = { selectedTab = it }
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (selectedTab == 0) "전체 지역 진행 현황" else "지역별 미션",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMain
                )
                FilterChipBox("전체")
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        items(districtProgressList) { district ->
            DistrictProgressRow(district) {
                navController.navigate("map/${district.name}")
            }
        }

        item { Spacer(modifier = Modifier.height(120.dp)) }
    }
}

/** 구·군 한 줄: 이름 + 진행 바 + 퍼센트 + 개수 + 화살표. */
@Composable
fun DistrictProgressRow(
    district: DistrictProgress,
    onClick: () -> Unit
) {
    val percent = if (district.total == 0) 0f
    else district.completed.toFloat() / district.total
    val percentInt = (percent * 100).toInt()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(
            district.name,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = TextMain,
            modifier = Modifier.width(64.dp)
        )

        // 진행 바 + 퍼센트
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    "$percentInt%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (percentInt == 0) TextSub else district.color
                )
            }
            Spacer(modifier = Modifier.height(5.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(TrackGray)
            ) {
                if (percent > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(percent)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(district.color)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Text(
            "${district.completed}/${district.total}",
            color = TextSub,
            fontSize = 13.sp
        )

        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = TextSub,
            modifier = Modifier.size(20.dp)
        )
    }
}
