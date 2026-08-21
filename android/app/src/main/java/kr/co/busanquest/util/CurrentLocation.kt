package kr.co.busanquest.util

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.math.round

/**
 * 지금 내 위치와 그 정확도.
 *
 * accuracyM 은 "이 좌표가 반경 몇 m 안에 있다"는 신뢰 반경(미터)이다.
 * 값이 클수록 부정확하다. 서버가 인증을 판정할 때 이 값을 함께 본다.
 */
data class LocationFix(
    val latitude: Double,
    val longitude: Double,
    val accuracyM: Double
)

/**
 * 서버가 허용하는 기본 정확도(미터).
 * 이보다 부정확하면 서버가 인증을 거절하므로, 보내기 전에 앱에서 먼저 걸러 안내한다.
 */
const val ACCURACY_LIMIT_M = 100.0

/**
 * 현재 위치를 가져온다. 못 얻으면 null.
 *
 * 정확도가 없는 좌표(hasAccuracy = false)도 null 로 본다.
 * 서버가 accuracy_m 없는 요청을 거절하므로 보내봐야 실패하기 때문이다.
 */
@SuppressLint("MissingPermission")  // 권한 확인은 화면에서 하므로 경고 끔
suspend fun getCurrentLocation(context: Context): LocationFix? =
    suspendCancellableCoroutine { cont ->
        val client = LocationServices.getFusedLocationProviderClient(context)
        client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location ->
                cont.resume(
                    if (location != null && location.hasAccuracy()) {
                        LocationFix(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            // Float -> Double 을 그대로 넘기면 12.34f 가 12.340000152587891 이 된다.
                            // 소수 한 자리로 정리해 보낸다.
                            accuracyM = round(location.accuracy.toDouble() * 10) / 10
                        )
                    } else null
                )
            }
            .addOnFailureListener { cont.resume(null) }
    }
