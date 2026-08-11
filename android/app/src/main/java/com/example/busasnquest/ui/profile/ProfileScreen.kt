package com.example.busasnquest.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    val editState by viewModel.editState.collectAsStateWithLifecycle()

    // 닉네임 편집 다이얼로그
    if (editState.visible) {
        NicknameEditDialog(
            currentName = uiState.name,
            state = editState,
            onDismiss = { viewModel.dismissNicknameEditor() },
            onConfirm = { viewModel.submitNickname(it) }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = Dimens.bottomBarSpace)
    ) {
        ScreenHeader(
            title = "내 정보",
            subtitle = "나의 활동과 정보를 확인하세요!"
        )

        ProfileSummaryCard(uiState = uiState, onEditName = { viewModel.openNicknameEditor() })

        Spacer(modifier = Modifier.height(Dimens.sectionGap))

        // 메뉴 카드 (미션 내역 / 찜한 미션 / 사진 관리)
        Column(
            modifier = Modifier
                .padding(horizontal = Dimens.screenPadding)
                .fillMaxWidth()
                .clip(RoundedCornerShape(Dimens.radiusCard))
                .background(CardWhite)
                .border(Dimens.borderWidth, InkBorder, RoundedCornerShape(Dimens.radiusCard))
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
                        modifier = Modifier.padding(horizontal = Dimens.cardPadding)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(Dimens.gapBlock))

        // 설정 리스트
        Column(
            modifier = Modifier
                .padding(horizontal = Dimens.screenPadding)
                .fillMaxWidth()
                .clip(RoundedCornerShape(Dimens.radiusCard))
                .background(CardWhite)
                .border(Dimens.borderWidth, InkBorder, RoundedCornerShape(Dimens.radiusCard))
        ) {
            settingItems.forEachIndexed { index, item ->
                SettingRow(item)
                if (index != settingItems.lastIndex) {
                    HorizontalDivider(
                        color = DividerGray,
                        modifier = Modifier.padding(horizontal = Dimens.cardPadding)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(Dimens.gapBlock))

        // 로그아웃
        Box(
            modifier = Modifier
                .padding(horizontal = Dimens.screenPadding)
                .fillMaxWidth()
                .pressable(onClick = onLogout)
                .clip(RoundedCornerShape(Dimens.radiusCard))
                .background(CoralTint)
                .border(Dimens.borderWidth, InkBorder, RoundedCornerShape(Dimens.radiusCard))
                .padding(vertical = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            // 크림(CoralTint) 위 Coral 은 대비 2.9:1 로 못 읽는다 → 크림 전용 전경색 사용
            Text("로그아웃", color = OnCoralTint, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun ProfileSummaryCard(uiState: ProfileUiState, onEditName: () -> Unit = {}) {
    Column(
        modifier = Modifier
            .padding(horizontal = Dimens.screenPadding)
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.radiusHero))
            .background(CardWhite)
            .border(Dimens.borderWidth, InkBorder, RoundedCornerShape(Dimens.radiusHero))
            .padding(Dimens.cardPadding + 6.dp)
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
                    // 이름 — 디스플레이 헤딩
                    Text(uiState.name, style = displayStyle(21.sp), color = TextMain)
                    Spacer(modifier = Modifier.width(6.dp))
                    // 18dp 아이콘 단독 클릭은 터치 타깃이 작다 → 36dp 박스로 확대
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .pressable(scaleDown = 0.86f, onClick = onEditName),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.Edit,
                            contentDescription = "이름 편집",
                            tint = TextSub,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(uiState.intro, color = TextSub, fontSize = 13.sp)
                Text("부산의 매력을 찾아 미션에 도전해요!", color = TextSub, fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(Dimens.gapBlock))
        HorizontalDivider(color = DividerGray)
        Spacer(modifier = Modifier.height(Dimens.gapBlock))

        // 통계 3개 (서페이스 카드)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ProfileStat("%,d".format(uiState.points), "보유 포인트", Modifier.weight(1f), leadingPoint = true)
            ProfileStat(uiState.completedCount.toString(), "완료 미션", Modifier.weight(1f))
            ProfileStat(uiState.savedCount.toString(), "찜한 미션", Modifier.weight(1f))
        }
    }
}

@Composable
fun ProfileStat(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    leadingPoint: Boolean = false
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(Dimens.radiusButton))
            .background(SurfaceGray)
            .border(Dimens.borderWidth, InkBorder, RoundedCornerShape(Dimens.radiusButton))
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 통계 숫자 — 액센트 폰트 (포인트 통계는 공통 P 뱃지 표시)
        if (leadingPoint) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                com.example.busasnquest.ui.components.PointBadge(size = 18.dp, fontSize = 10.sp)
                Spacer(modifier = Modifier.width(5.dp))
                Text(value, color = TextMain, style = accentStyle(20.sp))
            }
        } else {
            Text(value, color = TextMain, style = accentStyle(20.sp))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, color = TextSub, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun MenuRow(item: MenuItem, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // 카드 안 행이라 scale 대신 배경 하이라이트로 반응 (구분선과 어긋나지 않게)
            .pressableRow(onClick = onClick)
            .padding(Dimens.cardPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(Dimens.radiusChip))
                .background(item.bg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = androidx.compose.ui.res.painterResource(id = item.icon),
                contentDescription = null,
                tint = item.tint,
                modifier = Modifier.size(22.dp)
            )
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
private fun NicknameEditDialog(
    currentName: String,
    state: NicknameEditState,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var input by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = { if (!state.loading) onDismiss() },
        title = { Text("닉네임 수정", color = TextMain, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    singleLine = true,
                    enabled = !state.loading,
                    label = { Text("새 닉네임") },
                    supportingText = { Text("2~12자, 다른 사용자와 겹칠 수 없어요") }
                )
                if (state.error != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(state.error, color = PointRed, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(input) }, enabled = !state.loading) {
                Text(if (state.loading) "확인 중..." else "저장", color = Coral, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !state.loading) {
                Text("취소", color = TextSub)
            }
        },
        containerColor = CardWhite
    )
}

@Composable
fun SettingRow(item: SettingItem, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressableRow(onClick = onClick)
            .padding(horizontal = Dimens.cardPadding, vertical = Dimens.gapBlock),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(item.icon, contentDescription = null, tint = SeaBlue, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(item.title, fontSize = 15.sp, color = TextMain, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSub)
    }
}