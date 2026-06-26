package com.example.busasnquest.data.remote

import com.google.gson.annotations.SerializedName

// 서버에서 받을 '내 프로필' 데이터 모양
data class UserProfileDto(
    val name: String,
    val points: String,

    @SerializedName("completed_missions")  // 서버가 쓰는 이름
    val completedMissions: Int,            // 앱에서 쓸 이름

    @SerializedName("saved_missions")
    val savedMissions: Int
)