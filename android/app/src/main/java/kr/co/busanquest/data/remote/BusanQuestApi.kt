package kr.co.busanquest.data.remote

import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.Part
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import okhttp3.MultipartBody

interface BusanQuestApi {

    @GET("api/v1/users/me")
    suspend fun getMyProfile(): UserProfileDto

    // 닉네임 변경. 중복이면 서버가 409(Conflict)를 내려준다.
    @PATCH("api/v1/users/me/nickname")
    suspend fun updateNickname(@Body request: UpdateNicknameRequestDto): UserProfileDto

    /**
     * 비밀번호 변경.
     *
     * 400 = 현재 비밀번호 불일치 / 새 비밀번호 길이 미달
     * 401 = 토큰 만료 → 인터셉터가 토큰 삭제 후 로그인 화면으로 보낸다
     */
    @PATCH("api/v1/users/me/password")
    suspend fun changePassword(@Body request: ChangePasswordRequestDto): SimpleResultDto

    // 회원 탈퇴 (서버는 ACCOUNT_STATUS 를 WITHDRAWN 으로 바꾸는 소프트 삭제).
    // 성공 후 앱은 로컬 토큰을 지우고 로그인 화면으로 보낸다.
    @DELETE("api/v1/users/me")
    suspend fun withdraw(): SimpleResultDto

    @GET("api/v1/missions")
    suspend fun getMissions(): List<MissionDto>

    @GET("api/v1/missions/ongoing")
    suspend fun getOngoingMissions(): List<MissionDto>

    // ───────── 미션 찜 ─────────

    // 찜한 미션 목록 (최근 찜한 순). 없으면 빈 배열.
    @GET("api/v1/missions/saved")
    suspend fun getSavedMissions(): List<MissionDto>

    // 찜 추가 (201). 이미 찜한 미션이어도 성공 응답.
    @POST("api/v1/missions/{mission_id}/saved")
    suspend fun addSavedMission(
        @Path("mission_id") missionId: Int
    ): SavedMissionResponseDto

    // 찜 해제 (200). 이미 해제된 미션이어도 성공 응답.
    @DELETE("api/v1/missions/{mission_id}/saved")
    suspend fun removeSavedMission(
        @Path("mission_id") missionId: Int
    ): SavedMissionResponseDto

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
