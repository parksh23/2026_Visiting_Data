package com.example.busasnquest.data.remote

import retrofit2.http.GET
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.Part
import retrofit2.http.PATCH
import retrofit2.http.POST
import okhttp3.MultipartBody

interface BusanQuestApi {

    @GET("api/v1/users/me")
    suspend fun getMyProfile(): UserProfileDto

    // 닉네임 변경. 중복이면 서버가 409(Conflict)를 내려준다.
    @PATCH("api/v1/users/me/nickname")
    suspend fun updateNickname(@Body request: UpdateNicknameRequestDto): UserProfileDto

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

    @Multipart
    @POST("api/v1/uploads")
    suspend fun uploadImage(
        @Part file: MultipartBody.Part
    ): UploadResponseDto
}
