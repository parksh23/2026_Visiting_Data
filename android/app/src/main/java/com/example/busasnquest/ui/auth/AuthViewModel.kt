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

// 로그인 진행 상태
sealed interface LoginUiState {
    object Idle : LoginUiState           // 입력 대기
    object Loading : LoginUiState        // 로그인 시도 중
    object Success : LoginUiState        // 로그인 성공 (메인으로 이동)
    data class Error(val message: String) : LoginUiState
}

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
    fun loginWithKakao(kakaoAccessToken: String) {
        _uiState.value = LoginUiState.Loading
        viewModelScope.launch {
            repository.loginWithKakao(kakaoAccessToken)
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
    fun signup(email: String, password: String, passwordConfirm: String) {
        val trimmedEmail = email.trim()

        // 클라이언트 1차 유효성 검사
        val validationError = when {
            trimmedEmail.isBlank() || !trimmedEmail.contains("@") ->
                "올바른 이메일 형식을 입력해주세요."
            password.length < 8 ->
                "비밀번호는 8자 이상이어야 합니다."
            password != passwordConfirm ->
                "비밀번호가 일치하지 않습니다."
            else -> null
        }
        if (validationError != null) {
            _uiState.value = LoginUiState.Error(validationError)
            return
        }

        _uiState.value = LoginUiState.Loading
        viewModelScope.launch {
            repository.signup(trimmedEmail, password)
                .onSuccess { token ->
                    tokenStore.saveToken(token)   // 가입과 동시에 자동 로그인
                    _uiState.value = LoginUiState.Success
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

    // 에러 메시지를 닫거나 다시 입력할 때 상태 초기화
    fun resetState() {
        _uiState.value = LoginUiState.Idle
    }

    companion object {
        // Application Context 로 TokenStore 를 만들어 주입한다.
        // 카카오 로그인은 백엔드(RetrofitInstance.authApi)로 연동한다.
        // 이메일 로그인은 아직 백엔드가 없어 RetrofitAuthRepository 내부에서 기존 방식(가짜)을 유지한다.
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
