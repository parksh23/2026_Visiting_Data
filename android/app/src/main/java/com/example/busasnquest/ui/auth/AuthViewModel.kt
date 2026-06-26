package com.example.busasnquest.ui.auth

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.busasnquest.data.local.TokenStore
import com.example.busasnquest.data.repository.AuthRepository
import com.example.busasnquest.data.repository.FakeAuthRepository
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

    // 에러 메시지를 닫거나 다시 입력할 때 상태 초기화
    fun resetState() {
        _uiState.value = LoginUiState.Idle
    }

    companion object {
        // Application Context 로 TokenStore 를 만들어 주입한다.
        // 서버가 생기면 FakeAuthRepository() 자리만 실제 구현으로 교체.
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as Application
                AuthViewModel(
                    repository = FakeAuthRepository(),
                    tokenStore = TokenStore(app)
                )
            }
        }
    }
}
