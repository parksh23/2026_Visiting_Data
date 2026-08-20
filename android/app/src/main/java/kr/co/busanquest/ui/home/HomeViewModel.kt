package kr.co.busanquest.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kr.co.busanquest.data.model.MissionState
import kr.co.busanquest.data.model.MissionType
import kr.co.busanquest.data.model.toServerType
import kr.co.busanquest.data.remote.MissionVerifyRequestDto
import kr.co.busanquest.data.repository.MissionRepository
import kr.co.busanquest.data.repository.MissionWithState
import kr.co.busanquest.data.repository.UserRepository
import kr.co.busanquest.util.Notifier
import kr.co.busanquest.util.getCurrentLocation
import kr.co.busanquest.util.readImageLocation
import android.content.Context
import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import kr.co.busanquest.data.repository.OccupationStat

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
            // 서버 목록을 받은 뒤 "이전에 못 보던 미션"이 있으면 알림
            Notifier.checkNewMissions(MissionRepository.missions.value.map { it.mission })
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

    // 인사말용 닉네임 (서버 users/me 기준). 아직 못 불러왔으면 빈 문자열.
    val name: StateFlow<String> = UserRepository.name

    // ── 미션 찜 ──

    // 찜 요청 중인 미션 id (하트 중복 클릭 방지)
    val savePending: StateFlow<Set<Int>> = MissionRepository.savedPending

    // 찜 처리 실패 메시지
    private val _saveError = MutableStateFlow<String?>(null)
    val saveError: StateFlow<String?> = _saveError.asStateFlow()

    fun clearSaveError() {
        _saveError.value = null
    }

    // ── 미션 시작 / 취소 (POST /api/v1/missions/{id}/start · /cancel) ──

    // 시작·취소 요청 중인 미션 id (버튼 잠금용)
    val statePending: StateFlow<Set<Int>> = MissionRepository.statePending

    // 서버가 성공을 준 뒤에야 상태가 '진행 중'으로 바뀐다
    fun startMission(id: Int) {
        viewModelScope.launch {
            MissionRepository.startMissionOnServer(id)
                .onFailure { e -> _saveError.value = e.message }
        }
    }

    // 성공하면 '시작 전'으로 롤백된다. 이미 완료한 미션은 서버가 거부한다.
    fun cancelMission(id: Int) {
        viewModelScope.launch {
            MissionRepository.cancelMissionOnServer(id)
                .onFailure { e -> _saveError.value = e.message }
        }
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
                    missionType = serverTypeOf(id, MissionType.IMAGE_LOCATION),
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
                    missionType = serverTypeOf(id, MissionType.CURRENT_LOCATION),
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
                    missionType = serverTypeOf(id, MissionType.RECEIPT),
                    receiptImageUrl = receiptImageUrl
                )
            )
        }
    }

    fun onCameraPermissionDenied(id: Int) {
        MissionRepository.setError(id, "카메라 권한이 있어야 영수증을 촬영할 수 있어요.")
    }

    /**
     * 서버에 보낼 mission_type.
     *
     * 서버는 요청한 타입이 DB 값과 정확히 같은지 검사한다. 앱이 자체 문자열을 만들어 보내면
     * 서버 표기가 바뀔 때마다("PHOTO" ↔ "IMAGE") 인증이 거절되므로,
     * 서버가 내려준 원문(serverType)을 그대로 되돌려 보낸다.
     * 로컬 샘플 데이터라 원문이 없으면 앱 기본값으로 폴백한다.
     */
    private fun serverTypeOf(missionId: Int, fallback: MissionType): String =
        MissionRepository.missions.value
            .firstOrNull { it.mission.id == missionId }
            ?.mission?.serverType
            ?.takeIf { it.isNotBlank() }
            ?: fallback.toServerType()

    // 공통: 서버 제출 → 성공이면 완료 처리, 실패면 에러 표시 후 진행 중으로 복귀
    private suspend fun submitVerification(id: Int, request: MissionVerifyRequestDto) {
        val mission = MissionRepository.missions.value
            .firstOrNull { it.mission.id == id }?.mission
        val title = mission?.title ?: "미션"

        MissionRepository.verifyOnServer(request)
            .onSuccess {
                completeMission(id)
                // 인증은 시간이 걸려 사용자가 다른 화면에 가 있을 수 있다 → 결과를 알림으로
                Notifier.missionResult(
                    missionId = id,
                    title = title,
                    reward = mission?.reward ?: 0,
                    success = true
                )
            }
            .onFailure { e ->
                val message = e.message ?: "인증에 실패했습니다."
                MissionRepository.setError(id, message)
                Notifier.missionResult(
                    missionId = id,
                    title = title,
                    reward = 0,
                    success = false,
                    reason = message
                )
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
