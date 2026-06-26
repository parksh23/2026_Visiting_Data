package com.example.busasnquest.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.lifecycle.viewmodel.compose.viewModel

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

    var selectedTab by remember { mutableStateOf(0) }       // 0=Log in, 1=Sign up
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

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

            // ── 에러 메시지 ──
            if (errorMessage != null) {
                Spacer(Modifier.height(10.dp))
                Text(errorMessage, color = Color(0xFFE94F4F), fontSize = 13.sp)
            }

            Spacer(Modifier.height(18.dp))

            // ── Continue 버튼 ──
            Button(
                onClick = { viewModel.login(email, password) },
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

            // ── 소셜 버튼 (이번엔 UI만, 동작 없음) ──
            SocialButton("Login with Apple", Color(0xFF111111))
            Spacer(Modifier.height(12.dp))
            SocialButton("Login with Google", Color(0xFF4285F4))

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
private fun SocialButton(label: String, markColor: Color) {
    OutlinedButton(
        onClick = { /* TODO: 소셜 로그인 (추후 연결) */ },
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, FieldBorder),
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(markColor)
        )
        Spacer(Modifier.width(10.dp))
        Text(label, color = Color(0xFF333333), fontWeight = FontWeight.Medium, fontSize = 14.sp)
    }
}
