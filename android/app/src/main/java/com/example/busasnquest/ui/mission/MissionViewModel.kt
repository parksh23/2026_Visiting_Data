package com.example.busasnquest.ui.mission

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.busasnquest.data.repository.DistrictMissionProgress
import com.example.busasnquest.data.repository.MissionRepository
import com.example.busasnquest.data.model.MissionType
import com.example.busasnquest.data.repository.MissionWithState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import com.example.busasnquest.data.repository.OccupationStat

data class MissionUiState(
    val selectedTab: Int = 0,                              // 0 = 지역별, 1 = 종류별
    val allMissions: List<MissionWithState> = emptyList(),
    val districts: List<DistrictMissionProgress> = emptyList(),
    val expandedDistrict: String? = null,                  // (구 UI 잔재 — 그리드에서는 selectedDistrict 사용)
    val selectedDistrict: String? = null,                  // 그리드에서 탭한 구 (바텀시트 열림)
    val typeFilter: MissionType? = null                    // 종류별 탭 필터 (null = 전체)
)

class MissionViewModel : ViewModel() {

    private val _localState = MutableStateFlow(LocalState())
    private data class LocalState(
        val selectedTab: Int = 0,
        val expandedDistrict: String? = null,
        val selectedDistrict: String? = null,
        val typeFilter: MissionType? = null
    )

    // 서버에서 미션을 불러오다 실패했을 때의 메시지 (null 이면 정상)
    private val _loadError = MutableStateFlow<String?>(null)
    val loadError: StateFlow<String?> = _loadError

    init {
        refreshFromServer()
    }

    // GET /api/v1/missions + /api/v1/districts/progress
    // 실패해도 앱이 죽지 않고, 갖고 있던 로컬(샘플) 데이터를 그대로 보여준다.
    fun refreshFromServer() {
        viewModelScope.launch {
            try {
                MissionRepository.refreshMissionsFromServer()
                MissionRepository.refreshDistrictProgressFromServer()
                _loadError.value = null
            } catch (e: java.io.IOException) {
                _loadError.value = "네트워크 연결을 확인해주세요. 임시 데이터를 표시합니다."
            } catch (e: retrofit2.HttpException) {
                _loadError.value = "미션을 불러오지 못했습니다. (${e.code()})"
            } catch (e: Exception) {
                _loadError.value = "미션을 불러오는 중 오류가 발생했습니다."
            }
        }
    }

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
                expandedDistrict = local.expandedDistrict,
                selectedDistrict = local.selectedDistrict,
                typeFilter = local.typeFilter
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

    // 그리드에서 구 박스를 탭 → 바텀시트 열기
    fun selectDistrict(name: String) {
        _localState.value = _localState.value.copy(selectedDistrict = name)
    }

    // 바텀시트 닫기
    fun dismissDistrict() {
        _localState.value = _localState.value.copy(selectedDistrict = null)
    }

    // 종류별 탭 필터 (같은 칩 재탭 = 전체로 해제)
    fun selectTypeFilter(type: MissionType?) {
        _localState.value = _localState.value.copy(
            typeFilter = if (_localState.value.typeFilter == type) null else type
        )
    }

    fun startMission(id: Int) {
        MissionRepository.startMission(id)
    }
    fun toggleSaved(id: Int) {
        MissionRepository.toggleSaved(id)
    }
}