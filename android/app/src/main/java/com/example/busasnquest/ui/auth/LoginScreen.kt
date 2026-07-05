package com.example.busasnquest.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import android.content.Context
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient

// 사진의 색감에 맞춘 로컬 색상
private val LoginBg = Color(0xFFE9EAF8)
private val Indigo = Color(0xFF6C7BE0)
private val FieldBorder = Color(0xFFE2E4EF)
private val LabelGray = Color(0xFF3A3F55)
private val HintGray = Color(0xFFB6BAC9)

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var selectedTab by remember { mutableStateOf(0) }       // 0=Log in, 1=Sign up
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var passwordConfirm by remember { mutableStateOf("") }
    var passwordConfirmVisible by remember { mutableStateOf(false) }

    val isLoading = uiState is LoginUiState.Loading
    val errorMessage = (uiState as? LoginUiState.Error)?.message

    // 로그인 성공 시 메인으로 이동
    LaunchedEffect(uiState) {
        if (uiState is LoginUiState.Success) onLoginSuccess()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LoginBg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(Color.White)
                .padding(horizontal = 24.dp, vertical = 28.dp)
        ) {
            // ── 탭 (Log in / Sign up) ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                AuthTab("Log in", selectedTab == 0) { selectedTab = 0 }
                Spacer(Modifier.width(28.dp))
                AuthTab("Sign up", selectedTab == 1) { selectedTab = 1 }
            }

            Spacer(Modifier.height(28.dp))

            // ── 이메일 ──
            FieldLabel("Your Email")
            Spacer(Modifier.height(8.dp))
            AuthTextField(
                value = email,
                onValueChange = { email = it },
                hint = "Enter your email",
                keyboardType = KeyboardType.Email
            )

            Spacer(Modifier.height(18.dp))

            // ── 비밀번호 ──
            FieldLabel("Password")
            Spacer(Modifier.height(8.dp))
            AuthTextField(
                value = password,
                onValueChange = { password = it },
                hint = "Enter your password",
                keyboardType = KeyboardType.Password,
                visualTransformation =
                    if (passwordVisible) VisualTransformation.None
                    else PasswordVisualTransformation(),
                trailingIcon = {
                    val icon = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                    Icon(
                        icon,
                        contentDescription = "비밀번호 표시 전환",
                        tint = HintGray,
                        modifier = Modifier.clickable { passwordVisible = !passwordVisible }
                    )
                }
            )

            // ── 비밀번호 확인 (회원가입 탭에서만 표시) ──
            if (selectedTab == 1) {
                Spacer(Modifier.height(18.dp))
                FieldLabel("Confirm Password")
                Spacer(Modifier.height(8.dp))
                AuthTextField(
                    value = passwordConfirm,
                    onValueChange = { passwordConfirm = it },
                    hint = "Re-enter your password",
                    keyboardType = KeyboardType.Password,
                    visualTransformation =
                        if (passwordConfirmVisible) VisualTransformation.None
                        else PasswordVisualTransformation(),
                    trailingIcon = {
                        val icon = if (passwordConfirmVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                        Icon(
                            icon,
                            contentDescription = "비밀번호 확인 표시 전환",
                            tint = HintGray,
                            modifier = Modifier.clickable { passwordConfirmVisible = !passwordConfirmVisible }
                        )
                    }
                )
            }

            // ── 비밀번호 찾기 (로그인 탭에서만 표시) ──
            if (selectedTab == 0) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "Forgot password?",
                    color = Indigo,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .align(Alignment.End)
                        .clickable { /* TODO: 비밀번호 찾기 (추후) */ }
                )
            }

            // ── 에러 메시지 ──
            if (errorMessage != null) {
                Spacer(Modifier.height(10.dp))
                Text(errorMessage, color = Color(0xFFE94F4F), fontSize = 13.sp)
            }

            Spacer(Modifier.height(18.dp))

            // ── Continue / Sign up 버튼 ──
            Button(
                onClick = {
                    if (selectedTab == 0) viewModel.login(email, password)
                    else viewModel.signup(email, password, passwordConfirm)
                },
                enabled = !isLoading,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Indigo),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(22.dp)
                    )
                } else {
                    Text(
                        if (selectedTab == 0) "Continue" else "Sign up",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }

            Spacer(Modifier.height(22.dp))

            // ── or 구분선 ──
            Row(verticalAlignment = Alignment.CenterVertically) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = FieldBorder)
                Text("  or  ", color = HintGray, fontSize = 12.sp)
                HorizontalDivider(modifier = Modifier.weight(1f), color = FieldBorder)
            }

            Spacer(Modifier.height(20.dp))

            // ── 카카오 로그인 버튼 ──
            KakaoLoginButton(
                enabled = !isLoading,
                onClick = {
                    startKakaoLogin(
                        context = context,
                        onToken = { accessToken -> viewModel.loginWithKakao(accessToken) },
                        onError = { msg -> viewModel.onKakaoError(msg) }
                    )
                }
            )

            Spacer(Modifier.height(22.dp))

            // ── 하단 안내 ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text("Don't have an account? ", color = HintGray, fontSize = 13.sp)
                Text(
                    "Sign up",
                    color = Indigo,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { selectedTab = 1 }
                )
            }
        }
    }
}

@Composable
private fun AuthTab(label: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            label,
            color = if (selected) Indigo else HintGray,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 16.sp
        )
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .height(2.dp)
                .width(40.dp)
                .background(if (selected) Indigo else Color.Transparent)
        )
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text, color = LabelGray, fontSize = 13.sp, fontWeight = FontWeight.Medium)
}

@Composable
private fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(hint, color = HintGray, fontSize = 14.sp) },
        singleLine = true,
        visualTransformation = visualTransformation,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        trailingIcon = trailingIcon,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Indigo,
            unfocusedBorderColor = FieldBorder
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun KakaoLoginButton(enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFFEE500),   // 카카오 브랜드 노란색
            contentColor = Color(0xFF191919)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
    ) {
        Text("카카오로 로그인", fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}

/**
 * 카카오 SDK 로그인을 실행한다.
 * - 카카오톡 앱이 설치돼 있으면 앱으로 로그인, 없으면 카카오 계정(웹)으로 로그인
 * - 성공 시 access token 을 onToken 으로, 실패/취소 시 메시지를 onError 로 전달
 */
private fun startKakaoLogin(
    context: Context,
    onToken: (String) -> Unit,
    onError: (String) -> Unit
) {
    // 카카오 계정(웹) 로그인 콜백
    val accountCallback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
        when {
            error != null -> onError("카카오 로그인에 실패했습니다.")
            token != null -> onToken(token.accessToken)
        }
    }

    if (UserApiClient.instance.isKakaoTalkLoginAvailable(context)) {
        UserApiClient.instance.loginWithKakaoTalk(context) { token, error ->
            if (error != null) {
                // 사용자가 직접 취소한 경우엔 계정 로그인으로 넘어가지 않는다
                if (error is ClientError && error.reason == ClientErrorCause.Cancelled) {
                    onError("로그인이 취소되었습니다.")
                    return@loginWithKakaoTalk
                }
                // 그 외 오류면 카카오 계정 로그인으로 폴백
                UserApiClient.instance.loginWithKakaoAccount(context, callback = accountCallback)
            } else if (token != null) {
                onToken(token.accessToken)
            }
        }
    } else {
        UserApiClient.instance.loginWithKakaoAccount(context, callback = accountCallback)
    }
}
