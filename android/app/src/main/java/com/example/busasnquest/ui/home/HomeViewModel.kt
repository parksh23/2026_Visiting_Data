package com.example.busasnquest.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.busasnquest.data.model.MissionState
import com.example.busasnquest.data.model.MissionType
import com.example.busasnquest.data.model.toServerType
import com.example.busasnquest.data.remote.MissionVerifyRequestDto
import com.example.busasnquest.data.repository.MissionRepository
import com.example.busasnquest.data.repository.MissionWithState
import com.example.busasnquest.data.repository.UserRepository
import com.example.busasnquest.util.getCurrentLocation
import com.example.busasnquest.util.readImageLocation
import android.content.Context
import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import com.example.busasnquest.data.repository.OccupationStat

// 추천 미션 배지 종류 (인기 / 신규 / 추천)
enum class RecommendBadge { POPULAR, NEW, RECOMMEND }

// 홈 "추천 미션" 카드에 쓰는 가벼운 데이터
data class RecommendMission(
    val id: Int,
    val title: String,
    val subtitle: String,
    val reward: Int,
    val distanceText: String,
    val badge: RecommendBadge,
    val imageUrl: String? = null   // 서버 대표 이미지 (null 이면 플레이스홀더)
)

class HomeViewModel : ViewModel() {

    init {
        // 홈이 시작 화면이라 여기서 서버 동기화를 한 번 돌린다.
        // (미션 탭에 들어가지 않아도 추천 미션 사진 · 보유 포인트가 실제 값으로 채워지도록)
        viewModelScope.launch {
            runCatching { MissionRepository.refreshMissionsFromServer() }
            runCatching { UserRepository.refreshProfile() }
        }
    }

    // 추천 미션 — 실제 미션 목록에서 몇 개를 뽑아 보여준다.
    // 카드에 실제 mission.id 를 담아, 클릭 시 해당 미션 상세로 이동할 수 있게 한다.
    val recommendedMissions: StateFlow<List<RecommendMission>> =
        MissionRepository.missions
            .map { list ->
                val badges = listOf(
                    RecommendBadge.POPULAR, RecommendBadge.NEW, RecommendBadge.RECOMMEND
                )
                list.take(6).mapIndexed { index, item ->
                    val m = item.mission
                    RecommendMission(
                        id = m.id,
                        title = m.title,
                        subtitle = m.region.ifBlank { m.district }.ifBlank { missionTypeLabel(m.type) },
                        reward = m.reward,
                        distanceText = "",
                        badge = badges[index % badges.size],
                        imageUrl = m.imageUrl
                    )
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    // Repository의 전체 미션 중 "진행 중(IN_PROGRESS)·인증 중"만 걸러서 홈에 보여줌
    // 완료(COMPLETED)된 미션은 진행중 목록에서 제외 → 완료하면 사라진다
    val homeMissions: StateFlow<List<MissionWithState>> =
        MissionRepository.missions
            .map { list ->
                list.filter {
                    it.state == MissionState.IN_PROGRESS ||
                            it.state == MissionState.VERIFYING
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    // 점령률 (실제 미션 완료 기반)
    val occupation: StateFlow<OccupationStat> = MissionRepository.occupation

    // 보유 포인트 (홈 헤더 칩용)
    val points: StateFlow<Int> = UserRepository.points

    // ── 미션 찜 ──

    // 찜 요청 중인 미션 id (하트 중복 클릭 방지)
    val savePending: StateFlow<Set<Int>> = MissionRepository.savedPending

    // 찜 처리 실패 메시지
    private val _saveError = MutableStateFlow<String?>(null)
    val saveError: StateFlow<String?> = _saveError.asStateFlow()

    fun clearSaveError() {
        _saveError.value = null
    }

    // POST/DELETE /api/v1/missions/{id}/saved → 응답의 is_saved 로 화면 갱신
    fun toggleSaved(id: Int) {
        viewModelScope.launch {
            MissionRepository.toggleSavedOnServer(id)
                .onSuccess { UserRepository.refreshProfile() }   // 마이페이지 찜 개수 동기화
                .onFailure { e -> _saveError.value = e.message }
        }
    }

    // ── 미션 인증: 타입별로 서버에 제출 (POST /api/v1/missions/verify) ──

    // IMAGE: 사진을 골랐을 때 → 사진 GPS 확인 후 image + 좌표 전송
    fun onImagePicked(id: Int, context: Context, uri: Uri) {
        val location = readImageLocation(context, uri)
        if (location == null) {
            MissionRepository.setError(id, "이 사진에는 위치정보가 없어요. 위치 기록을 켜고 찍은 사진을 올려주세요.")
            return
        }
        viewModelScope.launch {
            MissionRepository.setVerifying(id)
            val imageUrl = MissionRepository.uploadImage(context, uri)
                .getOrElse { error ->
                    MissionRepository.setError(
                        id,
                        error.message ?: "사진 업로드에 실패했습니다."
                    )
                    return@launch
                }
            submitVerification(
                id,
                MissionVerifyRequestDto(
                    missionId = id,
                    missionType = MissionType.IMAGE_LOCATION.toServerType(),
                    imageUrl = imageUrl,
                    latitude = location.latitude,
                    longitude = location.longitude
                )
            )
        }
    }

    // CURRENT_LOCATION: 위치 권한 허락 → 현재 위도/경도 전송
    fun onLocationPermissionGranted(id: Int, context: Context) {
        viewModelScope.launch {
            MissionRepository.setVerifying(id)
            val location = getCurrentLocation(context)
            if (location == null) {
                MissionRepository.setError(id, "위치를 가져오지 못했어요. 야외에서 다시 시도해주세요.")
                return@launch
            }
            submitVerification(
                id,
                MissionVerifyRequestDto(
                    missionId = id,
                    missionType = MissionType.CURRENT_LOCATION.toServerType(),
                    latitude = location.latitude,
                    longitude = location.longitude
                )
            )
        }
    }

    fun onLocationPermissionDenied(id: Int) {
        MissionRepository.setError(id, "위치 권한이 있어야 이 미션을 완료할 수 있어요.")
    }

    // RECEIPT: 영수증 촬영 완료 → receipt_image_url 전송
    fun onReceiptCaptured(id: Int, context: Context, success: Boolean, uri: Uri?) {
        if (!success || uri == null) return
        viewModelScope.launch {
            MissionRepository.setVerifying(id)
            val receiptImageUrl = MissionRepository.uploadImage(context, uri)
                .getOrElse { error ->
                    MissionRepository.setError(
                        id,
                        error.message ?: "영수증 업로드에 실패했습니다."
                    )
                    return@launch
                }
            submitVerification(
                id,
                MissionVerifyRequestDto(
                    missionId = id,
                    missionType = MissionType.RECEIPT.toServerType(),
                    receiptImageUrl = receiptImageUrl
                )
            )
        }
    }

    fun onCameraPermissionDenied(id: Int) {
        MissionRepository.setError(id, "카메라 권한이 있어야 영수증을 촬영할 수 있어요.")
    }

    // 공통: 서버 제출 → 성공이면 완료 처리, 실패면 에러 표시 후 진행 중으로 복귀
    private suspend fun submitVerification(id: Int, request: MissionVerifyRequestDto) {
        MissionRepository.verifyOnServer(request)
            .onSuccess { completeMission(id) }
            .onFailure { e ->
                MissionRepository.setError(id, e.message ?: "인증에 실패했습니다.")
            }
    }

    // 완료 처리 + 포인트 적립
    private fun completeMission(id: Int) {
        val reward = MissionRepository.missions.value
            .firstOrNull { it.mission.id == id }?.mission?.reward ?: 0
        MissionRepository.setCompleted(id)
        // 즉시 반영 후, 서버 점수를 기준으로 다시 맞춘다
        UserRepository.addPoints(reward)
        viewModelScope.launch { UserRepository.refreshProfile() }
    }
}
