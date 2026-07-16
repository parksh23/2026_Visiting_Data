package com.example.busasnquest.data.repository

import com.example.busasnquest.data.model.MissionState
import com.example.busasnquest.data.model.MissionType
import com.example.busasnquest.data.model.OngoingMission
import com.example.busasnquest.data.remote.DistrictStatusDto
import com.example.busasnquest.data.remote.MissionDto
import com.example.busasnquest.data.remote.RetrofitInstance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import com.example.busasnquest.data.remote.MissionVerifyRequestDto

// 미션 하나 + 그 미션의 상태
data class MissionWithState(
    val mission: OngoingMission,
    val state: MissionState = MissionState.NOT_STARTED,
    val error: String? = null,
    val saved: Boolean = false
)


// 구·군별 미션 진행 상황
data class DistrictMissionProgress(
    val name: String,      // 구·군 이름
    val completed: Int,    // 완료한 미션 수
    val total: Int         // 전체 미션 수
)


// 점령 통계
data class OccupationStat(
    val completedMissions: Int = 0,
    val totalMissions: Int = 0,
    val rate: Float = 0f
)


// 앱 전체에서 미션을 관리하는 단일 진실 공급원
object MissionRepository {

    // StateFlow를 stateIn으로 만들 때 필요한 CoroutineScope
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // 전체 미션 목록 기본값
    // 서버 연결 전에도 앱이 빈 화면이 되지 않도록 임시 미션을 가지고 있음
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
        OngoingMission(8, "자갈치시장 구경", "중구", 100, 0, 1, MissionType.PHOTO_LOCATION, "중구")
    )

    // 미션 + 상태 목록
    private val _missions = MutableStateFlow(
        allMissions.map { MissionWithState(mission = it) }
    )

    val missions: StateFlow<List<MissionWithState>> = _missions.asStateFlow()

    // 서버에서 받은 구·군별 진행률 저장
    // null이면 아직 서버 데이터를 불러오지 않은 상태
    // 이 변수는 districtProgress에서 사용되므로 districtProgress보다 위에 있어야 함
    private val _serverDistrictProgress =
        MutableStateFlow<List<DistrictMissionProgress>?>(null)

    // 서버에서 미션 데이터를 한 번이라도 불러왔는지 확인하는 변수
    private var loadedFromServer = false


    // 찜하기 토글
    fun toggleSaved(id: Int) {
        updateMission(id) { it.copy(saved = !it.saved) }
    }


    // 특정 미션의 상태를 바꾸는 헬퍼 함수
    private fun updateMission(id: Int, transform: (MissionWithState) -> MissionWithState) {
        _missions.update { list ->
            list.map { if (it.mission.id == id) transform(it) else it }
        }
    }


    // 미션 탭에서 "도전하기"를 눌렀을 때 진행 중으로 변경
    fun startMission(id: Int) {
        updateMission(id) {
            if (it.state == MissionState.NOT_STARTED) {
                it.copy(state = MissionState.IN_PROGRESS)
            } else {
                it
            }
        }
    }


    // 인증 시작 → 확인 중 상태로 변경
    fun setVerifying(id: Int) {
        updateMission(id) {
            it.copy(state = MissionState.VERIFYING, error = null)
        }
    }


    // 인증 완료 → 완료 상태로 변경
    fun setCompleted(id: Int) {
        updateMission(id) {
            it.copy(state = MissionState.COMPLETED)
        }
    }


    // 인증 실패 → 진행 중으로 되돌리고 에러 메시지 저장
    fun setError(id: Int, message: String) {
        updateMission(id) {
            it.copy(state = MissionState.IN_PROGRESS, error = message)
        }
    }


    // 특정 구에서 내가 완료한 미션 수
    fun completedCountInDistrict(district: String): Int {
        return _missions.value.count {
            it.mission.district == district && it.state == MissionState.COMPLETED
        }
    }


    // FastAPI 서버에서 미션 목록을 가져와 앱 내부 미션 목록으로 변환
    suspend fun refreshMissionsFromServer(force: Boolean = false) {
        // 이미 서버에서 불러왔고 강제 새로고침이 아니면 다시 요청하지 않음
        if (loadedFromServer && !force) return

        // GET /api/v1/missions 호출
        val serverMissions = RetrofitInstance.api.getMissions()

        // 서버 DTO를 앱 내부 모델로 변환
        _missions.value = serverMissions.map { dto ->
            MissionWithState(
                mission = dto.toOngoingMission(),
                state = dto.status.toMissionState()
            )
        }

        loadedFromServer = true
    }


    // FastAPI 서버에서 구·군별 점령 현황을 가져오는 함수
    suspend fun refreshDistrictProgressFromServer() {
        // GET /api/v1/districts/progress 호출
        val serverDistricts = RetrofitInstance.api.getDistrictProgress()

        // 서버 DTO를 앱 내부 모델로 변환해서 저장
        _serverDistrictProgress.value = serverDistricts.map { dto ->
            dto.toDistrictMissionProgress()
        }
    }


    // 구·군별 진행률
    // 서버 데이터가 있으면 서버 값을 사용하고,
    // 아직 서버 데이터가 없으면 미션 목록 기준으로 직접 계산
    val districtProgress: StateFlow<List<DistrictMissionProgress>> =
        combine(
            _missions,
            _serverDistrictProgress
        ) { missionList, serverDistricts ->

            if (serverDistricts != null) {
                serverDistricts
            } else {
                missionList
                    .groupBy { it.mission.district }
                    .map { (district, missions) ->
                        DistrictMissionProgress(
                            name = district,
                            completed = missions.count { it.state == MissionState.COMPLETED },
                            total = missions.size
                        )
                    }
                    .sortedByDescending { it.total }
            }
        }.stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )


    // 점령률 = 완료한 미션 수 / 전체 미션 수
    val occupation: StateFlow<OccupationStat> =
        _missions
            .map { list ->
                val completed = list.count { it.state == MissionState.COMPLETED }
                val total = list.size

                OccupationStat(
                    completedMissions = completed,
                    totalMissions = total,
                    rate = if (total == 0) 0f else completed.toFloat() / total
                )
            }
            .stateIn(
                scope = scope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = OccupationStat()
            )
        // 미션 인증 정보를 서버로 제출하는 함수
    suspend fun submitMissionVerification(
        missionId: Int,
        missionType: String,
        photoUrl: String? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        receiptImageUrl: String? = null
    ): Boolean {
        // 앱에서 받은 값을 서버 요청 DTO로 변환
        val request = MissionVerifyRequestDto(
            missionId = missionId,
            missionType = missionType,
            photoUrl = photoUrl,
            latitude = latitude,
            longitude = longitude,
            receiptImageUrl = receiptImageUrl
        )

        // POST /api/v1/missions/verify 호출
        val response = RetrofitInstance.api.verifyMission(request)

        // 서버가 success=true를 주면 앱 상태를 확인 중으로 변경
        if (response.success) {
            setVerifying(missionId)
        }

        return response.success
    }
}


// 서버에서 받은 MissionDto를 앱 화면에서 쓰는 OngoingMission으로 변환
private fun MissionDto.toOngoingMission(): OngoingMission {
    return OngoingMission(
        id = missionId,
        title = title,
        region = location,
        reward = rewardPoints,
        current = progressCurrent,
        total = progressTotal,
        type = missionType.toMissionType(),
        district = location.extractDistrict()
    )
}


// 서버에서 받은 mission_type 문자열을 앱 내부 MissionType으로 변환
private fun String.toMissionType(): MissionType {
    return when (this.uppercase()) {
        "CURRENT_LOCATION" -> MissionType.CURRENT_LOCATION
        "PHOTO", "PHOTO_LOCATION" -> MissionType.PHOTO_LOCATION
        "RECEIPT" -> MissionType.RECEIPT
        else -> MissionType.CURRENT_LOCATION
    }
}


// 서버에서 받은 status 문자열을 앱 내부 MissionState로 변환
private fun String.toMissionState(): MissionState {
    return when (this.lowercase()) {
        "completed" -> MissionState.COMPLETED
        "ongoing", "in_progress" -> MissionState.IN_PROGRESS
        "verifying" -> MissionState.VERIFYING
        else -> MissionState.NOT_STARTED
    }
}


// "남구 용호동" 같은 location 문자열에서 첫 단어인 "남구"만 추출
private fun String.extractDistrict(): String {
    return this.split(" ").firstOrNull().orEmpty()
}


// 서버에서 받은 DistrictStatusDto를 앱 내부 DistrictMissionProgress로 변환
private fun DistrictStatusDto.toDistrictMissionProgress(): DistrictMissionProgress {
    return DistrictMissionProgress(
        name = districtName,
        completed = completedCount,
        total = totalCount
    )
}