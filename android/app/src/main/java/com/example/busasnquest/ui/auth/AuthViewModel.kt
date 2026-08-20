package com.example.busasnquest.ui.auth

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.busasnquest.data.local.TokenStore
import com.example.busasnquest.data.remote.RetrofitInstance
import com.example.busasnquest.data.repository.AuthRepository
import com.example.busasnquest.data.repository.RetrofitAuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.example.busasnquest.data.local.AppDocuments
import com.example.busasnquest.data.remote.AgreementDto

// 로그인 진행 상태
sealed interface LoginUiState {
    object Idle : LoginUiState           // 입력 대기
    object Loading : LoginUiState        // 로그인 시도 중
    object Success : LoginUiState          // 로그인 성공 (메인으로 이동)
    object SignupSuccess : LoginUiState    // ← 추가: 회원가입 성공 (자동 로그인 X)
    data class Error(val message: String) : LoginUiState
}

/**
 * 아이디/비밀번호 찾기 다이얼로그 상태.
 *
 * @param maskedEmail 아이디 찾기 성공 시 서버가 준 마스킹 이메일. 채워지면 결과 화면으로 바뀐다.
 * @param sent        비밀번호 찾기 성공 여부. 화면은 이걸 보고 스낵바를 띄우고 다이얼로그를 닫는다.
 */
data class AccountFindState(
    val visible: Boolean = false,
    val loading: Boolean = false,
    val error: String? = null,
    val maskedEmail: String? = null,
    val sent: Boolean = false
)

class AuthViewModel(
    private val repository: AuthRepository,
    private val tokenStore: TokenStore
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun login(email: String, password: String) {
        _uiState.value = LoginUiState.Loading
        viewModelScope.launch {
            val result = repository.login(email.trim(), password)
            result
                .onSuccess { token ->
                    tokenStore.saveToken(token)   // 토큰 저장 → 자동 로그인 가능
                    _uiState.value = LoginUiState.Success
                }
                .onFailure { e ->
                    _uiState.value = LoginUiState.Error(e.message ?: "로그인에 실패했습니다.")
                }
        }
    }

    /**
     * 카카오 로그인.
     * LoginScreen 에서 카카오 SDK 로그인으로 받은 access token 을 넘겨받아
     * 백엔드로 보내고, 돌아온 우리 서버 JWT 를 저장한다.
     */
    fun loginWithKakao(
        kakaoAccessToken: String,
        agreements: List<AgreementDto> = emptyList()
    ) {
        _uiState.value = LoginUiState.Loading
        viewModelScope.launch {
            repository.loginWithKakao(kakaoAccessToken, agreements)
                .onSuccess { token ->
                    tokenStore.saveToken(token)
                    _uiState.value = LoginUiState.Success
                }
                .onFailure { e ->
                    _uiState.value = LoginUiState.Error(e.message ?: "카카오 로그인에 실패했습니다.")
                }
        }
    }

    /**
     * 이메일 회원가입.
     * 1) 클라이언트에서 이메일 형식/비밀번호 길이/비밀번호 일치를 먼저 검사하고
     * 2) 통과하면 repository.signup 을 호출한다.
     * 성공 시 토큰을 저장해 가입과 동시에 자동 로그인 처리한다.
     */
    fun signup(
        email: String,
        password: String,
        passwordConfirm: String,
        nickname: String,
        agreements: List<AgreementDto> = emptyList()
    ) {
        val trimmedEmail = email.trim()
        val trimmedNickname = nickname.trim()

        // 클라이언트 1차 유효성 검사
        val validationError = when {
            trimmedEmail.isBlank() || !trimmedEmail.contains("@") ->
                "올바른 이메일 형식을 입력해주세요."
            trimmedNickname.isBlank() ->
                "닉네임을 입력해주세요."
            password.length < 8 ->
                "비밀번호는 8자 이상이어야 합니다."
            password != passwordConfirm ->
                "비밀번호가 일치하지 않습니다."
            // 화면에서 버튼을 잠그지만, 우회 경로가 생겨도 가입되지 않도록 여기서도 막는다
            !agreements.filter { it.agreed }.map { it.doc }
                .containsAll(AppDocuments.requiredSlugs) ->
                "필수 약관에 모두 동의해주세요."
            else -> null
        }
        if (validationError != null) {
            _uiState.value = LoginUiState.Error(validationError)
            return
        }

        _uiState.value = LoginUiState.Loading
        viewModelScope.launch {
            repository.signup(trimmedEmail, password, trimmedNickname, agreements)
                .onSuccess {
                    // 자동 로그인하지 않고, 사용자가 직접 다시 로그인하도록 한다
                    _uiState.value = LoginUiState.SignupSuccess
                }
                .onFailure { e ->
                    _uiState.value = LoginUiState.Error(e.message ?: "회원가입에 실패했습니다.")
                }
        }
    }

    // 카카오 SDK 자체에서 로그인이 취소/실패했을 때 화면에 메시지를 표시
    fun onKakaoError(message: String) {
        _uiState.value = LoginUiState.Error(message)
    }

    // ───────── 아이디 찾기 ─────────
    private val _findIdState = MutableStateFlow(AccountFindState())
    val findIdState: StateFlow<AccountFindState> = _findIdState.asStateFlow()

    fun openFindId() { _findIdState.value = AccountFindState(visible = true) }
    fun dismissFindId() { _findIdState.value = AccountFindState(visible = false) }

    fun submitFindId(nickname: String) {
        val trimmed = nickname.trim()
        if (trimmed.isBlank()) {
            _findIdState.value = _findIdState.value.copy(error = "닉네임을 입력해주세요.")
            return
        }
        // 이미 요청 중이면 무시 — 버튼 연타로 중복 호출되지 않도록
        if (_findIdState.value.loading) return

        _findIdState.value = _findIdState.value.copy(loading = true, error = null)
        viewModelScope.launch {
            repository.findId(trimmed)
                .onSuccess { masked ->
                    _findIdState.value = _findIdState.value.copy(
                        loading = false,
                        maskedEmail = masked
                    )
                }
                .onFailure { e ->
                    _findIdState.value = _findIdState.value.copy(
                        loading = false,
                        error = e.message ?: "아이디를 찾지 못했습니다."
                    )
                }
        }
    }

    // ───────── 비밀번호 찾기 ─────────
    private val _findPasswordState = MutableStateFlow(AccountFindState())
    val findPasswordState: StateFlow<AccountFindState> = _findPasswordState.asStateFlow()

    fun openFindPassword() { _findPasswordState.value = AccountFindState(visible = true) }
    fun dismissFindPassword() { _findPasswordState.value = AccountFindState(visible = false) }

    /** 스낵바를 한 번 띄운 뒤 화면이 호출해 sent 플래그를 내린다 */
    fun consumeFindPasswordSent() {
        _findPasswordState.value = _findPasswordState.value.copy(sent = false)
    }

    fun submitFindPassword(email: String) {
        val trimmed = email.trim()
        if (trimmed.isBlank() || !trimmed.contains("@")) {
            _findPasswordState.value =
                _findPasswordState.value.copy(error = "올바른 이메일 형식을 입력해주세요.")
            return
        }
        if (_findPasswordState.value.loading) return

        _findPasswordState.value = _findPasswordState.value.copy(loading = true, error = null)
        viewModelScope.launch {
            repository.findPassword(trimmed)
                .onSuccess {
                    // 다이얼로그는 닫고, sent 로 화면에 스낵바를 요청한다.
                    // 서버 message 는 임시 비밀번호가 섞여 올 수 있어 쓰지 않는다 (Repository 주석 참고).
                    _findPasswordState.value = AccountFindState(visible = false, sent = true)
                }
                .onFailure { e ->
                    _findPasswordState.value = _findPasswordState.value.copy(
                        loading = false,
                        error = e.message ?: "임시 비밀번호를 발송하지 못했습니다."
                    )
                }
        }
    }

    // 에러 메시지를 닫거나 다시 입력할 때 상태 초기화
    fun resetState() {
        _uiState.value = LoginUiState.Idle
    }

    companion object {
        // Application Context 로 TokenStore 를 만들어 주입한다.
        // 카카오·이메일 로그인 모두 FastAPI 백엔드로 연동한다.
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as Application
                AuthViewModel(
                    repository = RetrofitAuthRepository(RetrofitInstance.authApi),
                    tokenStore = TokenStore(app)
                )
            }
        }
    }
}
