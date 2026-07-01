package com.example.busasnquest.data.model

// 미션의 진행 상태
enum class MissionState {
    NOT_STARTED,  // 아직 도전 안 함 (미션 탭에만 보임)
    IN_PROGRESS,  // 도전 중 (홈 화면에 나타남)
    VERIFYING,    // 인증 확인 중
    COMPLETED     // 완료
}