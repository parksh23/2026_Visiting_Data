package com.example.busasnquest.data.remote

import retrofit2.http.GET

interface BusanQuestApi {

    // "내 프로필 가져와줘" (GET = 읽기 요청)
    @GET("api/v1/users/me")
    suspend fun getMyProfile(): UserProfileDto
}

