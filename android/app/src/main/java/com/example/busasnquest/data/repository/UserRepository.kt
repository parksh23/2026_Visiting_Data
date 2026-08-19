package com.example.busasnquest.data.repository

import com.example.busasnquest.data.remote.ChangePasswordRequestDto
import com.example.busasnquest.data.remote.RetrofitInstance
import com.example.busasnquest.data.remote.UpdateNicknameRequestDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONObject
import retrofit2.HttpException

// object = 앱 전체에서 딱 하나만 존재하는 인스턴스 (모든 탭이 같은 걸 봄)
object UserRepository {

    // 초기값 0 — 서버(GET /api/v1/users/me)에서 받아오기 전까지는 아무 값도 지어내지 않는다.
    private val _points = MutableStateFlow(0)
    val points: StateFlow<Int> = _points.asStateFlow()

    // 서버에서 불러온 사용자 닉네임 (로그인한 실제 이름). 아직 못 불러왔으면 빈 문자열.
    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()

    // 마이페이지 통계 — 서버(users/me) 기준. 아직 못 불러왔으면 null.
    private val _completedCount = MutableStateFlow<Int?>(null)
    val completedCount: StateFlow<Int?> = _completedCount.asStateFlow()

    private val _savedCount = MutableStateFlow<Int?>(null)
    val savedCount: StateFlow<Int?> = _savedCount.asStateFlow()

    // 포인트 적립 — 인증 직후 즉시 반영용(낙관적 갱신).
    // 서버가 최종 점수의 기준이므로, 곧이어 refreshProfile()로 덮어쓴다.
    fun addPoints(amount: Int) {
        _points.update { it + amount }
    }

    /** "2,450P" / "2450" 등 서버 표기에서 숫자만 뽑아 Int로 변환 */
    private fun String.toPointsInt(): Int =
        filter { it.isDigit() }.toIntOrNull() ?: 0

    // GET /api/v1/users/me → 프로필(닉네임, 포인트, 완료/찜 개수)을 불러와 저장.
    // 실패해도 앱이 죽지 않고 기존 값을 유지한다.
    suspend fun refreshProfile() {
        try {
            val profile = RetrofitInstance.api.getMyProfile()
            _name.value = profile.name
            // 서버는 "2,450P" 형태의 문자열로 준다 → 숫자만 뽑아 반영
            _points.value = profile.points.toPointsInt()
            _completedCount.value = profile.completedMissions
            _savedCount.value = profile.savedMissions
        } catch (_: Exception) {
            // 네트워크/서버 오류 시 기존 값 유지 (화면은 기본값으로 폴백)
        }
    }

    // PATCH /api/v1/users/me/nickname → 닉네임 변경.
    // 중복 닉네임이면 서버가 409 를 주고, 그때 사용자에게 안내 메시지를 돌려준다.
    suspend fun updateNickname(newName: String): Result<Unit> {
        return try {
            val profile = RetrofitInstance.api.updateNickname(
                UpdateNicknameRequestDto(newName.trim())
            )
            _name.value = profile.name
            Result.success(Unit)
        } catch (e: retrofit2.HttpException) {
            if (e.code() == 409) {
                Result.failure(Exception("이미 사용 중인 닉네임이에요. 다른 닉네임을 입력해주세요."))
            } else {
                Result.failure(Exception("닉네임 변경에 실패했습니다. (${e.code()})"))
            }
        } catch (e: java.io.IOException) {
            Result.failure(Exception("네트워크 연결을 확인해주세요."))
        } catch (e: Exception) {
            Result.failure(Exception("닉네임 변경 중 오류가 발생했습니다."))
        }
    }

    // FastAPI 오류 본문({"detail": "..."})에서 사유를 뽑는다. 실패하면 fallback.
    private fun HttpException.detailOr(fallback: String): String = try {
        val body = response()?.errorBody()?.string()
        if (body.isNullOrBlank()) fallback else JSONObject(body).optString("detail", fallback)
    } catch (e: Exception) {
        fallback
    }

    /**
     * 비밀번호 변경 (PATCH /api/v1/users/me/password).
     *
     * 400 이면 서버가 준 사유(현재 비밀번호 불일치 / 길이 미달)를 그대로 보여준다.
     * 401 은 인터셉터가 토큰 삭제 + 로그인 화면 이동을 이미 처리한다.
     */
    suspend fun changePassword(oldPassword: String, newPassword: String): Result<Unit> {
        return try {
            RetrofitInstance.api.changePassword(
                ChangePasswordRequestDto(
                    oldPassword = oldPassword,
                    newPassword = newPassword
                )
            )
            Result.success(Unit)
        } catch (e: HttpException) {
            val message = when (e.code()) {
                400 -> e.detailOr("현재 비밀번호가 일치하지 않습니다.")
                401 -> "로그인이 만료되었어요. 다시 로그인해주세요."
                500 -> "잠시 후 다시 시도해주세요."
                else -> "비밀번호 변경에 실패했습니다. (${e.code()})"
            }
            Result.failure(Exception(message))
        } catch (e: java.io.IOException) {
            Result.failure(Exception("네트워크 연결을 확인해주세요."))
        } catch (e: Exception) {
            Result.failure(Exception("비밀번호 변경 중 오류가 발생했습니다."))
        }
    }

    /**
     * 회원 탈퇴 (DELETE /api/v1/users/me).
     *
     * 서버는 소프트 삭제(ACCOUNT_STATUS = WITHDRAWN)를 하고, 이후 로그인은 403 으로 막힌다.
     * 성공하면 싱글턴에 남은 프로필 값을 비운다. 토큰 삭제·화면 이동은 호출한 쪽이 한다.
     */
    suspend fun withdraw(): Result<Unit> {
        return try {
            RetrofitInstance.api.withdraw()
            clear()
            Result.success(Unit)
        } catch (e: HttpException) {
            val message = when (e.code()) {
                401 -> "로그인이 만료되었어요. 다시 로그인해주세요."
                500 -> "잠시 후 다시 시도해주세요."
                else -> "회원 탈퇴에 실패했습니다. (${e.code()})"
            }
            Result.failure(Exception(message))
        } catch (e: java.io.IOException) {
            Result.failure(Exception("네트워크 연결을 확인해주세요."))
        } catch (e: Exception) {
            Result.failure(Exception("회원 탈퇴 중 오류가 발생했습니다."))
        }
    }

    /**
     * 로그아웃·탈퇴 시 호출.
     *
     * UserRepository 는 object(싱글턴)라 앱을 다시 켜지 않는 한 값이 남는다.
     * 비우지 않으면 다음 사용자가 로그인했을 때 이전 계정의 닉네임/포인트가 잠깐 보인다.
     */
    fun clear() {
        _points.value = 0
        _name.value = ""
        _completedCount.value = null
        _savedCount.value = null
    }
}
