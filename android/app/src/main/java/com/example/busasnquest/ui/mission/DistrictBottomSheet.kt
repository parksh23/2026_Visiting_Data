package com.example.busasnquest.ui.mission

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.busasnquest.data.model.MissionState
import com.example.busasnquest.data.model.MissionType
import com.example.busasnquest.data.repository.MissionWithState
import com.example.busasnquest.ui.theme.CardWhite
import com.example.busasnquest.ui.theme.Coral
import com.example.busasnquest.ui.theme.CoralTint
import com.example.busasnquest.ui.theme.Dimens
import com.example.busasnquest.ui.theme.IconGreen
import com.example.busasnquest.ui.theme.OnCoralTint
import com.example.busasnquest.ui.theme.SurfaceGray
import com.example.busasnquest.ui.theme.TextMain
import com.example.busasnquest.ui.theme.TextSub
import com.example.busasnquest.ui.theme.onFilled
import com.example.busasnquest.ui.theme.pressable

/**
 * 구·군 박스를 탭했을 때 올라오는 바텀시트.
 * 구 요약(점령률 배지) + 남은 미션 리스트. 화면 전환 없이 탐색 → 도전으로 이어진다.
 *
 * 시트 최대 높이는 화면의 55% — 뒤의 그리드(내 땅 현황)가 항상 보이게 유지.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DistrictBottomSheet(
    districtName: String,
    missions: List<MissionWithState>,
    onDismiss: () -> Unit,
    onMissionClick: (Int) -> Unit,
    onChallenge: (Int) -> Unit,
    onVerify: (Int, MissionType) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val maxHeight = (LocalConfiguration.current.screenHeightDp * 0.55f).dp

    val total = missions.size
    val completed = missions.count { it.state == MissionState.COMPLETED }
    val remaining = missions.filter { it.state != MissionState.COMPLETED }
    val percent = if (total == 0) 0 else (completed * 100 / total)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CardWhite
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(maxHeight)
                .padding(horizontal = Dimens.screenPadding)
        ) {
            // 헤더: 구 이름 + 점령률 배지
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    districtName,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMain,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(CoralTint)
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Text(
                        "점령률 $percent%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        // OccupancyTextDark(#EDEBE8)는 크림 배지 위에서 대비 1.1:1 로 사실상 안 보였다
                        color = OnCoralTint
                    )
                }
            }

            Spacer(Modifier.height(4.dp))
            Text(
                if (total == 0) "이 지역엔 아직 미션이 없어요."
                else "미션 ${total}개 중 ${completed}개 완료 · ${total - completed}개 남음",
                fontSize = 13.sp,
                color = TextSub
            )

            Spacer(Modifier.height(Dimens.gapBlock))

            // 남은 미션 리스트 (전부 완료면 완료 미션도 보여줌)
            val listToShow = if (remaining.isEmpty()) missions else remaining
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxHeight()
            ) {
                items(listToShow, key = { it.mission.id }) { item ->
                    SheetMissionRow(
                        item = item,
                        onClick = { onMissionClick(item.mission.id) },
                        onChallenge = { onChallenge(item.mission.id) },
                        onVerify = { onVerify(item.mission.id, item.mission.type) }
                    )
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun SheetMissionRow(
    item: MissionWithState,
    onClick: () -> Unit,
    onChallenge: () -> Unit,
    onVerify: () -> Unit
) {
    val mission = item.mission

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressable(onClick = onClick)
            .clip(RoundedCornerShape(Dimens.radiusChip))
            .background(SurfaceGray)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 타입 아이콘 타일
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(Dimens.radiusChip - 2.dp))
                .background(CoralTint),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when (mission.type) {
                    MissionType.IMAGE_LOCATION -> Icons.Filled.CameraAlt
                    MissionType.CURRENT_LOCATION -> Icons.Filled.LocationOn
                    MissionType.RECEIPT -> Icons.Filled.Receipt
                },
                contentDescription = null,
                tint = Coral,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(mission.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextMain)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(missionTypeLabel(mission.type), fontSize = 11.sp, color = TextSub)
                Spacer(Modifier.width(6.dp))
                // 보상 — 앱 공통 포인트 표시
                com.example.busasnquest.ui.components.PointAmount(
                    value = mission.reward,
                    prefix = "+",
                    badgeSize = 13.dp,
                    badgeFontSize = 7.sp,
                    fontSize = 11.sp,
                    gap = 3.dp
                )
            }
        }

        // 상태별 작은 버튼
        val (label, bg) = when (item.state) {
            MissionState.NOT_STARTED -> "도전" to Coral
            MissionState.IN_PROGRESS -> "인증" to Coral
            MissionState.VERIFYING -> "확인 중" to TextSub
            MissionState.COMPLETED -> "완료" to IconGreen
        }
        val actionable = item.state == MissionState.NOT_STARTED || item.state == MissionState.IN_PROGRESS
        Box(
            modifier = Modifier
                .pressable(enabled = actionable) {
                    when (item.state) {
                        MissionState.NOT_STARTED -> onChallenge()
                        MissionState.IN_PROGRESS -> onVerify()
                        else -> Unit
                    }
                }
                .clip(CircleShape)
                .background(bg)
                .padding(horizontal = 14.dp, vertical = 7.dp)
        ) {
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = onFilled(bg))
        }
    }
}

fun missionTypeLabel(type: MissionType): String = when (type) {
    MissionType.IMAGE_LOCATION -> "사진 인증"
    MissionType.CURRENT_LOCATION -> "위치 인증"
    MissionType.RECEIPT -> "영수증 인증"
}
