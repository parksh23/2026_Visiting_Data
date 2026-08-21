package kr.co.busanquest.data.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

// ───────────────── DATA ─────────────────

data class OngoingMission(
    val id: Int,                  // ← 미션 고유 번호
    val title: String,
    val region: String,
    val reward: Int,
    val current: Int,
    val total: Int,
    val type: MissionType = MissionType.CURRENT_LOCATION,
    val district: String = "",
    val lat: Double = 0.0,      // 위도 (지도 핀)
    val lng: Double = 0.0,      // 경도 (지도 핀)
    val imageUrl: String? = null, // 히어로 카드용 대표 사진 (null 이면 구별 그라데이션 폴백)

    /**
     * 서버가 이 미션을 내려줄 때 쓴 mission_type 원문("IMAGE" / "PHOTO" / "RECEIPT" …).
     *
     * 인증을 제출할 때 이 값을 그대로 되돌려 보내지는 않는다.
     * 백엔드 계약이 PHOTO · CURRENT_LOCATION · RECEIPT 세 값으로 정리되어,
     * 원문을 한 번 앱 타입으로 읽은 뒤 정규 표기로 바꿔 보낸다
     * (HomeViewModel.canonicalTypeOf). 서버 DB 에 옛 값이 남아 있어도
     * 요청은 항상 정규 값으로 나간다.
     * 비어 있으면(=로컬 샘플 데이터) 앱이 파싱한 type 을 쓴다.
     */
    val serverType: String = ""
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

// 내 정보 - 메뉴 카드 (icon: 손그림 drawable 리소스 id)
data class MenuItem(
    val title: String,
    val subtitle: String,
    @androidx.annotation.DrawableRes val icon: Int,
    val tint: Color,
    val bg: Color
)

// 설정 항목(SettingItem)은 ui/profile/SettingsCatalog.kt 로 이동했다.
// (제목·아이콘뿐 아니라 이동할 route 까지 갖게 되어 화면 레이어에 두는 게 맞다)
