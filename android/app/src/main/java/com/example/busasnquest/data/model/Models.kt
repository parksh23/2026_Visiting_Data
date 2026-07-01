package com.example.busasnquest.data.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

// ───────────────── DATA ─────────────────

data class OngoingMission(
    val id: Int,
    val title: String,
    val region: String,
    val reward: Int,
    val current: Int,
    val total: Int,
    val type: MissionType = MissionType.CURRENT_LOCATION,
    val district: String = "",
    val lat: Double = 0.0,      // 위도
    val lng: Double = 0.0       // 경도
)

// 구·군별 진행 현황
data class DistrictProgress(
    val name: String,
    val completed: Int,
    val total: Int,
    val color: Color
)

// 랭킹 항목
data class RankEntry(
    val rank: Int,
    val name: String,
    val score: String,
    val isMe: Boolean = false
)

// 내 정보 - 메뉴 카드
data class MenuItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val tint: Color,
    val bg: Color
)

// 내 정보 - 설정 항목
data class SettingItem(
    val title: String,
    val icon: ImageVector
)
