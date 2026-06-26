package com.example.busasnquest.data.remote

import kotlinx.coroutines.delay

// 진짜 서버 대신, 정해둔 가짜 데이터를 돌려주는 버전
class FakeBusanQuestApi : BusanQuestApi {

    override suspend fun getMyProfile(): UserProfileDto {
        delay(1000)  // 서버 통신처럼 1초 기다리는 척

        return UserProfileDto(
            name = "테스트갈매기",
            points = "9,999P",
            completedMissions = 42,
            savedMissions = 7
        )
    }
}