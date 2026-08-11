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

    // 서버가 내려주는 구 이름 (예: "영도구") — 지역별 매칭에 사용
    val district: String = "",

    val location: String = "",

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
    val imageUrl: String? = null,

    val latitude: Double = 0.0,

    val longitude: Double = 0.0,

    @SerializedName("target_text")
    val targetText: String = "",

    // 현재 로그인한 사용자의 찜 여부. 서버가 안 내려주는 옛 응답 대비 기본값 false.
    @SerializedName("is_saved")
    val isSaved: Boolean = false
)

// 찜 추가/해제 응답 (POST·DELETE /api/v1/missions/{id}/saved)
data class SavedMissionResponseDto(
    @SerializedName("mission_id")
    val missionId: Int,

    @SerializedName("is_saved")
    val isSaved: Boolean
)

// 서버 오류 본문 ({"detail": "..."}) 파싱용
data class ErrorDetailDto(
    val detail: String? = null
)

// 미션 인증(사진/위치/영수증) 제출용
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
    val nickname: String,
    val password: String
)
// 미션 인증 제출 요청 DTO
// 앱 → 백엔드로 보내는 데이터
data class MissionVerifyRequestDto(
    @SerializedName("mission_id")
    val missionId: Int,

    @SerializedName("mission_type")
    val missionType: String,

    // 사진(IMAGE) 미션 인증 이미지 URL — 백엔드 변수명 "image" 로 통일
    @SerializedName("image")
    val imageUrl: String? = null,

    val latitude: Double? = null,

    val longitude: Double? = null,

    @SerializedName("receipt_image_url")
    val receiptImageUrl: String? = null
)


// 미션 인증 제출 응답 DTO
// 백엔드 → 앱으로 돌아오는 데이터
data class MissionVerifyResponseDto(
    val success: Boolean,
    val message: String
)

data class UploadResponseDto(
    val url: String
)

// 닉네임 변경 요청 DTO (앱 → 백엔드)
data class UpdateNicknameRequestDto(
    val nickname: String
)
