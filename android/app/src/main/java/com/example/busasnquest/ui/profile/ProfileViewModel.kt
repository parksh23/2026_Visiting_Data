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

    // ── 찜한 미션 ──

    // 찜 목록 로딩/오류 상태
    private val _savedLoading = MutableStateFlow(false)
    val savedLoading: StateFlow<Boolean> = _savedLoading.asStateFlow()

    private val _savedError = MutableStateFlow<String?>(null)
    val savedError: StateFlow<String?> = _savedError.asStateFlow()

    // ⚠️ init 은 위 프로퍼티들보다 뒤에 있어야 한다 (초기화 순서 — 위에 두면 null 참조)
    init {
        // 화면 진입 시 서버에서 실제 닉네임/통계와 찜 목록을 불러온다
        viewModelScope.launch { UserRepository.refreshProfile() }
        refreshSavedMissions()
    }

    // 찜 요청 중인 미션 id (중복 클릭 방지)
    val savePending: StateFlow<Set<Int>> = MissionRepository.savedPending

    fun clearSavedError() {
        _savedError.value = null
    }

    // GET /api/v1/missions/saved
    fun refreshSavedMissions() {
        viewModelScope.launch {
            _savedLoading.value = true
            try {
                MissionRepository.refreshSavedMissionsFromServer()
                _savedError.value = null
            } catch (e: retrofit2.HttpException) {
                _savedError.value = when (e.code()) {
                    401 -> "로그인이 필요해요. 다시 로그인해주세요."
                    403 -> "이용이 제한된 계정이에요."
                    else -> "찜한 미션을 불러오지 못했어요. (${e.code()})"
                }
            } catch (e: java.io.IOException) {
                _savedError.value = "네트워크 연결을 확인해주세요."
            } catch (e: Exception) {
                _savedError.value = "찜한 미션을 불러오는 중 오류가 발생했어요."
            } finally {
                _savedLoading.value = false
            }
        }
    }

    // DELETE /api/v1/missions/{id}/saved → 성공하면 목록에서 즉시 제거된다
    fun unsaveMission(id: Int) {
        viewModelScope.launch {
            MissionRepository.setSavedOnServer(id, saved = false)
                .onSuccess { UserRepository.refreshProfile() }   // 찜 개수 재동기화
                .onFailure { e -> _savedError.value = e.message }
        }
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

    // 서버 통계 (users/me). 아직 못 불러왔으면 null → 로컬 계산값으로 폴백.
    private val serverCounts =
        combine(UserRepository.completedCount, UserRepository.savedCount) { c, s -> c to s }

    val uiState: StateFlow<ProfileUiState> =
        combine(
            UserRepository.points,
            UserRepository.name,
            MissionRepository.missions,
            MissionRepository.savedMissions,
            serverCounts
        ) { points, name, missions, savedMissions, counts ->
            val (serverCompleted, serverSaved) = counts
            val completed = missions.filter { it.state == MissionState.COMPLETED }
            ProfileUiState(
                name = name.ifBlank { "부산갈매기" },   // 아직 못 불러왔으면 기본값
                points = points,
                completedCount = serverCompleted ?: completed.size,
                completedMissions = completed,
                savedCount = serverSaved ?: savedMissions.size,
                savedMissions = savedMissions          // GET /api/v1/missions/saved (최근순)
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ProfileUiState()
        )
}