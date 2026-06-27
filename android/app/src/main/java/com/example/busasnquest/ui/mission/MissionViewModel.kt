package com.example.busasnquest.ui.mission

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.busasnquest.data.model.DistrictProgress
import com.example.busasnquest.data.model.districtProgressList
import com.example.busasnquest.data.repository.MissionRepository
import com.example.busasnquest.data.repository.MissionWithState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted

data class MissionUiState(
    val selectedTab: Int = 0,                       // 0 = 전체, 1 = 지역
    val allMissions: List<MissionWithState> = emptyList(),
    val districts: List<DistrictProgress> = emptyList(),
    val expandedDistrict: String? = null            // 펼쳐진 구·군 (없으면 null)
)

class MissionViewModel : ViewModel() {

    // 화면에서 직접 바꾸는 상태 (탭 선택, 펼침)
    private val _localState = MutableStateFlow(LocalState())
    private data class LocalState(
        val selectedTab: Int = 0,
        val expandedDistrict: String? = null
    )

    // Repository의 미션 + 로컬 상태를 합쳐서 화면용 상태로
    val uiState: StateFlow<MissionUiState> =
        combine(MissionRepository.missions, _localState) { missions, local ->
            MissionUiState(
                selectedTab = local.selectedTab,
                allMissions = missions,
                districts = districtProgressList,
                expandedDistrict = local.expandedDistrict
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = MissionUiState()
        )

    fun selectTab(index: Int) {
        _localState.value = _localState.value.copy(selectedTab = index)
    }

    // 지역 화살표 누름 → 펼치기/접기 (같은 걸 또 누르면 접힘)
    fun toggleDistrict(name: String) {
        _localState.value = _localState.value.copy(
            expandedDistrict = if (_localState.value.expandedDistrict == name) null else name
        )
    }

    // "도전하기" → Repository에 진행 중으로 표시 (→ 홈에 나타남)
    fun startMission(id: Int) {
        MissionRepository.startMission(id)
    }

    // 특정 구·군의 미션만 골라내기
    fun missionsOf(district: String): List<MissionWithState> {
        return uiState.value.allMissions.filter { it.mission.district == district }
    }
}