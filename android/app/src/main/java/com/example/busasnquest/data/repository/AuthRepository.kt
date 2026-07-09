package com.example.busasnquest.data.repository

import kotlinx.coroutines.delay
import com.example.busasnquest.data.remote.AuthApi
import com.example.busasnquest.data.remote.KakaoLoginRequestDto
import com.example.busasnquest.data.remote.LoginRequestDto
import com.example.busasnquest.data.remote.SignupRequestDto
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

    // 카카오 access token 을 서버로 보내 우리 서버 JWT 를 받는다
    suspend fun loginWithKakao(kakaoAccessToken: String): Result<String>

    // 이메일/비밀번호로 회원가입 후 JWT 를 받는다
    suspend fun signup(email: String, password: String): Result<String>
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

    override suspend fun loginWithKakao(kakaoAccessToken: String): Result<String> {
        delay(500)
        return Result.success("fake-kakao-token-${System.currentTimeMillis()}")
    }

    override suspend fun signup(email: String, password: String): Result<String> {
        delay(800)
        return Result.success("fake-signup-token-${System.currentTimeMillis()}")
    }
}

class RetrofitAuthRepository(
    private val api: AuthApi
) : AuthRepository {
    /**
     * 이메일/비밀번호 로그인.
     * 아직 백엔드에 이메일 로그인 엔드포인트가 없으므로 기존 동작(가짜 검증)을 유지한다.
     * 서버에 /api/v1/auth/login 이 준비되면 아래 주석 처리된 실제 호출로 교체하면 된다.
     */
    override suspend fun login(email: String, password: String): Result<String> {
        delay(800)
        return if (email.isNotBlank() && password.length >= 4) {
            Result.success("fake-token-${System.currentTimeMillis()}")
        } else {
            Result.failure(Exception("이메일 또는 비밀번호를 확인해주세요."))
        }
        // 백엔드 준비 시:
        // return try {
        //     val response = api.login(LoginRequestDto(email, password))
        //     Result.success(response.token)
        // } catch (e: HttpException) {
        //     Result.failure(Exception("이메일 또는 비밀번호가 올바르지 않습니다."))
        // } catch (e: IOException) {
        //     Result.failure(Exception("네트워크 연결을 확인해주세요."))
        // }
    }

    /**
     * 카카오 로그인: 카카오 access token 을 서버로 보내면
     * 서버가 카카오에 검증 후 우리 서버 JWT 를 돌려준다.
     */
    override suspend fun loginWithKakao(kakaoAccessToken: String): Result<String> {
        return try {
            val response = api.kakaoLogin(KakaoLoginRequestDto(kakaoAccessToken))
            Result.success(response.token)
        } catch (e: HttpException) {
            Result.failure(Exception("카카오 로그인에 실패했습니다. 다시 시도해주세요."))
        } catch (e: IOException) {
            Result.failure(Exception("네트워크 연결을 확인해주세요."))
        }
    }

    /**
     * 이메일 회원가입.
     * 아직 백엔드에 회원가입 엔드포인트가 없으므로 임시로 가짜 성공 토큰을 반환한다.
     * 서버에 /api/v1/auth/signup 이 준비되면 아래 주석 처리된 실제 호출로 교체하면 된다.
     */
    override suspend fun signup(email: String, password: String): Result<String> {
        delay(800)
        return Result.success("fake-signup-token-${System.currentTimeMillis()}")
        // 백엔드 준비 시:
        // return try {
        //     val response = api.signup(SignupRequestDto(email, password))
        //     Result.success(response.token)
        // } catch (e: HttpException) {
        //     Result.failure(Exception("이미 가입된 이메일이거나 입력이 올바르지 않습니다."))
        // } catch (e: IOException) {
        //     Result.failure(Exception("네트워크 연결을 확인해주세요."))
        // }
    }
}
