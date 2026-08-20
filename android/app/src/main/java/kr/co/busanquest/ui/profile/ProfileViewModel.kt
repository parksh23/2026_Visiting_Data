package kr.co.busanquest.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kr.co.busanquest.data.model.MissionState
import kr.co.busanquest.data.repository.MissionRepository
import kr.co.busanquest.data.repository.MissionWithState
import kr.co.busanquest.data.remote.RetrofitInstance
import kr.co.busanquest.data.repository.RetrofitAuthRepository
import kr.co.busanquest.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch

data class ProfileUiState(
    val name: String = "",          // 서버(users/me)에서 받기 전엔 비워둔다
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

// 비밀번호 변경 다이얼로그 상태
data class PasswordEditState(
    val visible: Boolean = false,
    val loading: Boolean = false,
    val error: String? = null
)

// 회원 탈퇴 확인 다이얼로그 상태
data class WithdrawState(
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

    // ───────── 비밀번호 변경 ─────────
    private val _passwordState = MutableStateFlow(PasswordEditState())
    val passwordState: StateFlow<PasswordEditState> = _passwordState.asStateFlow()

    /** 변경 성공 → 화면이 안내를 띄운 뒤 강제 로그아웃시킨다 */
    private val _passwordChanged = MutableStateFlow(false)
    val passwordChanged: StateFlow<Boolean> = _passwordChanged.asStateFlow()

    fun openPasswordEditor() { _passwordState.value = PasswordEditState(visible = true) }
    fun dismissPasswordEditor() { _passwordState.value = PasswordEditState(visible = false) }

    /**
     * 비밀번호 변경.
     *
     * 서버 규칙(회원가입과 동일)인 8자 이상 / 72바이트 이하를 앱에서 먼저 검사한다.
     * 72바이트는 서버가 bcrypt 를 쓰기 때문의 제한이다 (한글 1자 = 3바이트).
     */
    fun submitPassword(oldPassword: String, newPassword: String, confirmPassword: String) {
        if (_passwordState.value.loading) return   // 연타 방지

        val validation = when {
            oldPassword.isBlank() -> "현재 비밀번호를 입력해주세요."
            newPassword.length < 8 -> "새 비밀번호는 8자 이상이어야 합니다."
            newPassword.toByteArray(Charsets.UTF_8).size > 72 ->
                "새 비밀번호가 너무 깁니다. (한글은 1자당 3바이트)"
            newPassword.any { it.isWhitespace() } -> "비밀번호에는 공백을 사용할 수 없습니다."
            newPassword == oldPassword -> "현재 비밀번호와 다른 비밀번호를 입력해주세요."
            newPassword != confirmPassword -> "새 비밀번호가 서로 일치하지 않습니다."
            else -> null
        }
        if (validation != null) {
            _passwordState.value = _passwordState.value.copy(error = validation)
            return
        }

        _passwordState.value = _passwordState.value.copy(loading = true, error = null)
        viewModelScope.launch {
            UserRepository.changePassword(oldPassword, newPassword)
                .onSuccess {
                    _passwordState.value = PasswordEditState(visible = false)
                    _passwordChanged.value = true
                }
                .onFailure { e ->
                    _passwordState.value = _passwordState.value.copy(
                        loading = false,
                        error = e.message ?: "비밀번호 변경에 실패했습니다."
                    )
                }
        }
    }

    fun consumePasswordChanged() { _passwordChanged.value = false }

    // ───────── 로그아웃 ─────────
    private val _logoutVisible = MutableStateFlow(false)
    val logoutVisible: StateFlow<Boolean> = _logoutVisible.asStateFlow()

    private val _logoutLoading = MutableStateFlow(false)
    val logoutLoading: StateFlow<Boolean> = _logoutLoading.asStateFlow()

    fun openLogoutDialog() { _logoutVisible.value = true }
    fun dismissLogoutDialog() { _logoutVisible.value = false }

    /**
     * 로그아웃.
     *
     * 서버에 통보는 하지만 JWT 는 stateless 라 서버가 토큰을 무효화하지 않는다.
     * 규격대로 **결과와 무관하게** 로컬 토큰을 지우고 첫 화면으로 보낸다.
     */
    fun submitLogout(onDone: () -> Unit) {
        if (_logoutLoading.value) return
        _logoutLoading.value = true
        viewModelScope.launch {
            RetrofitAuthRepository(RetrofitInstance.authApi).logout()   // 실패해도 무시
            UserRepository.clear()
            _logoutLoading.value = false
            _logoutVisible.value = false
            onDone()
        }
    }

    // ───────── 회원 탈퇴 ─────────
    private val _withdrawState = MutableStateFlow(WithdrawState())
    val withdrawState: StateFlow<WithdrawState> = _withdrawState.asStateFlow()

    fun openWithdrawDialog() { _withdrawState.value = WithdrawState(visible = true) }
    fun dismissWithdrawDialog() { _withdrawState.value = WithdrawState(visible = false) }

    /**
     * 회원 탈퇴.
     *
     * 로그아웃과 달리 **통신 성공 후에만** 토큰을 지운다.
     * 실패했는데 로그아웃시키면 사용자는 탈퇴된 줄 알지만 계정이 남는다.
     */
    fun submitWithdraw(onWithdrawn: () -> Unit) {
        if (_withdrawState.value.loading) return
        _withdrawState.value = _withdrawState.value.copy(loading = true, error = null)
        viewModelScope.launch {
            UserRepository.withdraw()
                .onSuccess {
                    _withdrawState.value = WithdrawState(visible = false)
                    onWithdrawn()
                }
                .onFailure { e ->
                    _withdrawState.value = _withdrawState.value.copy(
                        loading = false,
                        error = e.message ?: "회원 탈퇴에 실패했습니다."
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
                name = name,                            // 가짜 기본값을 넣으면 닉네임 편집창에 그대로 채워져 위험
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