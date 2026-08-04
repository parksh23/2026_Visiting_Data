package com.example.busasnquest.data.model

// 미션을 어떻게 완료하는지(검증 방식)
enum class MissionType {
    PHOTO_LOCATION,   // 사진 위치 인증 (사진의 GPS로 판정)
    CURRENT_LOCATION, // 현재 위치 인증 (지금 내 위치로 판정)
    RECEIPT           // 결제 영수증 인증
}

/**
 * 인증 제출 시 서버에 보내는 mission_type 문자열.
 * 백엔드가 미션을 내려줄 때 사진 미션을 "IMAGE" 로 주므로, 제출 방향도 "IMAGE" 로 맞춘다.
 */
fun MissionType.toServerType(): String = when (this) {
    MissionType.PHOTO_LOCATION -> "IMAGE"
    MissionType.CURRENT_LOCATION -> "CURRENT_LOCATION"
    MissionType.RECEIPT -> "RECEIPT"
}
