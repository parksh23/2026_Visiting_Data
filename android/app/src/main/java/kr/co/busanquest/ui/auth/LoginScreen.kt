package kr.co.busanquest.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import kr.co.busanquest.ui.theme.BgSoftBlue
import kr.co.busanquest.ui.theme.CardWhite
import kr.co.busanquest.ui.theme.DividerGray
import kr.co.busanquest.ui.theme.KakaoYellow
import kr.co.busanquest.ui.theme.NavyMain
import kr.co.busanquest.ui.theme.OnKakaoYellow
import kr.co.busanquest.ui.theme.PointRed
import kr.co.busanquest.ui.theme.TextSub
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.util.Log
import android.widget.Toast
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient

// 다크 단일 테마에 맞춘 로컬 색상
private val LoginBg = Color(0xFFF7F3EA)      // 화면 배경 (앱 공통 크림 페이퍼)
private val Indigo = Color(0xFF5B67D8)       // 포인트 (라이트 대비 위해 낮춤 · 흰 글자 4.8:1)
private val FieldBorder = Color(0xFFDED4C4)  // 입력창 테두리
private val LabelGray = Color(0xFF4A423C)    // 라벨
private val HintGray = Color(0xFFA79C92)     // 플레이스홀더
private val LoginCard = Color(0xFFFFFFFF)    // 로그인 카드 표면

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onOpenDocument: (String) -> Unit = {},
    viewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val findIdState by viewModel.findIdState.collectAsState()
    val findPasswordState by viewModel.findPasswordState.collectAsState()
    // 값이 있으면 "카카오 인증은 됐는데 우리 서비스엔 처음 오는 사람" — 약관 동의를 받아야 한다
    val kakaoAgreementToken by viewModel.kakaoAgreementToken.collectAsState()
    val context = LocalContext.current

    // 임시 비밀번호는 다이얼로그 안에서 직접 보여준다 (FindPasswordDialog 결과 화면).
    // 예전의 '메일 보냈어요' 토스트는 SMTP 차단 이후 사실과 달라 제거했다.

    // 약관 동의 상태 (회원가입 탭 전용)
    // 로그인 탭의 카카오 로그인은 동의를 미리 받지 않는다 — 이미 가입한 계정이 들어오는 자리라서.
    // 처음 오는 사람이면 서버가 400 을 주고, 아래 KakaoAgreementDialog 가 그 자리에서 동의를 받는다.
    val agreements = rememberAgreementState()

    // 위와 별개의 상태 — 로그인 탭에서 뜨는 카카오 약관 동의 시트 전용
    val kakaoAgreements = rememberAgreementState()

    // ⚠️ rememberSaveable — 약관 '보기'로 문서 화면에 다녀오면 이 화면의 컴포지션이
    //    사라졌다 다시 만들어진다. remember 로 두면 탭과 입력값이 전부 초기화된다.
    //    돌아왔을 때 처음부터 다시 입력하지 않도록 입력값을 모두 보존한다.
    var selectedTab by rememberSaveable { mutableStateOf(0) }   // 0=Log in, 1=Sign up
    var email by rememberSaveable { mutableStateOf("") }
    var nickname by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var passwordConfirm by rememberSaveable { mutableStateOf("") }
    var passwordConfirmVisible by rememberSaveable { mutableStateOf(false) }
    var infoMessage by rememberSaveable { mutableStateOf<String?>(null) }

    /**
     * 회원가입 방식. null 이면 아직 안 고른 상태(= 방식 선택 화면).
     * 이메일 가입은 입력칸이 4개라, 카카오로 가입할 사람에게까지 다 보여줄 필요가 없다.
     */
    var signupWithEmail by rememberSaveable { mutableStateOf(false) }

    // 이메일 입력 폼을 보여줄 조건 — 로그인 탭이거나, 회원가입에서 '메일'을 고른 경우
    val showEmailForm = selectedTab == 0 || signupWithEmail

    val isLoading = uiState is LoginUiState.Loading
    val errorMessage = (uiState as? LoginUiState.Error)?.message

    // 로그인 성공 시 메인으로 이동, 회원가입 -> 로그인
    LaunchedEffect(uiState) {
        when (uiState) {
            is LoginUiState.Success -> onLoginSuccess()
            is LoginUiState.SignupSuccess -> {
                selectedTab = 0                 // 로그인 탭으로 전환
                signupWithEmail = false         // 다음 가입은 다시 방식 선택부터
                password = ""
                passwordConfirm = ""
                nickname = ""
                infoMessage = "회원가입이 완료되었습니다. 로그인해주세요."
                viewModel.resetState()          // 상태 초기화 (Idle)
            }
            else -> {}
        }
    }

    // ⚠️ 스크롤 필수 — 회원가입 탭은 입력칸 4개 + 약관 박스까지 들어가 카드가 화면보다 길어진다.
    //    스크롤이 없으면 아래쪽(카카오 버튼·하단 안내)이 화면 밖으로 잘려 안 보인다.
    //    imePadding: 키보드가 올라와도 입력칸이 가리지 않게 한다.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LoginBg)
            .systemBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(LoginCard)
                .padding(horizontal = 24.dp, vertical = 28.dp)
        ) {
            // ── 탭 (Log in / Sign up) ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                AuthTab("로그인", selectedTab == 0) { selectedTab = 0 }
                Spacer(Modifier.width(28.dp))
                AuthTab("회원가입", selectedTab == 1) {
                    selectedTab = 1
                    signupWithEmail = false   // 회원가입 탭은 항상 방식 선택부터
                }
            }

            Spacer(Modifier.height(28.dp))

            // ── 이메일 입력 폼 (로그인 / 메일 회원가입에서만) ──
            if (showEmailForm) {
            // ── 이메일 ──
            FieldLabel("이메일")
            Spacer(Modifier.height(8.dp))
            AuthTextField(
                value = email,
                onValueChange = { email = it },
                hint = "이메일을 입력하세요",
                keyboardType = KeyboardType.Email
            )

            Spacer(Modifier.height(18.dp))
            // ── 닉네임 (회원가입 탭에서만 표시) ──
            if (selectedTab == 1) {
                FieldLabel("닉네임")
                Spacer(Modifier.height(8.dp))
                AuthTextField(
                    value = nickname,
                    onValueChange = { nickname = it },
                    hint = "닉네임을 입력하세요"
                )
                Spacer(Modifier.height(18.dp))
            }
            // ── 비밀번호 ──
            FieldLabel("비밀번호")
            Spacer(Modifier.height(8.dp))
            AuthTextField(
                value = password,
                onValueChange = { password = it },
                hint = "비밀번호를 입력하세요",
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
                FieldLabel("비밀번호 확인")
                Spacer(Modifier.height(8.dp))
                AuthTextField(
                    value = passwordConfirm,
                    onValueChange = { passwordConfirm = it },
                    hint = "비밀번호를 다시 입력하세요",
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

            }   // showEmailForm 끝

            // ── 아이디 / 비밀번호 찾기 (로그인 탭에서만 표시) ──
            if (selectedTab == 0) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.End),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "아이디 찾기",
                        color = Indigo,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable { viewModel.openFindId() }
                    )
                    Text("  |  ", color = HintGray, fontSize = 13.sp)
                    Text(
                        "비밀번호 찾기",
                        color = Indigo,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable { viewModel.openFindPassword() }
                    )
                }
            }
            // ── 회원가입 완료 안내 (로그인 탭에서만) ──
            if (infoMessage != null && selectedTab == 0) {
                Spacer(Modifier.height(10.dp))
                Text(infoMessage!!, color = Indigo, fontSize = 13.sp)
            }

            // ── 약관 동의 (회원가입 탭) ──
            // 방식(메일/카카오)과 무관하게 가입 전에 먼저 받는다.
            if (selectedTab == 1) {
                Spacer(Modifier.height(18.dp))
                AgreementSection(
                    state = agreements,
                    onOpenDocument = onOpenDocument,
                    accent = Indigo,
                    borderColor = FieldBorder,
                    labelColor = LabelGray,
                    subColor = HintGray
                )
            }

            // ── 에러 메시지 ──
            if (errorMessage != null) {
                Spacer(Modifier.height(10.dp))
                Text(errorMessage, color = Color(0xFFCC3B3B), fontSize = 13.sp)
            }

            Spacer(Modifier.height(18.dp))

            // ── 제출 버튼 (로그인 / 메일 회원가입) ──
            if (showEmailForm) {
            Button(
                onClick = {
                    if (selectedTab == 0) viewModel.login(email, password)
                    else viewModel.signup(
                        email, password, passwordConfirm, nickname,
                        agreements.toDtoList()
                    )
                },
                // 회원가입은 필수 약관에 모두 동의해야 누를 수 있다
                enabled = !isLoading && (selectedTab == 0 || agreements.allRequiredAgreed),
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
                        if (selectedTab == 0) "로그인" else "회원가입",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
            }   // 제출 버튼 끝

            Spacer(Modifier.height(22.dp))

            if (selectedTab == 0) {
                // ── 로그인 탭: 또는 구분선 + 카카오 로그인 ──
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = FieldBorder)
                    Text("  또는  ", color = HintGray, fontSize = 12.sp)
                    HorizontalDivider(modifier = Modifier.weight(1f), color = FieldBorder)
                }
                Spacer(Modifier.height(20.dp))
                // 로그인 경로라 동의를 미리 받지 않는다(기존 회원은 재동의를 강요당하면 안 된다).
                // 신규 사용자면 서버가 400 을 주고, KakaoAgreementDialog 로 이어진다.
                KakaoLoginButton(
                    enabled = !isLoading,
                    label = "카카오로 로그인",
                    onClick = {
                        startKakaoLogin(
                            context = context,
                            onToken = { accessToken -> viewModel.loginWithKakao(accessToken) },
                            onError = { msg -> viewModel.onKakaoError(msg) }
                        )
                    }
                )
            } else if (!signupWithEmail) {
                // ── 회원가입 탭 · 방식 선택 ──
                // 약관에 동의해야 두 방식 모두 열린다.
                val needsAgreement = !agreements.allRequiredAgreed

                Button(
                    onClick = { signupWithEmail = true },
                    enabled = !isLoading && !needsAgreement,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Indigo),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text(
                        "메일로 회원가입",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                Spacer(Modifier.height(12.dp))

                KakaoLoginButton(
                    enabled = !isLoading && !needsAgreement,
                    label = "카카오로 회원가입",
                    onClick = {
                        startKakaoLogin(
                            context = context,
                            onToken = { accessToken ->
                                viewModel.loginWithKakao(accessToken, agreements.toDtoList())
                            },
                            onError = { msg -> viewModel.onKakaoError(msg) }
                        )
                    }
                )

                if (needsAgreement) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "약관에 동의하면 회원가입을 진행할 수 있어요.",
                        color = HintGray,
                        fontSize = 12.sp,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            } else {
                // ── 회원가입 탭 · 메일 가입 중 → 방식 선택으로 되돌아가기 ──
                Text(
                    "다른 방법으로 가입하기",
                    color = Indigo,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .clickable { signupWithEmail = false }
                )
            }

            Spacer(Modifier.height(22.dp))

            // ── 하단 안내 ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text("계정이 없으신가요? ", color = HintGray, fontSize = 13.sp)
                Text(
                    "회원가입",
                    color = Indigo,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { selectedTab = 1 }
                )
            }
        }
    }

    // ── 신규 카카오 사용자 약관 동의 ──
    // 카카오 인증까지 끝난 상태라, 여기서 동의만 받으면 같은 토큰으로 바로 가입이 끝난다.
    if (kakaoAgreementToken != null) {
        KakaoAgreementDialog(
            state = kakaoAgreements,
            loading = isLoading,
            onOpenDocument = onOpenDocument,
            onDismiss = { viewModel.dismissKakaoAgreement() },
            onConfirm = { viewModel.submitKakaoAgreement(kakaoAgreements.toDtoList()) }
        )
    }

    // ── 아이디 찾기 ──
    if (findIdState.visible) {
        FindIdDialog(
            state = findIdState,
            onDismiss = { viewModel.dismissFindId() },
            onSubmit = { viewModel.submitFindId(it) }
        )
    }

    // ── 비밀번호 찾기 ──
    if (findPasswordState.visible) {
        FindPasswordDialog(
            state = findPasswordState,
            onDismiss = { viewModel.dismissFindPassword() },
            onSubmit = { viewModel.submitFindPassword(it) },
            onConfirmed = { confirmedEmail, tempPwd ->
                // 로그인 탭으로 돌아가 이메일/임시 비밀번호를 미리 채워준다.
                // 사용자는 [로그인] 버튼만 누르면 된다.
                selectedTab = 0
                email = confirmedEmail
                password = tempPwd
                viewModel.dismissFindPassword()
            }
        )
    }
}

/**
 * 신규 카카오 사용자 약관 동의 시트.
 *
 * 카카오 로그인 버튼을 눌렀는데 우리 서비스에는 처음인 경우에 뜬다.
 * 회원가입 탭으로 돌려보내면 카카오 인증을 처음부터 다시 해야 하므로,
 * 이미 받아 둔 액세스 토큰을 그대로 쓰고 동의만 추가로 받는다.
 */
@Composable
private fun KakaoAgreementDialog(
    state: AgreementState,
    loading: Boolean,
    onOpenDocument: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!loading) onDismiss() },
        containerColor = LoginCard,
        title = {
            Text(
                "처음 오셨네요!",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = LabelGray
            )
        },
        text = {
            Column {
                Text(
                    "가입을 마치려면 약관 동의가 필요해요.",
                    color = HintGray,
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(14.dp))
                AgreementSection(
                    state = state,
                    onOpenDocument = onOpenDocument,
                    accent = Indigo,
                    borderColor = FieldBorder,
                    labelColor = LabelGray,
                    subColor = HintGray
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !loading && state.allRequiredAgreed
            ) {
                Text(
                    if (loading) "처리 중..." else "동의하고 시작하기",
                    color = if (!loading && state.allRequiredAgreed) Indigo else HintGray,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !loading) {
                Text("취소", color = HintGray)
            }
        }
    )
}

/**
 * 아이디 찾기 다이얼로그.
 *
 * 입력 → 성공하면 같은 창이 마스킹된 이메일을 보여주는 결과 화면으로 바뀐다.
 */
@Composable
private fun FindIdDialog(
    state: AccountFindState,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var nickname by remember { mutableStateOf("") }
    val found = state.maskedEmail

    AlertDialog(
        onDismissRequest = { if (!state.loading) onDismiss() },
        containerColor = LoginCard,
        title = {
            Text(
                if (found != null) "가입된 이메일" else "아이디 찾기",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = LabelGray
            )
        },
        text = {
            Column {
                if (found != null) {
                    Text("이 닉네임으로 가입된 이메일이에요.", color = HintGray, fontSize = 13.sp)
                    Spacer(Modifier.height(10.dp))
                    Text(found, color = LabelGray, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "개인정보 보호를 위해 일부만 표시됩니다.",
                        color = HintGray,
                        fontSize = 12.sp
                    )
                } else {
                    Text("가입할 때 쓴 닉네임을 입력해주세요.", color = HintGray, fontSize = 13.sp)
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = nickname,
                        onValueChange = { nickname = it },
                        singleLine = true,
                        enabled = !state.loading,
                        label = { Text("닉네임") }
                    )
                    if (state.error != null) {
                        Spacer(Modifier.height(6.dp))
                        Text(state.error, color = PointRed, fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            if (found != null) {
                TextButton(onClick = onDismiss) {
                    Text("확인", color = Indigo, fontWeight = FontWeight.Bold)
                }
            } else {
                // 통신 중에는 비활성 — 연타로 중복 요청되지 않게 한다
                TextButton(onClick = { onSubmit(nickname) }, enabled = !state.loading) {
                    Text(
                        if (state.loading) "찾는 중..." else "찾기",
                        color = if (state.loading) HintGray else Indigo,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        dismissButton = {
            if (found == null) {
                TextButton(onClick = onDismiss, enabled = !state.loading) {
                    Text("취소", color = HintGray)
                }
            }
        }
    )
}

/**
 * 비밀번호 찾기 다이얼로그.
 *
 * 2단계 구조 — FindIdDialog 와 같은 패턴이다.
 *  1) state.tempPassword == null : 이메일 입력 화면
 *  2) state.tempPassword != null : 발급된 임시 비밀번호를 보여주는 결과 화면
 *
 * 서버 SMTP 포트 차단으로 메일 발송이 불가해, 응답으로 받은 임시 비밀번호를
 * 창을 닫지 않고 그 자리에서 강조 표시한다.
 */
@Composable
private fun FindPasswordDialog(
    state: AccountFindState,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit,
    onConfirmed: (email: String, tempPassword: String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    val issued = state.tempPassword          // null 이면 입력 화면, 아니면 결과 화면
    val context = LocalContext.current

    AlertDialog(
        // 결과 화면에서는 바깥을 눌러 실수로 닫지 못하게 막는다 (다시 볼 수 없는 값이라서)
        onDismissRequest = { if (!state.loading && issued == null) onDismiss() },
        containerColor = LoginCard,
        title = {
            Text(
                if (issued != null) "임시 비밀번호 발급 완료" else "비밀번호 찾기",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = LabelGray
            )
        },
        text = {
            Column {
                if (issued != null) {
                    // ───── 결과 화면 ─────
                    Text(
                        "아래 임시 비밀번호로 로그인해주세요.",
                        color = HintGray,
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Indigo.copy(alpha = 0.08f))
                            .padding(start = 14.dp, top = 6.dp, end = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            issued,
                            modifier = Modifier.weight(1f),
                            color = Indigo,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = {
                            val clipboard =
                                context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(
                                ClipData.newPlainText("임시 비밀번호", issued)
                            )
                            // Android 13(API 33) 부터는 시스템이 복사 알림을 직접 띄운다
                            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
                                Toast.makeText(context, "복사했어요", Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Icon(
                                Icons.Filled.ContentCopy,
                                contentDescription = "임시 비밀번호 복사",
                                tint = Indigo
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    Text(
                        "이 창을 닫으면 다시 볼 수 없어요.\n로그인한 뒤 계정 설정에서 꼭 변경해주세요.",
                        color = PointRed,
                        fontSize = 12.sp
                    )
                } else {
                    // ───── 입력 화면 ─────
                    Text(
                        "가입한 이메일을 입력하면 임시 비밀번호를 발급해드려요.",
                        color = HintGray,
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        singleLine = true,
                        enabled = !state.loading,
                        label = { Text("이메일") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )
                    if (state.error != null) {
                        Spacer(Modifier.height(6.dp))
                        Text(state.error, color = PointRed, fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            if (issued != null) {
                TextButton(onClick = { onConfirmed(email.trim(), issued) }) {
                    Text("확인 및 로그인하기", color = Indigo, fontWeight = FontWeight.Bold)
                }
            } else {
                // 통신 중에는 비활성 — 연타로 중복 요청되지 않게 한다
                TextButton(onClick = { onSubmit(email) }, enabled = !state.loading) {
                    Text(
                        if (state.loading) "발급 중..." else "임시 비밀번호 발급",
                        color = if (state.loading) HintGray else Indigo,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        dismissButton = {
            if (issued == null) {
                TextButton(onClick = onDismiss, enabled = !state.loading) {
                    Text("취소", color = HintGray)
                }
            }
        }
    )
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
private fun KakaoLoginButton(
    enabled: Boolean,
    label: String = "카카오로 로그인",
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = KakaoYellow,   // 카카오 브랜드 규정색 (임의 변경 금지)
            contentColor = OnKakaoYellow
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
    ) {
        Text(label, fontWeight = FontWeight.Bold, fontSize = 15.sp)
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
            error != null -> {
                // 실제 카카오 에러(KOE006=키해시 미등록 등)를 Logcat 에 남긴다
                Log.e("KAKAO_LOGIN", "계정 로그인 실패: ${error.message}", error)
                onError("카카오 로그인에 실패했습니다.")
            }
            token != null -> onToken(token.accessToken)
        }
    }

    if (UserApiClient.instance.isKakaoTalkLoginAvailable(context)) {
        UserApiClient.instance.loginWithKakaoTalk(context) { token, error ->
            if (error != null) {
                Log.e("KAKAO_LOGIN", "카카오톡 로그인 실패: ${error.message}", error)
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
