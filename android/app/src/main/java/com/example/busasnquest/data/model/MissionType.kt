package com.example.busasnquest.data.model

// 미션을 어떻게 완료하는지(검증 방식)
enum class MissionType {
    IMAGE_LOCATION,   // 사진 위치 인증 (사진의 GPS로 판정)
    CURRENT_LOCATION, // 현재 위치 인증 (지금 내 위치로 판정)
    RECEIPT           // 결제 영수증 인증
}

/**
 * 인증 제출 시 서버에 보내는 mission_type 문자열.
 * 서버 계약의 PHOTO/CURRENT_LOCATION/RECEIPT 값을 사용한다.
 */
fun MissionType.toServerType(): String = when (this) {
    MissionType.IMAGE_LOCATION -> "PHOTO"
    MissionType.CURRENT_LOCATION -> "CURRENT_LOCATION"
    MissionType.RECEIPT -> "RECEIPT"
}
