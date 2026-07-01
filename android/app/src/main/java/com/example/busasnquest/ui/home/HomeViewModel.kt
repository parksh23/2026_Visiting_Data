package com.example.busasnquest.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.busasnquest.data.model.MissionState
import com.example.busasnquest.data.repository.MissionRepository
import com.example.busasnquest.data.repository.MissionWithState
import com.example.busasnquest.data.repository.UserRepository
import com.example.busasnquest.util.getCurrentLocation
import com.example.busasnquest.util.readPhotoLocation
import android.content.Context
import android.net.Uri
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import com.example.busasnquest.data.repository.OccupationStat

class HomeViewModel : ViewModel() {

    // Repository의 전체 미션 중 "진행 중(IN_PROGRESS)·인증 중·완료"만 걸러서 홈에 보여줌
    val homeMissions: StateFlow<List<MissionWithState>> =
        MissionRepository.missions
            .map { list ->
                list.filter {
                    it.state == MissionState.IN_PROGRESS ||
                            it.state == MissionState.VERIFYING ||
                            it.state == MissionState.COMPLETED
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    // 점령률 (실제 미션 완료 기반)
    val occupation: StateFlow<OccupationStat> = MissionRepository.occupation

    // 사진을 골랐을 때 (id로 미션 지정)
    fun onPhotoPicked(id: Int, context: Context, uri: Uri) {
        val location = readPhotoLocation(context, uri)
        if (location != null) {
            verifyAndComplete(id)
        } else {
            MissionRepository.setError(id, "이 사진에는 위치정보가 없어요. 위치 기록을 켜고 찍은 사진을 올려주세요.")
        }
    }

    // 위치 권한 허락 → 현재 위치로 인증
    fun onLocationPermissionGranted(id: Int, context: Context) {
        viewModelScope.launch {
            MissionRepository.setVerifying(id)
            val location = getCurrentLocation(context)
            if (location != null) {
                completeMission(id)
            } else {
                MissionRepository.setError(id, "위치를 가져오지 못했어요. 야외에서 다시 시도해주세요.")
            }
        }
    }

    fun onLocationPermissionDenied(id: Int) {
        MissionRepository.setError(id, "위치 권한이 있어야 이 미션을 완료할 수 있어요.")
    }

    // 영수증 촬영 완료
    fun onReceiptCaptured(id: Int, success: Boolean) {
        if (!success) return
        verifyAndComplete(id)
    }

    fun onCameraPermissionDenied(id: Int) {
        MissionRepository.setError(id, "카메라 권한이 있어야 영수증을 촬영할 수 있어요.")
    }

    // 인증 중 표시 → 1초 후 완료 (사진/영수증 공통)
    private fun verifyAndComplete(id: Int) {
        viewModelScope.launch {
            MissionRepository.setVerifying(id)
            delay(1000)
            completeMission(id)
        }
    }

    // 완료 처리 + 포인트 적립
    private fun completeMission(id: Int) {
        val reward = MissionRepository.missions.value
            .firstOrNull { it.mission.id == id }?.mission?.reward ?: 0
        MissionRepository.setCompleted(id)
        UserRepository.addPoints(reward)
    }
}