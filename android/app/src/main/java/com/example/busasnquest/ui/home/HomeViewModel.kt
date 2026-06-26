package com.example.busasnquest.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.busasnquest.data.model.OngoingMission
import com.example.busasnquest.data.model.ongoingMissions
import com.example.busasnquest.data.repository.UserRepository
import com.example.busasnquest.util.getCurrentLocation
import com.example.busasnquest.util.readPhotoLocation
import android.content.Context
import android.net.Uri
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// 미션의 완료 단계
enum class MissionStatus {
    READY,      // 아직 시작 안 함
    VERIFYING,  // 인증 확인 중 (지금은 가짜)
    COMPLETED   // 완료됨
}

// 미션 하나 + 그 미션의 진행 상태
data class MissionItem(
    val mission: OngoingMission,
    val status: MissionStatus = MissionStatus.READY,
    val error: String? = null
)

data class HomeUiState(
    val missions: List<MissionItem> = emptyList()
)

class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        _uiState.update {
            it.copy(missions = ongoingMissions.map { m -> MissionItem(mission = m) })
        }
    }

    // 특정 미션의 상태만 바꾸는 헬퍼
    private fun updateMission(index: Int, transform: (MissionItem) -> MissionItem) {
        _uiState.update { state ->
            val newList = state.missions.toMutableList()
            newList[index] = transform(newList[index])
            state.copy(missions = newList)
        }
    }

    // 기본 인증 (위치/영수증 등에서 공통으로 쓰는 완료 처리)
    fun verifyMission(index: Int) {
        if (_uiState.value.missions[index].status != MissionStatus.READY) return

        viewModelScope.launch {
            updateMission(index) { it.copy(status = MissionStatus.VERIFYING, error = null) }
            delay(1000)
            updateMission(index) { it.copy(status = MissionStatus.COMPLETED) }
            UserRepository.addPoints(_uiState.value.missions[index].mission.reward)
        }
    }

    // 갤러리에서 사진을 골랐을 때 (index번째 미션)
    fun onPhotoPicked(index: Int, context: Context, uri: Uri) {
        if (_uiState.value.missions[index].status != MissionStatus.READY) return

        val location = readPhotoLocation(context, uri)
        if (location != null) {
            // 위치가 있으면 인증 진행
            viewModelScope.launch {
                updateMission(index) { it.copy(status = MissionStatus.VERIFYING, error = null) }
                delay(1000)
                updateMission(index) { it.copy(status = MissionStatus.COMPLETED) }
                UserRepository.addPoints(_uiState.value.missions[index].mission.reward)
            }
        } else {
            updateMission(index) {
                it.copy(error = "이 사진에는 위치정보가 없어요. 위치 기록을 켜고 찍은 사진을 올려주세요.")
            }
        }
    }

    // 위치 권한 허락 → 현재 위치로 인증 (index번째 미션)
    fun onLocationPermissionGranted(index: Int, context: Context) {
        if (_uiState.value.missions[index].status != MissionStatus.READY) return

        viewModelScope.launch {
            updateMission(index) { it.copy(status = MissionStatus.VERIFYING, error = null) }

            val location = getCurrentLocation(context)

            if (location != null) {
                updateMission(index) { it.copy(status = MissionStatus.COMPLETED) }
                UserRepository.addPoints(_uiState.value.missions[index].mission.reward)
            } else {
                updateMission(index) {
                    it.copy(status = MissionStatus.READY, error = "위치를 가져오지 못했어요. 야외에서 다시 시도해주세요.")
                }
            }
        }
    }

    // 위치 권한 거부 (index번째 미션)
    fun onLocationPermissionDenied(index: Int) {
        updateMission(index) {
            it.copy(error = "위치 권한이 있어야 이 미션을 완료할 수 있어요.")
        }
    }

    // 영수증 촬영 완료 (index번째 미션)
    fun onReceiptCaptured(index: Int, success: Boolean) {
        if (!success) return  // 촬영 취소
        if (_uiState.value.missions[index].status != MissionStatus.READY) return

        viewModelScope.launch {
            updateMission(index) { it.copy(status = MissionStatus.VERIFYING, error = null) }
            delay(1000)
            updateMission(index) { it.copy(status = MissionStatus.COMPLETED) }
            UserRepository.addPoints(_uiState.value.missions[index].mission.reward)
        }
    }

    // 카메라 권한 거부 (index번째 미션)
    fun onCameraPermissionDenied(index: Int) {
        updateMission(index) {
            it.copy(error = "카메라 권한이 있어야 영수증을 촬영할 수 있어요.")
        }
    }
}