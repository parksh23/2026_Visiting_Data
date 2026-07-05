package com.example.busasnquest.data.remote

import com.google.gson.annotations.SerializedName

// 서버에서 받을 '내 프로필' 데이터 모양
data class UserProfileDto(
    val name: String,
    val points: String,

    @SerializedName("completed_missions")
    val completedMissions: Int,

    @SerializedName("saved_missions")
    val savedMissions: Int
)

// 미션 목록/상세에서 받을 데이터 모양
data class MissionDto(
    @SerializedName("mission_id")
    val missionId: Int,

    val title: String,
    val location: String,

    @SerializedName("reward_points")
    val rewardPoints: Int,

    @SerializedName("progress_current")
    val progressCurrent: Int,

    @SerializedName("progress_total")
    val progressTotal: Int,

    val status: String,

    @SerializedName("mission_type")
    val missionType: String,

    @SerializedName("image_url")
    val imageUrl: String? = null
)

// 미션 인증(사진/위치/영수증) 제출용
data class MissionVerifyRequestDto(
    @SerializedName("mission_id")
    val missionId: Int,

    @SerializedName("mission_type")
    val missionType: String,

    @SerializedName("photo_url")
    val photoUrl: String? = null,

    val latitude: Double? = null,
    val longitude: Double? = null,

    @SerializedName("receipt_image_url")
    val receiptImageUrl: String? = null
)

// 지도 화면 - 구/군별 점령 현황
data class DistrictStatusDto(
    @SerializedName("district_name")
    val districtName: String,

    @SerializedName("completed_count")
    val completedCount: Int,

    @SerializedName("total_count")
    val totalCount: Int,

    val status: String
)

// 로그인 요청 - 서버로 보낼 것
data class LoginRequestDto(
    val email: String,
    val password: String
)

// 로그인 응답 - 서버가 돌려줄 것
data class LoginResponseDto(
    val token: String
)

// 카카오 로그인 요청 - 앱이 받은 카카오 access token 을 서버로 전달
data class KakaoLoginRequestDto(
    @SerializedName("access_token")
    val accessToken: String
)

// 회원가입 요청 - 서버로 보낼 것
data class SignupRequestDto(
    val email: String,
    val password: String
)