package com.example.busasnquest.data.model

// 미션을 어떻게 완료하는지(검증 방식)
enum class MissionType {
    PHOTO_LOCATION,   // 사진 위치 인증 (사진의 GPS로 판정)
    CURRENT_LOCATION, // 현재 위치 인증 (지금 내 위치로 판정)
    RECEIPT           // 결제 영수증 인증
}