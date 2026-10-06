package kr.co.busanquest.data.repository

import kotlinx.coroutines.delay
import kr.co.busanquest.data.remote.AuthApi
import kr.co.busanquest.data.remote.KakaoLoginRequestDto
import kr.co.busanquest.data.remote.FindIdRequestDto
import kr.co.busanquest.data.remote.FindPasswordRequestDto
import kr.co.busanquest.data.remote.LoginRequestDto
import kr.co.busanquest.data.remote.SignupRequestDto
import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException
import kr.co.busanquest.data.remote.AgreementDto

/**
 * 카카오 로그인인데 아직 우리 서비스 회원이 아니고, 약관 동의 이력도 안 보낸 경우.
 *
 * 서버는 이때 400 을 준다(신규 가입이므로 필수 약관 동의가 필요하다).
 * 화면이 "그냥 실패"와 구분해서 약관 동의 시트를 띄울 수 있도록 별도 타입으로 올린다.
 */
class KakaoAgreementRequiredException(message: String) : Exception(message)

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
/**
 * 네트워크 예외를 사용자 문구로. "네트워크 연결을 확인해주세요" 하나로 뭉치면
 * (a) 기기가 오프라인, (b) 서버가 잠들어 응답이 늦음, (c) 주소를 못 찾음 을 구분할 수 없다.
 */
private fun IOException.networkMessage(): String = when (this) {
    is java.net.SocketTimeoutException ->
        "서버 응답이 늦습니다. 잠시 후 다시 시도해주세요."
    is java.net.UnknownHostException ->
        "서버에 연결할 수 없습니다. 인터넷 연결을 확인해주세요."
    else -> "네트워크 연결을 확인해주세요."
}

private fun HttpException.accountLookupMessage(): String = when (code()) {
    404 -> serverDetail("계정을 찾을 수 없습니다.")
    500 -> "잠시 후 다시 시도해주세요."
    else -> serverDetail("요청을 처리하지 못했습니다. (${code()})")
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
     * 임시 비밀번호 발급.
     *
     * 서버 SMTP 포트 차단으로 메일 발송이 불가해,
     * 응답 본문(temp_password)으로 받은 임시 비밀번호 평문을 그대로 돌려준다.
     * 화면은 이 값을 다이얼로그 안에 표시한다.
     *
     * ⚠️ message 필드에도 임시 비밀번호가 섞여 오지만 형식이 보장되지 않으므로 쓰지 않는다.
     */
    suspend fun findPassword(email: String): Result<String>

    /** 로그아웃 통보. 실패해도 앱은 로컬 토큰을 지우고 진행한다. */
    suspend fun logout(): Result<Unit>
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

    override suspend fun loginWithKakao(
        kakaoAccessToken: String,
        agreements: List<AgreementDto>
    ): Result<String> {
        delay(500)
        return Result.success("fake-kakao-token-${System.currentTimeMillis()}")
    }

    override suspend fun signup(
        email: String,
        password: String,
        nickname: String,
        agreements: List<AgreementDto>
    ): Result<String> {
        delay(800)
        return Result.success("fake-signup-token-${System.currentTimeMillis()}")
    }

    override suspend fun findId(nickname: String): Result<String> {
        delay(600)
        return if (nickname.isNotBlank()) Result.success("bu*****@gmail.com")
        else Result.failure(Exception("계정을 찾을 수 없습니다."))
    }

    override suspend fun findPassword(email: String): Result<String> {
        delay(600)
        return if (email.contains("@")) Result.success("a1B2c3D4!")
        else Result.failure(Exception("계정을 찾을 수 없습니다."))
    }

    override suspend fun logout(): Result<Unit> {
        delay(200)
        return Result.success(Unit)
    }
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
    * POST {BASE_URL}api/v1/auth/login
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

            // access_token 우선, 없으면 token — 둘 다 없으면 실패로 돌린다
            response.authToken?.let { Result.success(it) }
                ?: Result.failure(Exception("서버 응답에 토큰이 없습니다."))

        } catch (e: HttpException) {
            // 서버가 401, 400 같은 오류 상태 코드를 내려준 경우
            Result.failure(Exception(e.serverDetail("이메일 또는 비밀번호가 올바르지 않습니다.")))

        } catch (e: IOException) {
            // 서버가 꺼져 있거나, 네트워크 연결이 안 되는 경우
            Result.failure(Exception(e.networkMessage()))

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
            response.authToken?.let { Result.success(it) }
                ?: Result.failure(Exception("서버 응답에 토큰이 없습니다."))
        } catch (e: HttpException) {
            // 400 = 신규 가입인데 필수 약관 동의가 없다는 뜻.
            // 화면이 약관 동의를 받아 같은 토큰으로 다시 호출할 수 있게 전용 예외로 올린다.
            if (e.code() == 400) {
                Result.failure(
                    KakaoAgreementRequiredException(
                        e.serverDetail("가입을 위해 약관 동의가 필요합니다.")
                    )
                )
            } else {
                // 서버가 내려준 실패 사유(detail)가 있으면 그대로 보여준다
                Result.failure(Exception(e.serverDetail("카카오 로그인에 실패했습니다. 다시 시도해주세요.")))
            }
        } catch (e: IOException) {
            Result.failure(Exception(e.networkMessage()))
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
    * POST {BASE_URL}api/v1/auth/signup
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
            response.authToken?.let { Result.success(it) }
                ?: Result.failure(Exception("서버 응답에 토큰이 없습니다."))
        } catch (e: HttpException) {
            Result.failure(Exception(e.serverDetail("이미 가입된 이메일이거나 입력이 올바르지 않습니다.")))
        } catch (e: IOException) {
            Result.failure(Exception(e.networkMessage()))
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
            Result.failure(Exception(e.networkMessage()))
        } catch (e: Exception) {
            Result.failure(Exception("아이디 찾기 중 오류가 발생했습니다."))
        }
    }

    /**
     * 임시 비밀번호 발급.
     *
     * 서버가 temp_password 에 담아 준 평문을 상위 레이어로 그대로 올린다.
     * 값이 비어 있으면(서버 계약 위반) 실패로 처리한다 — 인터페이스 주석 참고.
     */
    override suspend fun findPassword(email: String): Result<String> {
        return try {
            val response = api.findPassword(
                FindPasswordRequestDto(email = email.trim().lowercase())
            )
            val tempPwd = response.tempPassword
            if (!tempPwd.isNullOrBlank()) {
                Result.success(tempPwd)
            } else {
                Result.failure(Exception("임시 비밀번호를 가져오지 못했습니다."))
            }
        } catch (e: HttpException) {
            Result.failure(Exception(e.accountLookupMessage()))
        } catch (e: IOException) {
            Result.failure(Exception(e.networkMessage()))
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
