package kr.co.busanquest.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import kr.co.busanquest.ui.theme.*

/**
 * 계정 설정.
 *
 * 닉네임 변경 · 비밀번호 변경 · 로그아웃 · 회원 탈퇴.
 *
 * @param onSessionEnd 로컬 토큰을 지우고 로그인 화면으로 보낸다 (BusanQuestApp 이 넘겨줌).
 *   비밀번호 변경·로그아웃·탈퇴 세 경우 모두 이 콜백으로 끝난다.
 *
 * ⚠️ SessionManager.notifySessionExpired() 만 부르면 화면만 이동하고 토큰은 기기에 남는다.
 *    (토큰 삭제는 401 인터셉터가 하는 일이라 직접 호출 시에는 동작하지 않는다)
 *    그래서 토큰 삭제까지 함께 하는 onSessionEnd 로 처리한다.
 */
@Composable
fun AccountSettingsScreen(
    navController: NavHostController,
    onSessionEnd: () -> Unit = {},
    viewModel: ProfileViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val editState by viewModel.editState.collectAsStateWithLifecycle()
    val passwordState by viewModel.passwordState.collectAsStateWithLifecycle()
    val passwordChanged by viewModel.passwordChanged.collectAsStateWithLifecycle()
    val withdrawState by viewModel.withdrawState.collectAsStateWithLifecycle()
    val logoutVisible by viewModel.logoutVisible.collectAsStateWithLifecycle()
    val logoutLoading by viewModel.logoutLoading.collectAsStateWithLifecycle()

    if (editState.visible) {
        NicknameEditDialog(
            currentName = uiState.name,
            state = editState,
            onDismiss = { viewModel.dismissNicknameEditor() },
            onConfirm = { viewModel.submitNickname(it) }
        )
    }

    if (passwordState.visible) {
        PasswordEditDialog(
            state = passwordState,
            onDismiss = { viewModel.dismissPasswordEditor() },
            onConfirm = { old, new, confirm -> viewModel.submitPassword(old, new, confirm) }
        )
    }

    // 비밀번호 변경 성공 → 안내 후 강제 로그아웃 (규격서 8장)
    if (passwordChanged) {
        AlertDialog(
            onDismissRequest = { },   // 반드시 확인을 눌러 로그아웃되도록 밖으로 못 닫는다
            title = { Text("비밀번호가 변경됐어요", color = TextMain, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "보안을 위해 다시 로그인해주세요.",
                    color = TextSub,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.consumePasswordChanged()
                    onSessionEnd()
                }) {
                    Text("확인", color = CoralDark, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = CardWhite
        )
    }

    if (logoutVisible) {
        AlertDialog(
            onDismissRequest = { if (!logoutLoading) viewModel.dismissLogoutDialog() },
            title = { Text("로그아웃 하시겠습니까?", color = TextMain, fontWeight = FontWeight.Bold) },
            text = { Text("다시 로그인하면 기록은 그대로 남아 있어요.", color = TextSub, fontSize = 14.sp) },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.submitLogout(onDone = onSessionEnd) },
                    enabled = !logoutLoading
                ) {
                    Text(
                        if (logoutLoading) "처리 중..." else "로그아웃",
                        color = if (logoutLoading) TextSub else CoralDark,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.dismissLogoutDialog() },
                    enabled = !logoutLoading
                ) { Text("취소", color = TextSub) }
            },
            containerColor = CardWhite
        )
    }

    if (withdrawState.visible) {
        WithdrawDialog(
            state = withdrawState,
            onDismiss = { viewModel.dismissWithdrawDialog() },
            onConfirm = { viewModel.submitWithdraw(onWithdrawn = onSessionEnd) }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgSoftBlue)
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = bottomBarSpacing())
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

        SettingsCard("보안") {
            ValueRow(
                title = "비밀번호 변경",
                onClick = { viewModel.openPasswordEditor() }
            )
        }

        Spacer(Modifier.height(20.dp))

        SettingsCard("계정 관리") {
            ValueRow(title = "로그아웃", onClick = { viewModel.openLogoutDialog() })
            SettingsDivider()
            ValueRow(title = "회원 탈퇴", onClick = { viewModel.openWithdrawDialog() })
        }

        Spacer(Modifier.height(24.dp))
    }
}

/** 비밀번호 변경 다이얼로그 — 현재 / 새 / 새 비밀번호 확인 */
@Composable
private fun PasswordEditDialog(
    state: PasswordEditState,
    onDismiss: () -> Unit,
    onConfirm: (old: String, new: String, confirm: String) -> Unit
) {
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!state.loading) onDismiss() },
        title = { Text("비밀번호 변경", color = TextMain, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = oldPassword,
                    onValueChange = { oldPassword = it },
                    singleLine = true,
                    enabled = !state.loading,
                    label = { Text("현재 비밀번호") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    singleLine = true,
                    enabled = !state.loading,
                    label = { Text("새 비밀번호") },
                    supportingText = { Text("8자 이상, 공백 없이") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    singleLine = true,
                    enabled = !state.loading,
                    label = { Text("새 비밀번호 확인") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )
                if (state.error != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(state.error, color = PointRed, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(oldPassword, newPassword, confirmPassword) },
                enabled = !state.loading
            ) {
                Text(
                    if (state.loading) "변경 중..." else "변경",
                    color = if (state.loading) TextSub else CoralDark,
                    fontWeight = FontWeight.Bold
                )
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

/** 회원 탈퇴 확인 다이얼로그 — 되돌릴 수 없으므로 결과를 먼저 강하게 알린다 */
@Composable
private fun WithdrawDialog(
    state: WithdrawState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!state.loading) onDismiss() },
        title = { Text("정말 탈퇴하시겠습니까?", color = TextMain, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    "모든 정보가 삭제되며 복구할 수 없습니다.",
                    color = PointRed,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "· 완료한 미션과 모은 포인트가 사라집니다\n" +
                        "· 랭킹에서 제외됩니다\n" +
                        "· 같은 계정으로 다시 로그인할 수 없습니다",
                    color = TextSub,
                    fontSize = 13.sp
                )
                if (state.error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(state.error, color = PointRed, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !state.loading) {
                Text(
                    if (state.loading) "처리 중..." else "탈퇴하기",
                    color = if (state.loading) TextSub else PointRed,
                    fontWeight = FontWeight.Bold
                )
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
