package kr.co.busanquest.data.remote

import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("api/v1/auth/login")   // ← 명세서에 적은 경로
    suspend fun login(@Body request: LoginRequestDto): LoginResponseDto

    // 카카오 로그인: 앱이 받은 카카오 access token 을 보내고, 우리 서버 JWT 를 받는다
    @POST("api/v1/auth/kakao")
    suspend fun kakaoLogin(@Body request: KakaoLoginRequestDto): LoginResponseDto

    // 회원가입: 이메일/비밀번호로 계정 생성 후 JWT 를 받는다 (백엔드 준비 시 연결)
    @POST("api/v1/auth/signup")
    suspend fun signup(@Body request: SignupRequestDto): LoginResponseDto

    // ───────── 아이디/비밀번호 찾기 (로그인 전 · 토큰 불필요) ─────────

    // 닉네임으로 가입 이메일을 찾는다. 없으면 서버가 404.
    @POST("api/v1/auth/find-id")
    suspend fun findId(@Body request: FindIdRequestDto): FindIdResponseDto

    // 임시 비밀번호를 발급해 응답 본문(temp_password)으로 직접 돌려준다. 계정이 없으면 404.
    // (서버 SMTP 포트 차단으로 '메일 발송' → '직접 반환' 방식으로 변경됨)
    @POST("api/v1/auth/find-password")
    suspend fun findPassword(@Body request: FindPasswordRequestDto): FindPasswordResponseDto

    // 로그아웃. JWT 는 stateless 라 서버가 토큰을 무효화하진 않지만,
    // 규격상 호출한 뒤 앱이 로컬 토큰을 지운다.
    @POST("api/v1/auth/logout")
    suspend fun logout(): SimpleResultDto
}