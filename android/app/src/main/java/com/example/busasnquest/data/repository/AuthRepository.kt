package com.example.busasnquest.data.repository

import com.example.busasnquest.data.remote.AuthApi
import com.example.busasnquest.data.remote.KakaoLoginRequestDto
import com.example.busasnquest.data.remote.FindIdRequestDto
import com.example.busasnquest.data.remote.FindPasswordRequestDto
import com.example.busasnquest.data.remote.LoginRequestDto
import com.example.busasnquest.data.remote.SignupRequestDto
import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException
import com.example.busasnquest.data.remote.AgreementDto

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
 * 규격서 9장(오류 처리)의 상태 코드별 안내 문구.
 *
 * 404 = 닉네임/이메일과 일치하는 계정 없음
 * 500 = 메일 발송 실패 등 서버 문제
 * 그 외에는 서버가 준 detail 을 그대로 쓴다.
 */
private fun HttpException.accountLookupMessage(): String = when (code()) {
    404 -> serverDetail("계정을 찾을 수 없습니다.")
    500 -> "잠시 후 다시 시도해주세요."
    else -> serverDetail("요청을 처리하지 못했습니다. (${code()})")
}

/**
 * 인증 데이터 계층의 추상화.
 * 화면/ViewModel 은 이 인터페이스만 알고 실제 서버 구현을 주입받는다.
 *
 * 성공 시 토큰 문자열을 담은 Result 를 돌려준다.
 */
interface AuthRepository {
    suspend fun login(email: String, password: String): Result<String>

    // 카카오 access token 을 서버로 보내 우리 서버 JWT 를 받는다
    // agreements: 신규 가입일 수 있으므로 약관 동의 이력을 함께 전달한다
    suspend fun loginWithKakao(
        kakaoAccessToken: String,
        agreements: List<AgreementDto> = emptyList()
    ): Result<String>

    // 이메일/비밀번호로 회원가입 후 JWT 를 받는다 (약관 동의 이력 포함)
    suspend fun signup(
        email: String,
        password: String,
        nickname: String,
        agreements: List<AgreementDto> = emptyList()
    ): Result<String>

    /** 닉네임으로 가입 이메일 찾기. 성공 시 마스킹된 이메일("bu*****@gmail.com")을 돌려준다. */
    suspend fun findId(nickname: String): Result<String>

    /**
     * 임시 비밀번호 메일 발송.
     *
     * 계정 존재 여부가 노출되지 않도록 화면에는 고정 안내 문구만 보여준다.
     */
    suspend fun findPassword(email: String): Result<Unit>

    /** 로그아웃 통보. 실패해도 앱은 로컬 토큰을 지우고 진행한다. */
    suspend fun logout(): Result<Unit>
}

class RetrofitAuthRepository(
    private val api: AuthApi
) : AuthRepository {
    /**
    * 이메일/비밀번호 로그인.
    * 앱에서 입력한 email, password를 FastAPI 백엔드로 전송하고,
    * 성공하면 백엔드가 내려준 JWT token 문자열을 반환한다.
    *
    * 호출되는 백엔드 API:
    * POST /api/v1/auth/login
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
    override suspend fun loginWithKakao(
        kakaoAccessToken: String,
        agreements: List<AgreementDto>
    ): Result<String> {
        return try {
            val response = api.kakaoLogin(
                KakaoLoginRequestDto(
                    accessToken = kakaoAccessToken,
                    agreements = agreements
                )
            )
            Result.success(response.token)
        } catch (e: HttpException) {
            // 서버가 내려준 실패 사유(detail)가 있으면 그대로 보여준다
            Result.failure(Exception(e.serverDetail("카카오 로그인에 실패했습니다. 다시 시도해주세요.")))
        } catch (e: IOException) {
            Result.failure(Exception("네트워크 연결을 확인해주세요."))
        } catch (e: Exception) {
            Result.failure(Exception("카카오 로그인 중 오류가 발생했습니다."))
        }
    }
    /**
    * 이메일/비밀번호 회원가입.
    * 회원가입 성공 시 백엔드가 JWT token을 바로 내려주므로,
    * 앱에서는 이 token을 저장해서 자동 로그인처럼 처리할 수 있다.
    *
    * 호출되는 백엔드 API:
    * POST /api/v1/auth/signup
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
    override suspend fun signup(
        email: String,
        password: String,
        nickname: String,
        agreements: List<AgreementDto>
    ): Result<String> {
        return try {
            val response = api.signup(
                SignupRequestDto(
                    email = email,
                    password = password,
                    nickname = nickname,
                    agreements = agreements    // 약관 동의 이력
                )
            )
            Result.success(response.token)
        } catch (e: HttpException) {
            Result.failure(Exception(e.serverDetail("이미 가입된 이메일이거나 입력이 올바르지 않습니다.")))
        } catch (e: IOException) {
            Result.failure(Exception("네트워크 연결을 확인해주세요."))
        } catch (e: Exception) {
            Result.failure(Exception("회원가입 중 오류가 발생했습니다."))
        }
    }

    /** 닉네임 → 마스킹된 가입 이메일 */
    override suspend fun findId(nickname: String): Result<String> {
        return try {
            val response = api.findId(FindIdRequestDto(nickname = nickname.trim()))
            val masked = response.maskedEmail
            if (masked.isNullOrBlank()) {
                Result.failure(Exception("계정을 찾을 수 없습니다."))
            } else {
                Result.success(masked)
            }
        } catch (e: HttpException) {
            Result.failure(Exception(e.accountLookupMessage()))
        } catch (e: IOException) {
            Result.failure(Exception("네트워크 연결을 확인해주세요."))
        } catch (e: Exception) {
            Result.failure(Exception("아이디 찾기 중 오류가 발생했습니다."))
        }
    }

    /**
     * 임시 비밀번호 메일 발송.
     *
     * ⚠️ 응답 본문(message)을 의도적으로 쓰지 않는다 — 인터페이스 주석 참고.
     */
    override suspend fun findPassword(email: String): Result<Unit> {
        return try {
            api.findPassword(FindPasswordRequestDto(email = email.trim().lowercase()))
            Result.success(Unit)
        } catch (e: HttpException) {
            Result.failure(Exception(e.accountLookupMessage()))
        } catch (e: IOException) {
            Result.failure(Exception("네트워크 연결을 확인해주세요."))
        } catch (e: Exception) {
            Result.failure(Exception("비밀번호 찾기 중 오류가 발생했습니다."))
        }
    }

    /** 로그아웃 통보. 호출한 쪽은 실패해도 로컬 토큰을 지운다. */
    override suspend fun logout(): Result<Unit> {
        return try {
            api.logout()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
