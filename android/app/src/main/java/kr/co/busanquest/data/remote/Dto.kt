package kr.co.busanquest.data.remote

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

/**
 * 로그인·회원가입·카카오 로그인의 공통 응답.
 *
 * 서버는 지금 같은 JWT 를 access_token 과 token 두 키로 함께 내려준다.
 *   {"access_token": "...", "token_type": "bearer", "token": "..."}
 *
 * access_token 이 OAuth2 표준 키라 서버가 언젠가 token 을 걷어낼 수 있다.
 * 그때 앱이 조용히 깨지지 않도록 access_token 을 먼저 보고, 없을 때만 token 으로 물러선다.
 * 둘 다 비어 있으면 authToken 이 null 이고, 호출부가 실패로 처리한다.
 */
data class LoginResponseDto(
    @SerializedName("access_token")
    val accessToken: String? = null,

    val token: String? = null
) {
    val authToken: String?
        get() = accessToken?.takeIf { it.isNotBlank() }
            ?: token?.takeIf { it.isNotBlank() }
}

/**
 * 약관 동의 이력 한 건.
 *
 * 어떤 문서를, 어느 버전으로, 언제 동의했는지를 남긴다.
 * doc 값: "terms"(이용약관) | "privacy"(개인정보처리방침) | "location"(위치기반서비스 이용약관)
 * version 은 assets/{doc}.md 머리말의 version 값을 그대로 보낸다.
 * agreedAt 은 ISO-8601 UTC (예: 2026-08-12T09:30:00Z)
 */
data class AgreementDto(
    val doc: String,
    val version: String,
    val agreed: Boolean,

    @SerializedName("agreed_at")
    val agreedAt: String
)

// 카카오 로그인 요청 - 앱이 받은 카카오 access token 을 서버로 전달
// 신규 가입일 수 있으므로 동의 이력을 함께 보낸다 (서버는 신규일 때만 저장하면 된다)
data class KakaoLoginRequestDto(
    @SerializedName("access_token")
    val accessToken: String,

    val agreements: List<AgreementDto> = emptyList()
)

// 회원가입 요청 - 서버로 보낼 것
data class SignupRequestDto(
    val email: String,
    val nickname: String,
    val password: String,

    val agreements: List<AgreementDto> = emptyList()
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
) {
    /**
     * 호환용 — 같은 값을 예전 키(photo_url)로도 함께 보낸다.
     *
     * 배포된 서버는 아직 photo_url 을 기대하는데 앱은 image 로 보내고 있어서,
     * 서버가 사진 URL 을 못 받아 사진 인증이 실패한다. 두 키를 같이 실어 보내면
     * 백엔드 배포 순서와 상관없이 동작한다 (FastAPI 는 모르는 필드를 무시한다).
     *
     * ⚠️ 백엔드가 image 로 전환·배포되면 이 필드는 지울 것.
     */
    @SerializedName("photo_url")
    private val photoUrlCompat: String? = imageUrl
}


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

// ───────── 계정 보안·관리 ─────────

// 아이디 찾기 요청 (POST /api/v1/auth/find-id) — 로그인 전이라 토큰 불필요
data class FindIdRequestDto(
    val nickname: String
)

// 아이디 찾기 응답. 서버가 이메일 앞 2자만 남기고 마스킹해서 준다.
data class FindIdResponseDto(
    val message: String? = null,

    @SerializedName("masked_email")
    val maskedEmail: String? = null
)

// 비밀번호 찾기 요청 (POST /api/v1/auth/find-password)
// 서버가 임시 비밀번호를 만들어 응답 본문으로 직접 돌려준다.
data class FindPasswordRequestDto(
    val email: String
)

/**
 * 비밀번호 찾기 응답 (POST /api/v1/auth/find-password)
 *
 * 서버 SMTP 포트 차단으로 메일 발송이 불가해,
 * 발급된 임시 비밀번호를 응답 본문(temp_password)으로 직접 내려준다.
 *
 * message 에도 임시 비밀번호가 섞여 오므로 화면에는 tempPassword 만 쓴다.
 * 서버가 필드를 빠뜨릴 수 있으니 전부 nullable 로 둔다.
 */
data class FindPasswordResponseDto(
    val success: Boolean? = null,

    @SerializedName("temp_password")
    val tempPassword: String? = null,

    val message: String? = null
)

/**
 * 비밀번호 변경 요청 (PATCH /api/v1/users/me/password)
 *
 * 키 이름은 서버 계약 그대로 old_password / new_password 다.
 */
data class ChangePasswordRequestDto(
    @SerializedName("old_password")
    val oldPassword: String,

    @SerializedName("new_password")
    val newPassword: String
)

/**
 * 성공 여부만 돌려주는 공통 응답 ({"success": true, "message": "..."}).
 *
 * 로그아웃·회원 탈퇴·비밀번호 변경이 이 형태다.
 * (비밀번호 찾기는 임시 비밀번호를 받아야 해서 FindPasswordResponseDto 를 따로 쓴다)
 * 서버가 본문 없이 204 를 주는 경우도 대비해 전부 nullable 로 둔다.
 */
data class SimpleResultDto(
    val success: Boolean? = null,
    val message: String? = null
)

/**
 * 알림 설정 (GET/PATCH /api/v1/users/me/notifications).
 *
 * 키 이름과 기본값은 서버 USER_SETTINGS 와 1:1 이다.
 * 서버도 푸시를 보낼지 말지 이 값으로 판단하므로, 기기 설정만 바꾸면 서버 푸시는 그대로 온다.
 */
data class NotificationSettingsDto(
    @SerializedName("mission_result")
    val missionResult: Boolean = true,

    @SerializedName("new_mission")
    val newMission: Boolean = true,

    @SerializedName("ranking_change")
    val rankingChange: Boolean = false,

    @SerializedName("night_mute")
    val nightMute: Boolean = true,

    val marketing: Boolean = false
)

/**
 * 알림 설정 부분 변경 (PATCH).
 * 서버가 null 인 필드는 건드리지 않으므로, 바꾼 항목 하나만 채워 보낸다.
 */
data class NotificationSettingsUpdateDto(
    @SerializedName("mission_result")
    val missionResult: Boolean? = null,

    @SerializedName("new_mission")
    val newMission: Boolean? = null,

    @SerializedName("ranking_change")
    val rankingChange: Boolean? = null,

    @SerializedName("night_mute")
    val nightMute: Boolean? = null,

    val marketing: Boolean? = null
)

/** FCM 토큰 등록 (POST /api/v1/users/me/push-token) */
data class PushTokenRequestDto(
    val token: String,
    val platform: String = "android"
)

/** FCM 토큰 해제 (DELETE /api/v1/users/me/push-token) — 로그아웃 시 호출 */
data class PushTokenDeleteRequestDto(
    val token: String
)
