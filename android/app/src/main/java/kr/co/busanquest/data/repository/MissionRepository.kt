package kr.co.busanquest.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kr.co.busanquest.data.model.MissionState
import kr.co.busanquest.data.model.MissionType
import kr.co.busanquest.data.model.OngoingMission
import kr.co.busanquest.data.model.toMissionTypeOrNull
import kr.co.busanquest.data.remote.DistrictStatusDto
import kr.co.busanquest.data.remote.MissionDto
import kr.co.busanquest.data.remote.RetrofitInstance
import kr.co.busanquest.data.remote.SimpleResultDto
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
import kr.co.busanquest.data.remote.MissionVerifyRequestDto
import kr.co.busanquest.data.remote.ErrorDetailDto
import com.google.gson.Gson
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

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


// 부산 16개 구·군 전체 (15구 + 기장군) — 그리드 히트맵의 기준 목록
val ALL_BUSAN_DISTRICTS = listOf(
    "강서구", "북구", "금정구", "기장군",
    "사상구", "부산진구", "동래구", "해운대구",
    "사하구", "서구", "연제구", "수영구",
    "중구", "동구", "남구", "영도구"
)

// 앱 전체에서 미션을 관리하는 단일 진실 공급원
object MissionRepository {
    private const val MAX_IMAGE_DIMENSION = 2048
    private const val MAX_UPLOAD_BYTES = 5 * 1024 * 1024

    // StateFlow를 stateIn으로 만들 때 필요한 CoroutineScope
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // 서버 시드와 같은 규칙의 임시 대표 이미지 (서버 연결 전 폴백)
    private const val FALLBACK_IMAGE_URL =
        "https://picsum.photos/seed/busan-quest-%d/1280/720"

    // 전체 미션 목록 기본값
    // 서버 연결 전에도 앱이 빈 화면이 되지 않도록 임시 미션을 가지고 있음
    private val allMissions = listOf(
        // 해운대구
        OngoingMission(1, "해운대 해수욕장 인증샷", "해운대구", 100, 0, 1, MissionType.IMAGE_LOCATION, "해운대구"),
        OngoingMission(2, "동백섬 산책로 걷기", "해운대구", 80, 0, 1, MissionType.CURRENT_LOCATION, "해운대구"),
        OngoingMission(3, "해운대 맛집에서 식사", "해운대구", 150, 0, 1, MissionType.RECEIPT, "해운대구"),

        // 수영구
        OngoingMission(4, "광안리 해변 인증샷", "수영구", 100, 0, 1, MissionType.IMAGE_LOCATION, "수영구"),
        OngoingMission(5, "광안대교 야경 보기", "수영구", 80, 0, 1, MissionType.CURRENT_LOCATION, "수영구"),

        // 중구
        OngoingMission(6, "남포동 맛집에서 식사", "중구", 150, 0, 1, MissionType.RECEIPT, "중구"),
        OngoingMission(7, "용두산공원 방문", "중구", 80, 0, 1, MissionType.CURRENT_LOCATION, "중구"),
        OngoingMission(8, "자갈치시장 구경", "중구", 100, 0, 1, MissionType.IMAGE_LOCATION, "중구")
    ).map { it.copy(imageUrl = it.imageUrl ?: FALLBACK_IMAGE_URL.format(it.id)) }

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


    // ───────────────── 미션 찜 ─────────────────

    // GET /api/v1/missions/saved 결과 (최근 찜한 순)
    private val _savedMissions = MutableStateFlow<List<MissionWithState>>(emptyList())
    val savedMissions: StateFlow<List<MissionWithState>> = _savedMissions.asStateFlow()

    // 요청 중인 미션 id 목록 — 하트를 연속으로 눌러도 중복 요청이 나가지 않게 한다
    private val _savedPending = MutableStateFlow<Set<Int>>(emptySet())
    val savedPending: StateFlow<Set<Int>> = _savedPending.asStateFlow()

    /**
     * ⚠️ 이 두 함수는 절대 suspend 로 만들지 말 것.
     *
     * 코루틴이 취소된 뒤에는 finally 안의 suspend 호출이 즉시 CancellationException 을 던진다.
     * 예전 구현은 Mutex.withLock(suspend) 을 썼는데, 요청 중에 화면을 벗어나
     * viewModelScope 이 취소되면 endSavedRequest 가 실행되지 못하고
     * 해당 미션 id 가 savedPending 에 영원히 남았다 → 돌아왔을 때 하트가 계속 잠김.
     *
     * MutableStateFlow.update 는 CAS 기반이라 락 없이도 원자적이다.
     */
    private fun beginSavedRequest(id: Int): Boolean {
        var accepted = false
        _savedPending.update { current ->
            accepted = !current.contains(id)
            if (accepted) current + id else current
        }
        return accepted
    }

    private fun endSavedRequest(id: Int) {
        _savedPending.update { it - id }
    }

    fun isSaved(id: Int): Boolean =
        _missions.value.firstOrNull { it.mission.id == id }?.saved
            ?: _savedMissions.value.any { it.mission.id == id }

    /**
     * 찜 추가/해제. POST·DELETE /api/v1/missions/{mission_id}/saved
     *
     * - 응답의 is_saved 값을 그대로 화면 상태로 반영한다(서버가 진실).
     * - 같은 미션에 대한 요청이 이미 진행 중이면 무시한다(중복 클릭 방지).
     */
    suspend fun setSavedOnServer(missionId: Int, saved: Boolean): Result<Boolean> {
        if (!beginSavedRequest(missionId)) {
            // 이미 요청이 날아가 있는 상태 → 무시하고 현재 값을 그대로 돌려준다
            return Result.success(isSaved(missionId))
        }
        // Repository 자체 scope 에서 실행한다.
        // 호출한 화면이 사라져 viewModelScope 이 취소돼도 요청은 끝까지 보내고
        // pending 도 반드시 해제된다 (서버엔 반영됐는데 앱만 모르는 상태 방지).
        return scope.async {
            try {
                val response = if (saved) {
                    RetrofitInstance.api.addSavedMission(missionId)
                } else {
                    RetrofitInstance.api.removeSavedMission(missionId)
                }
                applySavedResult(missionId, response.isSaved)
                Result.success(response.isSaved)
            } catch (e: Exception) {
                Result.failure(Exception(e.toSavedErrorMessage()))
            } finally {
                endSavedRequest(missionId)
            }
        }.await()
    }

    // 현재 상태의 반대로 토글
    suspend fun toggleSavedOnServer(missionId: Int): Result<Boolean> =
        setSavedOnServer(missionId, !isSaved(missionId))

    // 서버 응답(is_saved)을 전체 미션 목록과 찜 목록에 반영
    private fun applySavedResult(missionId: Int, saved: Boolean) {
        updateMission(missionId) { it.copy(saved = saved) }

        _savedMissions.update { list ->
            if (saved) {
                val target = _missions.value.firstOrNull { it.mission.id == missionId }
                    ?: list.firstOrNull { it.mission.id == missionId }
                // 최근 찜한 미션이 맨 앞
                if (target == null) list
                else listOf(target.copy(saved = true)) + list.filter { it.mission.id != missionId }
            } else {
                list.filter { it.mission.id != missionId }
            }
        }
    }

    // GET /api/v1/missions/saved → 찜 목록 새로고침 + 전체 목록의 하트 상태 동기화
    suspend fun refreshSavedMissionsFromServer() {
        val saved = RetrofitInstance.api.getSavedMissions()

        _savedMissions.value = saved.map { dto ->
            MissionWithState(
                mission = dto.toOngoingMission(),
                state = dto.status.toMissionState(),
                saved = true
            )
        }

        val savedIds = saved.map { it.missionId }.toSet()
        _missions.update { list ->
            list.map { it.copy(saved = savedIds.contains(it.mission.id)) }
        }
    }

    // 서버 오류 → 사용자에게 보여줄 문구
    private fun Throwable.toSavedErrorMessage(): String = when (this) {
        is retrofit2.HttpException -> when (code()) {
            401 -> "로그인이 필요해요. 다시 로그인해주세요."
            403 -> "이용이 제한된 계정이에요."
            404 -> "미션을 찾을 수 없어요."
            422 -> serverDetail() ?: "요청 정보를 확인해주세요."
            in 500..599 -> "서버에 문제가 생겼어요. 잠시 후 다시 시도해주세요."
            else -> serverDetail() ?: "찜 처리에 실패했어요. (${code()})"
        }
        is java.io.IOException -> "네트워크 연결을 확인해주세요."
        else -> "찜 처리 중 오류가 발생했어요."
    }

    // 오류 응답 본문의 {"detail": "..."} 추출
    private fun retrofit2.HttpException.serverDetail(): String? = try {
        response()?.errorBody()?.string()
            ?.let { Gson().fromJson(it, ErrorDetailDto::class.java) }
            ?.detail
            ?.takeIf { it.isNotBlank() }
    } catch (_: Exception) {
        null
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



    // ───────────────── 미션 진행 상태 (시작 / 취소) ─────────────────

    // 요청 중인 미션 id — 버튼을 연타해도 중복 요청이 나가지 않게 한다
    private val _statePending = MutableStateFlow<Set<Int>>(emptySet())
    val statePending: StateFlow<Set<Int>> = _statePending.asStateFlow()

    // ⚠️ 위 beginSavedRequest/endSavedRequest 와 같은 이유로 suspend 로 만들지 않는다.
    private fun beginStateRequest(id: Int): Boolean {
        var accepted = false
        _statePending.update { current ->
            accepted = !current.contains(id)
            if (accepted) current + id else current
        }
        return accepted
    }

    private fun endStateRequest(id: Int) {
        _statePending.update { it - id }
    }

    /**
     * 도전 시작. POST /api/v1/missions/{mission_id}/start
     *
     * 서버가 성공을 돌려준 뒤에야 화면 상태를 진행 중으로 바꾼다.
     * 서버 USER_MISSIONS 에 ongoing 으로 남으므로 앱을 껐다 켜도 유지된다.
     */
    suspend fun startMissionOnServer(id: Int): Result<Unit> =
        changeMissionState(id, MissionState.IN_PROGRESS) {
            RetrofitInstance.api.startMission(id)
        }

    /**
     * 도전 취소. POST /api/v1/missions/{mission_id}/cancel
     *
     * 서버에서 진행 기록이 지워지고 다음 조회부터 not_started 로 내려온다.
     * 이미 완료한 미션은 서버가 거부한다.
     */
    suspend fun cancelMissionOnServer(id: Int): Result<Unit> =
        changeMissionState(id, MissionState.NOT_STARTED) {
            RetrofitInstance.api.cancelMission(id)
        }

    /**
     * 시작/취소 공통 처리.
     * 요청은 Repository 자체 scope 에서 돌린다 — 호출한 화면이 사라져도
     * 요청은 끝까지 보내고 pending 도 반드시 해제된다(찜과 같은 이유).
     */
    private suspend fun changeMissionState(
        id: Int,
        newState: MissionState,
        request: suspend () -> SimpleResultDto
    ): Result<Unit> {
        // 이미 같은 미션 요청이 날아가 있으면 무시한다
        if (!beginStateRequest(id)) return Result.success(Unit)

        return scope.async {
            try {
                val response = request()
                if (response.success == false) {
                    // 200 이지만 서버가 거절 (예: 이미 완료한 미션 취소)
                    Result.failure(
                        Exception(
                            response.message?.takeIf { it.isNotBlank() }
                                ?: "요청을 처리하지 못했습니다."
                        )
                    )
                } else {
                    updateMission(id) { it.copy(state = newState, error = null) }
                    Result.success(Unit)
                }
            } catch (e: Exception) {
                Result.failure(Exception(e.toMissionStateErrorMessage()))
            } finally {
                endStateRequest(id)
            }
        }.await()
    }

    // 서버 오류 → 사용자에게 보여줄 문구
    private fun Throwable.toMissionStateErrorMessage(): String = when (this) {
        is retrofit2.HttpException -> when (code()) {
            400, 409 -> serverDetail() ?: "이미 처리된 미션이에요."
            401 -> "로그인이 필요해요. 다시 로그인해주세요."
            404 -> "미션을 찾을 수 없어요."
            else -> serverDetail() ?: "요청을 처리하지 못했습니다. (${code()})"
        }
        is java.io.IOException -> "네트워크 연결을 확인해주세요."
        else -> "미션 상태를 바꾸는 중 오류가 발생했어요."
    }


    /**
     * 미션 인증을 서버로 제출한다. POST /api/v1/missions/verify
     *
     * 타입별로 채워야 하는 필드 (MissionVerifyRequestDto):
     * - CURRENT_LOCATION → latitude, longitude
     * - IMAGE            → image (+ 사진의 GPS 좌표도 함께 전송)
     * - RECEIPT          → receipt_image_url
     *
     * 성공/실패를 Result 로 돌려주고, 상태 변경(setCompleted/setError)은
     * 호출한 쪽(HomeViewModel)에서 처리한다.
     */
    suspend fun verifyOnServer(request: MissionVerifyRequestDto): Result<String> {
        return try {
            val response = RetrofitInstance.api.verifyMission(request)
            if (response.success) {
                Result.success(response.message)
            } else {
                // 200 이지만 서버가 인증 거절 (예: 위치가 미션 장소와 다름)
                Result.failure(Exception(response.message.ifBlank { "인증에 실패했습니다." }))
            }
        } catch (e: retrofit2.HttpException) {
            Result.failure(Exception("인증 요청이 실패했습니다. (${e.code()})"))
        } catch (e: java.io.IOException) {
            Result.failure(Exception("네트워크 연결을 확인해주세요."))
        } catch (e: Exception) {
            Result.failure(Exception("인증 처리 중 오류가 발생했습니다."))
        }
    }

    /**
     * 이미지 선택기/카메라의 content:// URI를 서버가 접근 가능한 HTTP URL로 변환한다.
     * 서버 업로드 규격에 맞춰 이미지를 최대 2048px JPEG로 변환하고 5MB 이하로 압축한다.
     */
    suspend fun uploadImage(context: Context, uri: Uri): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                // ⚠️ inJustDecodeBounds=true 이면 decodeStream 은 항상 null 을 반환한다.
                // 따라서 "열기 성공" 여부는 decodeStream 결과가 아니라
                // openInputStream 자체가 null 인지로 판단해야 한다.
                (context.contentResolver.openInputStream(uri)
                    ?: return@withContext Result.failure(
                        IllegalArgumentException("선택한 사진을 열 수 없습니다.")
                    )).use { stream ->
                    BitmapFactory.decodeStream(stream, null, options)
                }

                if (options.outWidth <= 0 || options.outHeight <= 0) {
                    return@withContext Result.failure(
                        IllegalArgumentException("지원하지 않는 이미지 형식입니다.")
                    )
                }

                var sampleSize = 1
                while (
                    options.outWidth / sampleSize > MAX_IMAGE_DIMENSION ||
                    options.outHeight / sampleSize > MAX_IMAGE_DIMENSION
                ) {
                    sampleSize *= 2
                }

                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                }
                val bitmap = context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, decodeOptions)
                } ?: return@withContext Result.failure(
                    IllegalArgumentException("선택한 사진을 처리할 수 없습니다.")
                )

                val bytes = bitmap.toJpegBytes()
                bitmap.recycle()
                if (bytes.size > MAX_UPLOAD_BYTES) {
                    return@withContext Result.failure(
                        IllegalArgumentException("사진을 5MB 이하로 줄이지 못했습니다.")
                    )
                }

                val requestBody = bytes.toRequestBody("image/jpeg".toMediaType())
                val part = MultipartBody.Part.createFormData(
                    "file",
                    "mission-${System.currentTimeMillis()}.jpg",
                    requestBody
                )
                Result.success(RetrofitInstance.api.uploadImage(part).url)
            } catch (e: retrofit2.HttpException) {
                Result.failure(Exception("사진 업로드에 실패했습니다. (${e.code()})"))
            } catch (e: java.io.IOException) {
                Result.failure(Exception("사진 업로드 중 네트워크 연결을 확인해주세요."))
            } catch (e: Exception) {
                Result.failure(Exception(e.message ?: "사진을 처리하지 못했습니다."))
            }
        }

    private fun Bitmap.toJpegBytes(): ByteArray {
        var quality = 90
        var bytes: ByteArray
        do {
            bytes = ByteArrayOutputStream().use { output ->
                compress(Bitmap.CompressFormat.JPEG, quality, output)
                output.toByteArray()
            }
            quality -= 10
        } while (bytes.size > MAX_UPLOAD_BYTES && quality >= 50)
        return bytes
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
                state = dto.status.toMissionState(),
                saved = dto.isSaved          // 서버 기준 찜 상태 = 하트 초기값
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
    // 아직 서버 데이터가 없으면 미션 목록 기준으로 직접 계산.
    // 어느 쪽이든 부산 16개 구·군 전체로 확장해서 반환 (그리드 히트맵용 — 데이터 없는 구는 0/0)
    val districtProgress: StateFlow<List<DistrictMissionProgress>> =
        combine(
            _missions,
            _serverDistrictProgress
        ) { missionList, serverDistricts ->

            val known = serverDistricts ?: missionList
                .groupBy { it.mission.district }
                .map { (district, missions) ->
                    DistrictMissionProgress(
                        name = district,
                        completed = missions.count { it.state == MissionState.COMPLETED },
                        total = missions.size
                    )
                }

            val byName = known.associateBy { it.name }
            ALL_BUSAN_DISTRICTS.map { name ->
                byName[name] ?: DistrictMissionProgress(name = name, completed = 0, total = 0)
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
        imageUrl: String? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        accuracyM: Double? = null,
        receiptImageUrl: String? = null
    ): Boolean {
        // 앱에서 받은 값을 서버 요청 DTO로 변환
        val request = MissionVerifyRequestDto(
            missionId = missionId,
            missionType = missionType,
            imageUrl = imageUrl,
            latitude = latitude,
            longitude = longitude,
            accuracyM = accuracyM,
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
    // 서버가 주는 district 를 우선 사용하고, 비어 있으면 location 첫 단어로 폴백
    val resolvedDistrict = district.ifBlank { location.extractDistrict() }
    return OngoingMission(
        id = missionId,
        title = title,
        region = location.ifBlank { resolvedDistrict },  // location 비면 구 이름이라도 표시
        reward = rewardPoints,
        current = progressCurrent,
        total = progressTotal,
        type = missionType.toMissionType(),
        district = resolvedDistrict,
        lat = latitude,
        lng = longitude,
        imageUrl = imageUrl,
        serverType = missionType     // 인증 제출 때 그대로 돌려보내기 위해 원문 보관
    )
}


// 서버에서 받은 mission_type 문자열을 앱 내부 MissionType으로 변환
// 과거 표기("IMAGE" 등) 호환은 MissionType.kt 의 toMissionTypeOrNull 이 맡는다.
private fun String.toMissionType(): MissionType =
    toMissionTypeOrNull() ?: MissionType.CURRENT_LOCATION


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
