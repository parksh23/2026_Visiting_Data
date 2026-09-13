package kr.co.busanquest.util

import android.content.Context
import android.net.Uri
import android.os.SystemClock
import android.util.Base64
import com.google.android.gms.tasks.Task
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.StandardIntegrityManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kr.co.busanquest.data.remote.MissionVerifyRequestDto
import kr.co.busanquest.data.remote.RetrofitInstance
import kr.co.busanquest.data.repository.MissionRepository

object LocationProof {
    private val providerMutex = Mutex()
    private var provider: StandardIntegrityManager.StandardIntegrityTokenProvider? = null
    private var providerProject = 0L
    private var preparedAt = 0L

    private suspend fun tokenProvider(context: Context, project: Long) = providerMutex.withLock {
        if (provider == null || providerProject != project ||
            SystemClock.elapsedRealtime() - preparedAt > 3_600_000) {
            provider = IntegrityManagerFactory.createStandard(context.applicationContext)
                .prepareIntegrityToken(StandardIntegrityManager.PrepareIntegrityTokenRequest.builder()
                    .setCloudProjectNumber(project).build()).awaitResult()
            providerProject = project
            preparedAt = SystemClock.elapsedRealtime()
        }
        requireNotNull(provider)
    }

    suspend fun create(context: Context, missionId: Int, photo: Uri?): MissionVerifyRequestDto = withTimeout(240_000) {
        val challenge = RetrofitInstance.api.createLocationChallenge(missionId)
        require(challenge.protocolVersion == 1 && challenge.missionId == missionId &&
            challenge.missionType == (if (photo == null) "CURRENT_LOCATION" else "PHOTO")) {
            "미션 인증 방식이 변경되었습니다. 미션을 새로 불러와주세요."
        }
        require(challenge.cloudProjectNumber > 0) { "앱 보안 확인이 준비되지 않았습니다." }
        val integrityProvider = try {
            withTimeout(60_000) { tokenProvider(context, challenge.cloudProjectNumber) }
        } catch (e: CancellationException) { throw e
        } catch (_: Exception) { error("앱 보안 확인에 실패했습니다. Play 스토어 설치본으로 다시 시도해주세요.") }
        var previousNanos = 0L
        repeat(2) { index ->
            if (index > 0) delay(2_000)
            val fix = getCurrentLocation(context) ?: error("현재 위치를 확인하지 못했습니다. 위치 권한과 GPS를 확인해주세요.")
            val failure = LocalLocationPolicy.failure(fix, challenge.latitude, challenge.longitude,
                challenge.radiusM, challenge.maxAccuracyM, SystemClock.elapsedRealtimeNanos())
            check(failure == null) { failure.orEmpty() }
            check(fix.elapsedRealtimeNanos > previousNanos) { "새로운 위치를 확인하지 못했습니다. 다시 시도해주세요." }
            previousNanos = fix.elapsedRealtimeNanos
        }
        val photoUrl = photo?.let {
            val gps = readImageLocation(context, it) ?: error("이 사진에는 위치정보가 없습니다. 위치 기록을 켜고 촬영해주세요.")
            check(LocalLocationPolicy.validCoordinate(gps.latitude, gps.longitude) &&
                LocalLocationPolicy.distanceM(gps.latitude, gps.longitude, challenge.latitude, challenge.longitude) <= challenge.radiusM) {
                "사진의 촬영 위치가 미션 장소와 다릅니다."
            }
            // Fresh JPEG from pixels; original GPS/EXIF is not uploaded.
            MissionRepository.uploadImage(context, it).getOrThrow()
        }
        val request = MissionVerifyRequestDto(missionId = missionId, missionType = challenge.missionType,
            imageUrl = photoUrl, challengeId = challenge.challengeId, localPassed = true)
        val hash = Base64.encodeToString(VerificationRequestHash.bytes(request), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        try {
            val token = withTimeout(60_000) { integrityProvider.request(
                StandardIntegrityManager.StandardIntegrityTokenRequest.builder().setRequestHash(hash).build()
            ).awaitResult().token() }
            request.copy(integrityToken = token)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            providerMutex.withLock { provider = null }
            error("앱 보안 확인에 실패했습니다. Play 스토어 설치본으로 다시 시도해주세요.")
        }
    }
}

private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { if (cont.isActive) cont.resume(it) }
    addOnFailureListener { if (cont.isActive) cont.resumeWithException(it) }
    addOnCanceledListener { cont.cancel() }
}
