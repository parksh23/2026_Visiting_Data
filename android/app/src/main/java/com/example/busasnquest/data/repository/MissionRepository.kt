package com.example.busasnquest.data.repository

import com.example.busasnquest.data.model.MissionState
import com.example.busasnquest.data.model.MissionType
import com.example.busasnquest.data.model.OngoingMission
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted

// 미션 하나 + 그 미션의 상태
data class MissionWithState(
    val mission: OngoingMission,
    val state: MissionState = MissionState.NOT_STARTED,
    val error: String? = null
)
// 구·군별 미션 진행 상황
data class DistrictMissionProgress(
    val name: String,      // 구·군 이름
    val completed: Int,    // 완료한 미션 수
    val total: Int         // 전체 미션 수
)
// 앱 전체에서 미션을 관리하는 단일 진실 공급원
object MissionRepository {

    // 전체 미션 목록 (구·군별로 다 들어있음)
    private val allMissions = listOf(
        // 해운대구
        OngoingMission(1, "해운대 해수욕장 인증샷", "해운대구", 100, 0, 1, MissionType.PHOTO_LOCATION, "해운대구"),
        OngoingMission(2, "동백섬 산책로 걷기", "해운대구", 80, 0, 1, MissionType.CURRENT_LOCATION, "해운대구"),
        OngoingMission(3, "해운대 맛집에서 식사", "해운대구", 150, 0, 1, MissionType.RECEIPT, "해운대구"),
        // 수영구
        OngoingMission(4, "광안리 해변 인증샷", "수영구", 100, 0, 1, MissionType.PHOTO_LOCATION, "수영구"),
        OngoingMission(5, "광안대교 야경 보기", "수영구", 80, 0, 1, MissionType.CURRENT_LOCATION, "수영구"),
        // 중구
        OngoingMission(6, "남포동 맛집에서 식사", "중구", 150, 0, 1, MissionType.RECEIPT, "중구"),
        OngoingMission(7, "용두산공원 방문", "중구", 80, 0, 1, MissionType.CURRENT_LOCATION, "중구"),
        OngoingMission(8, "자갈치시장 구경", "중구", 100, 0, 1, MissionType.PHOTO_LOCATION, "중구"),
    )

    // 미션 + 상태 목록 (이게 진짜 관리되는 데이터)
    private val _missions = MutableStateFlow(
        allMissions.map { MissionWithState(mission = it) }
    )
    val missions: StateFlow<List<MissionWithState>> = _missions.asStateFlow()

    // 특정 미션의 상태를 바꾸는 헬퍼
    private fun updateMission(id: Int, transform: (MissionWithState) -> MissionWithState) {
        _missions.update { list ->
            list.map { if (it.mission.id == id) transform(it) else it }
        }
    }

    // 미션 탭에서 "도전하기" → 진행 중으로
    fun startMission(id: Int) {
        updateMission(id) {
            if (it.state == MissionState.NOT_STARTED) it.copy(state = MissionState.IN_PROGRESS)
            else it
        }
    }

    // 인증 시작 → 확인 중으로
    fun setVerifying(id: Int) {
        updateMission(id) { it.copy(state = MissionState.VERIFYING, error = null) }
    }

    // 인증 완료 → 완료로
    fun setCompleted(id: Int) {
        updateMission(id) { it.copy(state = MissionState.COMPLETED) }
    }

    // 인증 실패 → 다시 진행 중으로 되돌리고 에러 표시
    fun setError(id: Int, message: String) {
        updateMission(id) { it.copy(state = MissionState.IN_PROGRESS, error = message) }
    }

    // 미션을 구·군별로 묶어서 진행률 계산 (실시간)
    val districtProgress: StateFlow<List<DistrictMissionProgress>> =
        _missions
            .map { list ->
                list.groupBy { it.mission.district }      // 구·군별로 묶기
                    .map { (district, missions) ->
                        DistrictMissionProgress(
                            name = district,
                            completed = missions.count { it.state == MissionState.COMPLETED },
                            total = missions.size
                        )
                    }
                    .sortedByDescending { it.total }       // 미션 많은 구·군 먼저
            }
            .stateIn(
                scope = scope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
}
private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)