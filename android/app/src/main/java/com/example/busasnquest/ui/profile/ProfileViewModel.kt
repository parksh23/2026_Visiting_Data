package com.example.busasnquest.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.busasnquest.data.remote.FakeBusanQuestApi
import com.example.busasnquest.data.remote.UserProfileDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val profile: UserProfileDto? = null,
    val isLoading: Boolean = false
)

class ProfileViewModel : ViewModel() {

    // 지금은 가짜 API. 서버 준비되면 RetrofitInstance.api 로 바꾸면 끝.
    private val api = FakeBusanQuestApi()

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val result = api.getMyProfile()      // 여기서 가짜 데이터 받아옴
                _uiState.update { it.copy(profile = result, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
                println("통신 실패: ${e.message}")
            }
        }
    }
}