package com.example.busasnquest.ui.mission

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.busasnquest.data.repository.DistrictMissionProgress
import com.example.busasnquest.data.repository.MissionRepository
import com.example.busasnquest.data.repository.MissionWithState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import com.example.busasnquest.data.repository.OccupationStat

data class MissionUiState(
    val selectedTab: Int = 0,                              // 0 = 전체, 1 = 지역
    val allMissions: List<MissionWithState> = emptyList(),
    val districts: List<DistrictMissionProgress> = emptyList(),
    val expandedDistrict: String? = null
)

class MissionViewModel : ViewModel() {

    private val _localState = MutableStateFlow(LocalState())
    private data class LocalState(
        val selectedTab: Int = 0,
        val expandedDistrict: String? = null
    )

    // 미션 목록 + 구·군 진행률 + 로컬 상태, 셋을 합쳐서 화면용 상태로
    val uiState: StateFlow<MissionUiState> =
        combine(
            MissionRepository.missions,
            MissionRepository.districtProgress,
            _localState
        ) { missions, districts, local ->
            MissionUiState(
                selectedTab = local.selectedTab,
                allMissions = missions,
                districts = districts,
                expandedDistrict = local.expandedDistrict
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = MissionUiState()
        )
    // 점령률 (실제 미션 완료 기반)
    val occupation: StateFlow<OccupationStat> = MissionRepository.occupation
    fun selectTab(index: Int) {
        _localState.value = _localState.value.copy(selectedTab = index)
    }

    fun toggleDistrict(name: String) {
        _localState.value = _localState.value.copy(
            expandedDistrict = if (_localState.value.expandedDistrict == name) null else name
        )
    }

    fun startMission(id: Int) {
        MissionRepository.startMission(id)
    }
}