package com.example.busasnquest.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.busasnquest.data.model.OngoingMission
import com.example.busasnquest.data.model.ongoingMission
import com.example.busasnquest.data.repository.UserRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import android.content.Context
import android.net.Uri
import com.example.busasnquest.util.PhotoLatLng
import com.example.busasnquest.util.readPhotoLocation
import com.example.busasnquest.util.getCurrentLocation
import com.example.busasnquest.util.readPhotoLocation

// 미션의 완료 단계
enum class MissionStatus {
    READY,      // 아직 시작 안 함
    VERIFYING,  // 인증 확인 중 (지금은 가짜)
    COMPLETED   // 완료됨
}

data class HomeUiState(
    val mission: OngoingMission = ongoingMission,
    val status: MissionStatus = MissionStatus.READY,
    val photoLocation: PhotoLatLng? = null,   // 사진에서 읽은 위치
    val photoError: String? = null            // 위치 못 읽었을 때 메시지
)

class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // 인증 버튼을 눌렀을 때
    fun verifyMission() {
        // 이미 완료했거나 인증 중이면 무시 → 무한 적립 방지!
        if (_uiState.value.status != MissionStatus.READY) return

        viewModelScope.launch {
            _uiState.update { it.copy(status = MissionStatus.VERIFYING) }

            delay(1000)  // 서버가 증거를 검증하는 척 (수준 4에서 진짜로 바뀜)

            _uiState.update { it.copy(status = MissionStatus.COMPLETED) }
            UserRepository.addPoints(_uiState.value.mission.reward)  // 포인트 적립
        }
    }

    // 갤러리에서 사진을 골랐을 때 호출됨
    fun onPhotoPicked(context: Context, uri: Uri) {
        val location = readPhotoLocation(context, uri)
        if (location != null) {
            _uiState.update {
                it.copy(photoLocation = location, photoError = null)
            }
            // 위치를 읽었으니 인증 흐름 시작 (수준 2에서 만든 그거)
            verifyMission()
        } else {
            _uiState.update {
                it.copy(photoError = "이 사진에는 위치정보가 없어요. 위치 기록을 켜고 찍은 사진을 올려주세요.")
            }
        }
    }

    // 위치 권한을 허락받았을 때 → 현재 위치로 인증
    fun onLocationPermissionGranted(context: Context) {
        if (_uiState.value.status != MissionStatus.READY) return  // 중복 방지

        viewModelScope.launch {
            _uiState.update { it.copy(status = MissionStatus.VERIFYING, photoError = null) }

            val location = getCurrentLocation(context)

            if (location != null) {
                _uiState.update {
                    it.copy(photoLocation = location, status = MissionStatus.COMPLETED)
                }
                UserRepository.addPoints(_uiState.value.mission.reward)
            } else {
                // 위치를 못 얻음 → 다시 시도할 수 있게 READY로 되돌림
                _uiState.update {
                    it.copy(status = MissionStatus.READY, photoError = "위치를 가져오지 못했어요. 야외에서 다시 시도해주세요.")
                }
            }
        }
    }

    // 위치 권한을 거부했을 때
    fun onLocationPermissionDenied() {
        _uiState.update {
            it.copy(photoError = "위치 권한이 있어야 이 미션을 완료할 수 있어요.")
        }
    }

    // 영수증 사진을 찍었을 때
    fun onReceiptCaptured(context: Context, uri: Uri, success: Boolean) {
        if (!success) {
            // 사용자가 촬영을 취소함
            return
        }
        if (_uiState.value.status != MissionStatus.READY) return

        viewModelScope.launch {
            _uiState.update { it.copy(status = MissionStatus.VERIFYING, photoError = null) }

            delay(1000)  // 서버가 영수증을 검증하는 척 (수준 4에서 진짜로)

            // 지금은 "사진을 찍었다"는 것만으로 완료 처리
            // (영수증이 진짜인지 판정은 서버 몫)
            _uiState.update { it.copy(status = MissionStatus.COMPLETED) }
            UserRepository.addPoints(_uiState.value.mission.reward)
        }
    }

    // 카메라 권한 거부 시
    fun onCameraPermissionDenied() {
        _uiState.update {
            it.copy(photoError = "카메라 권한이 있어야 영수증을 촬영할 수 있어요.")
        }
    }
}