package com.example.busasnquest.util

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.Granularity
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

data class DeviceLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float
)

// 현재 위치를 가져온다. 못 얻으면 null.
@SuppressLint("MissingPermission")  // 권한 확인은 화면에서 하므로 경고 끔
suspend fun getCurrentLocation(context: Context): DeviceLocation? =
    withTimeoutOrNull(17_000) {
        suspendCancellableCoroutine { cont ->
        val client = LocationServices.getFusedLocationProviderClient(context)
        val cancellation = CancellationTokenSource()
        val request = CurrentLocationRequest.Builder()
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setGranularity(Granularity.GRANULARITY_FINE)
            .setMaxUpdateAgeMillis(5_000)
            .setDurationMillis(15_000)
            .build()
        cont.invokeOnCancellation { cancellation.cancel() }
        client.getCurrentLocation(request, cancellation.token)
            .addOnSuccessListener { location ->
                if (!cont.isActive) return@addOnSuccessListener
                if (location != null && location.latitude in -90.0..90.0 &&
                    location.longitude in -180.0..180.0
                ) {
                    cont.resume(
                        DeviceLocation(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            accuracyMeters = location.accuracy.coerceAtLeast(0f)
                        )
                    )
                } else {
                    cont.resume(null)
                }
            }
            .addOnFailureListener {
                if (cont.isActive) cont.resume(null)
            }
        }
    }
