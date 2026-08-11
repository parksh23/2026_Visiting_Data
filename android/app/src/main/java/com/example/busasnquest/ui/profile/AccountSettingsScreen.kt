package com.example.busasnquest.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.busasnquest.ui.theme.*

/**
 * 계정 설정.
 *
 * 지금 동작하는 것: 닉네임 변경(PATCH /users/me/nickname), 로그아웃.
 * 서버 작업이 필요한 항목(이메일·로그인 수단·비밀번호 변경·회원 탈퇴)은
 * "준비 중" 배지로 표시하고, 눌렀을 때 왜 안 되는지 안내한다.
 */
@Composable
fun AccountSettingsScreen(
    navController: NavHostController,
    onLogout: () -> Unit = {},
    viewModel: ProfileViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val editState by viewModel.editState.collectAsStateWithLifecycle()

    var pendingMessage by remember { mutableStateOf<String?>(null) }
    var confirmLogout by remember { mutableStateOf(false) }

    if (editState.visible) {
        NicknameEditDialog(
            currentName = uiState.name,
            state = editState,
            onDismiss = { viewModel.dismissNicknameEditor() },
            onConfirm = { viewModel.submitNickname(it) }
        )
    }

    pendingMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { pendingMessage = null },
            title = { Text("아직 준비 중이에요", color = TextMain, fontWeight = FontWeight.Bold) },
            text = { Text(message, color = TextSub, fontSize = 14.sp) },
            confirmButton = {
                TextButton(onClick = { pendingMessage = null }) {
                    Text("확인", color = CoralDark, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = CardWhite
        )
    }

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text("로그아웃할까요?", color = TextMain, fontWeight = FontWeight.Bold) },
            text = { Text("다시 로그인하면 기록은 그대로 남아 있어요.", color = TextSub, fontSize = 14.sp) },
            confirmButton = {
                TextButton(onClick = { confirmLogout = false; onLogout() }) {
                    Text("로그아웃", color = CoralDark, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmLogout = false }) { Text("취소", color = TextSub) }
            },
            containerColor = CardWhite
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgSoftBlue)
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = Dimens.bottomBarClearance)
    ) {
        SubPageHeader("계정 설정", navController)
        Spacer(Modifier.height(8.dp))

        SettingsCard("프로필") {
            ValueRow(
                title = "닉네임",
                value = uiState.name.ifBlank { "불러오는 중..." },
                onClick = { viewModel.openNicknameEditor() }
            )
        }

        Spacer(Modifier.height(20.dp))

        SettingsCard("계정") {
            ValueRow(
                title = "이메일",
                pending = true,
                onClick = { pendingMessage = "서버 프로필 응답에 이메일이 아직 포함되지 않아 표시할 수 없어요." }
            )
            SettingsDivider()
            ValueRow(
                title = "로그인 수단",
                pending = true,
                onClick = { pendingMessage = "이메일 로그인인지 카카오 로그인인지 서버에서 내려주면 표시할 수 있어요." }
            )
        }

        Spacer(Modifier.height(20.dp))

        SettingsCard("보안") {
            ValueRow(
                title = "비밀번호 변경",
                pending = true,
                onClick = { pendingMessage = "비밀번호 변경 API가 준비되면 사용할 수 있어요. 카카오로 가입한 계정은 비밀번호가 없어 표시되지 않습니다." }
            )
        }

        Spacer(Modifier.height(20.dp))

        SettingsCard("계정 관리") {
            ValueRow(title = "로그아웃", onClick = { confirmLogout = true })
            SettingsDivider()
            ValueRow(
                title = "회원 탈퇴",
                pending = true,
                onClick = { pendingMessage = "탈퇴 처리 API가 준비되면 사용할 수 있어요. 지금 삭제가 필요하면 문의하기로 요청해주세요." }
            )
        }

        Spacer(Modifier.height(16.dp))
        Text(
            "'준비 중' 항목은 서버 작업이 끝나면 바로 열려요.",
            fontSize = 12.sp,
            color = TextSub,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        Spacer(Modifier.height(24.dp))
    }
}
