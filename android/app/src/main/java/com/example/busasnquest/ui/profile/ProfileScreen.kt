package com.example.busasnquest.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.HorizontalDivider
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
import com.example.busasnquest.data.model.*
import com.example.busasnquest.ui.components.ScreenHeader
import com.example.busasnquest.ui.theme.*
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController


@Composable
fun ProfileScreen(
    navController: NavHostController,
    onLogout: () -> Unit = {},
    viewModel: ProfileViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        ScreenHeader(
            title = "내 정보",
            subtitle = "나의 활동과 정보를 확인하세요!"
        )

        ProfileSummaryCard(uiState = uiState)

        Spacer(modifier = Modifier.height(20.dp))

        // 메뉴 카드 (미션 내역 / 찜한 미션 / 사진 관리)
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(CardWhite)
        ) {
            profileMenuItems.forEachIndexed { index, item ->
                MenuRow(item) {
                    when (item.title) {
                        "미션 내역" -> navController.navigate("missionHistory")
                        "찜한 미션" -> navController.navigate("savedMission")
                    }
                }
                if (index != profileMenuItems.lastIndex) {
                    HorizontalDivider(
                        color = DividerGray,
                        modifier = Modifier.padding(horizontal = 18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 설정 리스트
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(CardWhite)
        ) {
            settingItems.forEachIndexed { index, item ->
                SettingRow(item)
                if (index != settingItems.lastIndex) {
                    HorizontalDivider(
                        color = DividerGray,
                        modifier = Modifier.padding(horizontal = 18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 로그아웃
        Box(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(CoralTint)
                .clickable { onLogout() }
                .padding(vertical = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("로그아웃", color = Coral, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(120.dp))
    }
}

@Composable
fun ProfileSummaryCard(uiState: ProfileUiState) {
    Column(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(CardWhite)
            .padding(24.dp)
    ) {

        Row(verticalAlignment = Alignment.CenterVertically) {

            // 아바타 자리표시자
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(SeaBlue, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Person,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.width(18.dp))

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(uiState.name, fontWeight = FontWeight.Bold, fontSize = 19.sp, color = TextMain)
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        Icons.Outlined.Edit,
                        contentDescription = "이름 편집",
                        tint = TextSub,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(uiState.intro, color = TextSub, fontSize = 13.sp)
                Text("부산의 매력을 찾아 미션에 도전해요!", color = TextSub, fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        HorizontalDivider(color = DividerGray)
        Spacer(modifier = Modifier.height(20.dp))

        // 통계 3개 (서페이스 카드)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ProfileStat("%,d".format(uiState.points) + "P", "보유 포인트", Modifier.weight(1f))
            ProfileStat(uiState.completedCount.toString(), "완료 미션", Modifier.weight(1f))
            ProfileStat(uiState.savedCount.toString(), "찜한 미션", Modifier.weight(1f))
        }
    }
}

@Composable
fun ProfileStat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceGray)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, color = TextMain, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, color = TextSub, fontSize = 12.sp)
    }
}

@Composable
fun MenuRow(item: MenuItem, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(item.bg),
            contentAlignment = Alignment.Center
        ) {
            Icon(item.icon, contentDescription = null, tint = item.tint, modifier = Modifier.size(22.dp))
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(item.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextMain)
            Spacer(modifier = Modifier.height(2.dp))
            Text(item.subtitle, color = TextSub, fontSize = 12.sp)
        }

        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSub)
    }
}

@Composable
fun SettingRow(item: SettingItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(item.icon, contentDescription = null, tint = SeaBlue, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(item.title, fontSize = 15.sp, color = TextMain, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSub)
    }
}