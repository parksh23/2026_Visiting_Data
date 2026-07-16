package com.example.busasnquest.data.remote

import kotlinx.coroutines.delay

// 실제 서버가 아니라 테스트용 가짜 API
// BusanQuestApi 인터페이스를 구현하기 때문에,
// BusanQuestApi에 함수가 추가되면 여기에도 override를 추가해야 함
class FakeBusanQuestApi : BusanQuestApi {

    // 테스트용 프로필 응답
    override suspend fun getMyProfile(): UserProfileDto {
        delay(1000) // 실제 서버 통신처럼 1초 지연

        return UserProfileDto(
            name = "테스트갈매기",
            points = "9,999P",
            completedMissions = 42,
            savedMissions = 7
        )
    }

    // 테스트용 전체 미션 목록
    // 지금은 실제 서버를 쓸 예정이라 빈 리스트만 반환
    override suspend fun getMissions(): List<MissionDto> {
        delay(1000)
        return emptyList()
    }

    // 테스트용 진행 중 미션 목록
    override suspend fun getOngoingMissions(): List<MissionDto> {
        delay(1000)
        return emptyList()
    }

    // 테스트용 구/군별 진행률 목록
    override suspend fun getDistrictProgress(): List<DistrictStatusDto> {
        delay(1000)
        return emptyList()
    }
    override suspend fun verifyMission(
    request: MissionVerifyRequestDto
    ): MissionVerifyResponseDto {
        delay(1000)

    return MissionVerifyResponseDto(
        success = true,
        message = "미션 인증이 제출되었습니다."
        )
    }
}