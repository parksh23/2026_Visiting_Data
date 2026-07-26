package com.example.busasnquest.ui.mission

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.busasnquest.data.repository.DistrictMissionProgress
import com.example.busasnquest.ui.components.clickableNoRipple
import com.example.busasnquest.ui.theme.Dimens
import com.example.busasnquest.ui.theme.occupancyColor
import com.example.busasnquest.ui.theme.occupancyTextColor

/**
 * 부산 16개 구·군을 4×4 그리드로 배치 (지리 근사: 북서 → 남동).
 * 부산은 15개 구 + 1개 군 = 딱 16개라 4×4 가 성립한다.
 *
 * 강서   북구   금정   기장
 * 사상   부산진  동래   해운대
 * 사하   서구   연제   수영
 * 중구   동구   남구   영도
 */
val BusanDistrictLayout: List<List<String>> = listOf(
    listOf("강서구", "북구", "금정구", "기장군"),
    listOf("사상구", "부산진구", "동래구", "해운대구"),
    listOf("사하구", "서구", "연제구", "수영구"),
    listOf("중구", "동구", "남구", "영도구")
)

// 박스 라벨용 짧은 이름: 3글자 이상이면 뒤의 구/군을 뗀다 (해운대구→해운대, 중구→중구)
fun districtShortName(full: String): String =
    if (full.length >= 3) full.dropLast(1) else full

/**
 * 구·군 점령 히트맵 그리드.
 *
 * @param districts        구·군별 진행 현황 (없는 구는 0/0 취급)
 * @param inProgressSet    진행 중 미션이 있는 구 이름 집합 (박스 우상단 점 표시)
 * @param selected         현재 선택(바텀시트 열림)된 구 이름
 * @param onSelect         박스 탭 콜백
 */
@Composable
fun DistrictGrid(
    districts: List<DistrictMissionProgress>,
    inProgressSet: Set<String>,
    selected: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val byName = districts.associateBy { it.name }

    // 세그먼트 아래 ~ 하단탭 위까지 세로 공간을 4행이 균등하게 나눠 가진다.
    // 고정 높이가 아니라 weight 기반이라 어떤 기기에서도 빈공간 없이 꽉 찬다.
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.screenPadding),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        BusanDistrictLayout.forEach { rowNames ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowNames.forEach { name ->
                    val progress = byName[name]
                    DistrictBox(
                        name = name,
                        progress = progress,
                        hasInProgress = name in inProgressSet,
                        isSelected = selected == name,
                        onClick = { onSelect(name) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
            }
        }

        Text(
            "옅음 0% → 진함 100% · 미션을 완료할수록 진해져요",
            fontSize = 11.sp,
            color = com.example.busasnquest.ui.theme.TextSub,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(vertical = 2.dp)
        )
    }
}

@Composable
private fun DistrictBox(
    name: String,
    progress: DistrictMissionProgress?,
    hasInProgress: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val total = progress?.total ?: 0
    val completed = progress?.completed ?: 0
    val rate = if (total == 0) 0f else completed.toFloat() / total
    val percent = (rate * 100).toInt()
    val cleared = total > 0 && completed == total

    // 인증 성공으로 점령률이 오르면 색이 부드럽게 진해진다 (보상 연출)
    val bgColor by animateColorAsState(
        targetValue = occupancyColor(rate),
        animationSpec = tween(500),
        label = "occupancyColor"
    )
    val textColor = occupancyTextColor(rate)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .then(
                if (isSelected) Modifier.border(2.dp, Color(0xFF4A1B0C), RoundedCornerShape(16.dp))
                else Modifier
            )
            .clickableNoRipple { onClick() }
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                districtShortName(name),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Spacer(Modifier.height(2.dp))
            Text(
                if (total == 0) "-" else "$percent%",
                fontSize = 12.sp,
                color = textColor.copy(alpha = 0.85f)
            )
        }

        // 점령 완료(100%): 깃발 — 색 외의 보조 신호 (색약 대비)
        if (cleared) {
            Icon(
                imageVector = Icons.Filled.Flag,
                contentDescription = "점령 완료",
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 7.dp, end = 8.dp)
                    .size(15.dp)
            )
        } else if (hasInProgress) {
            // 진행 중 미션이 있는 구: 작은 점 (색은 성취, 점은 진행 중)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 9.dp, end = 10.dp)
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(if (rate > 0.5f) Color.White else Color(0xFFCE504D))
            )
        }
    }
}
