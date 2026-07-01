package com.example.busasnquest.data.remote

import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("api/v1/auth/login")   // ← 명세서에 적은 경로
    suspend fun login(@Body request: LoginRequestDto): LoginResponseDto
}