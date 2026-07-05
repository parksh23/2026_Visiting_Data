package com.example.busasnquest.data.remote

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
}