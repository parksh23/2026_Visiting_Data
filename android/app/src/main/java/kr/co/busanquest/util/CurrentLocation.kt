package kr.co.busanquest.util

import android.annotation.SuppressLint
import android.content.Context
import androidx.core.location.LocationCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * 지금 내 위치와 그 정확도.
 *
 * accuracyM 은 "이 좌표가 반경 몇 m 안에 있다"는 신뢰 반경(미터)이다.
 * 위치는 기기 메모리에서만 검사하며 API 요청으로 직렬화하지 않는다.
 */
data class LocationFix(
    val latitude: Double,
    val longitude: Double,
    val accuracyM: Double,
    val elapsedRealtimeNanos: Long,
    val isMock: Boolean,
)

/**
 * 현재 위치를 가져온다. 못 얻으면 null.
 *
 * 정확도가 없는 좌표(hasAccuracy = false)도 null 로 본다.
 */
@SuppressLint("MissingPermission")  // 권한 확인은 화면에서 하므로 경고 끔
suspend fun getCurrentLocation(context: Context): LocationFix? = withTimeoutOrNull(20_000) {
    suspendCancellableCoroutine { cont ->
        val cancellation = CancellationTokenSource()
        cont.invokeOnCancellation { cancellation.cancel() }
        val client = LocationServices.getFusedLocationProviderClient(context)
        val request = CurrentLocationRequest.Builder()
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setMaxUpdateAgeMillis(0).setDurationMillis(15_000).build()
        try {
            client.getCurrentLocation(request, cancellation.token)
                .addOnSuccessListener { location ->
                    if (cont.isActive) cont.resume(
                        if (location != null && location.hasAccuracy()) {
                            LocationFix(
                                latitude = location.latitude,
                                longitude = location.longitude,
                                accuracyM = location.accuracy.toDouble(),
                                elapsedRealtimeNanos = location.elapsedRealtimeNanos,
                                isMock = LocationCompat.isMock(location),
                            )
                        } else null
                    )
                }
                .addOnFailureListener { if (cont.isActive) cont.resume(null) }
                .addOnCanceledListener { if (cont.isActive) cont.resume(null) }
        } catch (_: SecurityException) {
            if (cont.isActive) cont.resume(null)
        }
    }
}
