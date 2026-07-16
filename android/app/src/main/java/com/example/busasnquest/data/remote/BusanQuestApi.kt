package com.example.busasnquest.data.remote

import retrofit2.http.GET
import retrofit2.http.Body
import retrofit2.http.POST

interface BusanQuestApi {

    @GET("api/v1/users/me")
    suspend fun getMyProfile(): UserProfileDto

    @GET("api/v1/missions")
    suspend fun getMissions(): List<MissionDto>

    @GET("api/v1/missions/ongoing")
    suspend fun getOngoingMissions(): List<MissionDto>

    @GET("api/v1/districts/progress")
    suspend fun getDistrictProgress(): List<DistrictStatusDto>

    // 미션 인증 제출
    @POST("api/v1/missions/verify")
    suspend fun verifyMission(
        @Body request: MissionVerifyRequestDto
    ): MissionVerifyResponseDto
}