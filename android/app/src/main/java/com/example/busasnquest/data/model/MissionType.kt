package com.example.busasnquest.data.model

// 미션을 어떻게 완료하는지(검증 방식)
enum class MissionType {
    PHOTO_LOCATION,   // 사진 위치 인증 (사진의 GPS로 판정)
    CURRENT_LOCATION, // 현재 위치 인증 (지금 내 위치로 판정)
    RECEIPT           // 결제 영수증 인증
}

/**
 * 인증 제출 시 서버에 보내는 mission_type 문자열.
 * 규격서 4-2 의 예시("PHOTO", "CURRENT_LOCATION")에 맞춤.
 * ⚠️ PHOTO_LOCATION → "PHOTO" 매핑이 맞는지 백엔드와 확인 필요.
 */
fun MissionType.toServerType(): String = when (this) {
    MissionType.PHOTO_LOCATION -> "PHOTO"
    MissionType.CURRENT_LOCATION -> "CURRENT_LOCATION"
    MissionType.RECEIPT -> "RECEIPT"
}
