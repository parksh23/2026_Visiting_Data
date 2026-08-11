package com.example.busasnquest.data.repository

import com.example.busasnquest.data.remote.RetrofitInstance
import com.example.busasnquest.data.remote.UpdateNicknameRequestDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// object = 앱 전체에서 딱 하나만 존재하는 인스턴스 (모든 탭이 같은 걸 봄)
object UserRepository {

    // 초기값 0 — 서버(GET /api/v1/users/me)에서 받아오기 전까지는 아무 값도 지어내지 않는다.
    private val _points = MutableStateFlow(0)
    val points: StateFlow<Int> = _points.asStateFlow()

    // 서버에서 불러온 사용자 닉네임 (로그인한 실제 이름). 아직 못 불러왔으면 빈 문자열.
    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()

    // 포인트 적립 — 인증 직후 즉시 반영용(낙관적 갱신).
    // 서버가 최종 점수의 기준이므로, 곧이어 refreshProfile()로 덮어쓴다.
    fun addPoints(amount: Int) {
        _points.update { it + amount }
    }

    /** "2,450P" / "2450" 등 서버 표기에서 숫자만 뽑아 Int로 변환 */
    private fun String.toPointsInt(): Int =
        filter { it.isDigit() }.toIntOrNull() ?: 0

    // GET /api/v1/users/me → 프로필(닉네임 등)을 불러와 저장.
    // 실패해도 앱이 죽지 않고 기존 값을 유지한다.
    suspend fun refreshProfile() {
        try {
            val profile = RetrofitInstance.api.getMyProfile()
            _name.value = profile.name
            // 서버는 "2,450P" 형태의 문자열로 준다 → 숫자만 뽑아 반영
            _points.value = profile.points.toPointsInt()
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
}
