package com.example.busasnquest.data.repository

import kotlinx.coroutines.delay
import com.example.busasnquest.data.remote.AuthApi
import com.example.busasnquest.data.remote.KakaoLoginRequestDto
import com.example.busasnquest.data.remote.LoginRequestDto
import com.example.busasnquest.data.remote.SignupRequestDto
import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException

// 서버가 내려준 에러 응답에서 detail 메시지를 뽑아낸다.
// FastAPI 는 오류 시 {"detail": "..."} 형태로 응답한다.
// 뽑아내지 못하면 fallback 문구를 사용한다.
private fun HttpException.serverDetail(fallback: String): String {
    return try {
        val body = response()?.errorBody()?.string()
        if (body.isNullOrBlank()) fallback
        else JSONObject(body).optString("detail", fallback)
    } catch (e: Exception) {
        fallback
    }
}

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
        /**
    * 이메일/비밀번호 로그인.
    * 앱에서 입력한 email, password를 FastAPI 백엔드로 전송하고,
    * 성공하면 백엔드가 내려준 JWT token 문자열을 반환한다.
    *
    * 호출되는 백엔드 API:
    * POST http://10.0.2.2:8000/api/v1/auth/login
    *
    * 요청 JSON:
    * {
    *   "email": "user@example.com",
    *   "password": "myPassword123"
    * }
    *
    * 응답 JSON:
    * {
    *   "token": "test-jwt-token"
    * }
    */
    override suspend fun login(email: String, password: String): Result<String> {
        return try {
            // Retrofit으로 로그인 API 호출
            val response = api.login(
                LoginRequestDto(
                    email = email,
                    password = password
                )
            )

            // 서버 응답에서 token만 꺼내서 성공 결과로 반환
            Result.success(response.token)

        } catch (e: HttpException) {
            // 서버가 401, 400 같은 오류 상태 코드를 내려준 경우
            Result.failure(Exception(e.serverDetail("이메일 또는 비밀번호가 올바르지 않습니다.")))

        } catch (e: IOException) {
            // 서버가 꺼져 있거나, 네트워크 연결이 안 되는 경우
            Result.failure(Exception("네트워크 연결을 확인해주세요."))

        } catch (e: Exception) {
            // 그 외 JSON 파싱 오류 등 예상하지 못한 오류
            Result.failure(Exception("로그인 중 오류가 발생했습니다."))
        }
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
    * 이메일/비밀번호 회원가입.
    * 회원가입 성공 시 백엔드가 JWT token을 바로 내려주므로,
    * 앱에서는 이 token을 저장해서 자동 로그인처럼 처리할 수 있다.
    *
    * 호출되는 백엔드 API:
    * POST http://10.0.2.2:8000/api/v1/auth/signup
    *
    * 요청 JSON:
    * {
    *   "email": "new@example.com",
    *   "password": "myPassword123"
    * }
    *
    * 응답 JSON:
    * {
    *   "token": "test-jwt-token"
    * }
    */
    override suspend fun signup(email: String, password: String): Result<String> {
        return try {
            // Retrofit으로 회원가입 API 호출
            val response = api.signup(
                SignupRequestDto(
                    email = email,
                    password = password
                )
            )

            // 서버가 내려준 token 반환
            Result.success(response.token)

        } catch (e: HttpException) {
            // 이메일 중복 409, 입력 오류 400 등이 여기로 들어옴
            // 서버가 준 구체적 이유(detail)를 그대로 보여준다.
            Result.failure(Exception(e.serverDetail("이미 가입된 이메일이거나 입력이 올바르지 않습니다.")))

        } catch (e: IOException) {
            // 서버 연결 실패
            Result.failure(Exception("네트워크 연결을 확인해주세요."))

        } catch (e: Exception) {
            // 기타 오류
            Result.failure(Exception("회원가입 중 오류가 발생했습니다."))
        }
    }
}
