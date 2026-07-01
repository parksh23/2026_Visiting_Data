package com.example.busasnquest.data.repository

import kotlinx.coroutines.delay
import com.example.busasnquest.data.remote.AuthApi
import com.example.busasnquest.data.remote.LoginRequestDto
import retrofit2.HttpException
import java.io.IOException

/**
 * 인증 데이터 계층의 추상화.
 * 화면/ViewModel 은 이 인터페이스만 알면 되고,
 * 서버가 생기면 RetrofitAuthRepository 로 갈아끼우기만 하면 된다.
 *
 * 성공 시 토큰 문자열을 담은 Result 를 돌려준다.
 */
interface AuthRepository {
    suspend fun login(email: String, password: String): Result<String>
}

/**
 * 서버가 아직 없으므로 사용하는 가짜 구현.
 * - 이메일이 비어있지 않고 비밀번호가 4자 이상이면 성공
 * - 그 외에는 실패
 * 네트워크 지연을 흉내내려고 delay 를 둔다.
 */
class FakeAuthRepository : AuthRepository {
    override suspend fun login(email: String, password: String): Result<String> {
        delay(800) // 서버 응답 기다리는 느낌

        return if (email.isNotBlank() && password.length >= 4) {
            Result.success("fake-token-${System.currentTimeMillis()}")
        } else {
            Result.failure(Exception("이메일 또는 비밀번호를 확인해주세요."))
        }
    }
}

class RetrofitAuthRepository(
    private val api: AuthApi
) : AuthRepository {
    override suspend fun login(email: String, password: String): Result<String> {
        return try {
            val response = api.login(LoginRequestDto(email, password))
            Result.success(response.token)
        } catch (e: HttpException) {
            Result.failure(Exception("이메일 또는 비밀번호가 올바르지 않습니다."))
        } catch (e: IOException) {
            Result.failure(Exception("네트워크 연결을 확인해주세요."))
        }
    }
}
