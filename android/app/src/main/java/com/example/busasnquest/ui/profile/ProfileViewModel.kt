package com.example.busasnquest.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.busasnquest.data.model.MissionState
import com.example.busasnquest.data.repository.MissionRepository
import com.example.busasnquest.data.repository.MissionWithState
import com.example.busasnquest.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch

data class ProfileUiState(
    val name: String = "부산갈매기",
    val intro: String = "부산을 사랑하는 여행자",
    val points: Int = 0,
    val completedCount: Int = 0,
    val savedCount: Int = 0,
    val completedMissions: List<MissionWithState> = emptyList(),
    val savedMissions: List<MissionWithState> = emptyList()    // 찜한 미션
)

// 닉네임 편집 다이얼로그 상태
data class NicknameEditState(
    val visible: Boolean = false,
    val loading: Boolean = false,
    val error: String? = null
)

class ProfileViewModel : ViewModel() {

    init {
        // 화면 진입 시 서버에서 실제 닉네임을 불러온다
        viewModelScope.launch { UserRepository.refreshProfile() }
    }

    // ── 닉네임 편집 ──
    private val _editState = MutableStateFlow(NicknameEditState())
    val editState: StateFlow<NicknameEditState> = _editState.asStateFlow()

    fun openNicknameEditor() { _editState.value = NicknameEditState(visible = true) }
    fun dismissNicknameEditor() { _editState.value = NicknameEditState(visible = false) }

    /**
     * 닉네임 저장. 먼저 앱에서 형식(길이/공백/동일값)을 검사하고,
     * 통과하면 서버로 변경 요청한다. 서버가 중복(409)을 주면 에러 메시지를 표시한다.
     */
    fun submitNickname(newName: String) {
        val trimmed = newName.trim()
        val validation = when {
            trimmed.isBlank() -> "닉네임을 입력해주세요."
            trimmed.length < 2 -> "닉네임은 2자 이상이어야 합니다."
            trimmed.length > 12 -> "닉네임은 12자 이하로 입력해주세요."
            trimmed == UserRepository.name.value -> "기존 닉네임과 동일해요."
            else -> null
        }
        if (validation != null) {
            _editState.value = _editState.value.copy(error = validation)
            return
        }

        _editState.value = _editState.value.copy(loading = true, error = null)
        viewModelScope.launch {
            UserRepository.updateNickname(trimmed)
                .onSuccess { _editState.value = NicknameEditState(visible = false) }
                .onFailure { e ->
                    _editState.value = _editState.value.copy(
                        loading = false,
                        error = e.message ?: "닉네임 변경에 실패했습니다."
                    )
                }
        }
    }

    val uiState: StateFlow<ProfileUiState> =
        combine(
            UserRepository.points,
            UserRepository.name,
            MissionRepository.missions
        ) { points, name, missions ->
            val completed = missions.filter { it.state == MissionState.COMPLETED }
            val saved = missions.filter { it.saved }
            ProfileUiState(
                name = name.ifBlank { "부산갈매기" },   // 아직 못 불러왔으면 기본값
                points = points,
                completedCount = completed.size,
                completedMissions = completed,
                savedCount = saved.size,
                savedMissions = saved
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ProfileUiState()
        )
}