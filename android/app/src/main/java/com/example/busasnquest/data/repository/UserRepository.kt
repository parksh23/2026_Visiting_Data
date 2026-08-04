package com.example.busasnquest.data.repository

import com.example.busasnquest.data.remote.RetrofitInstance
import com.example.busasnquest.data.remote.UpdateNicknameRequestDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// object = 앱 전체에서 딱 하나만 존재하는 인스턴스 (모든 탭이 같은 걸 봄)
object UserRepository {

    private val _points = MutableStateFlow(2450)
    val points: StateFlow<Int> = _points.asStateFlow()

    // 서버에서 불러온 사용자 닉네임 (로그인한 실제 이름). 아직 못 불러왔으면 빈 문자열.
    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()

    // 포인트 적립
    fun addPoints(amount: Int) {
        _points.update { it + amount }
    }

    // GET /api/v1/users/me → 프로필(닉네임 등)을 불러와 저장.
    // 실패해도 앱이 죽지 않고 기존 값을 유지한다.
    suspend fun refreshProfile() {
        try {
            val profile = RetrofitInstance.api.getMyProfile()
            _name.value = profile.name
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
