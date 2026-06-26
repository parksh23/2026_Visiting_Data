package com.example.busasnquest.util

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

// 현재 위치를 가져온다. 못 얻으면 null.
@SuppressLint("MissingPermission")  // 권한 확인은 화면에서 하므로 경고 끔
suspend fun getCurrentLocation(context: Context): PhotoLatLng? =
    suspendCancellableCoroutine { cont ->
        val client = LocationServices.getFusedLocationProviderClient(context)
        client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location ->
                if (location != null) {
                    cont.resume(PhotoLatLng(location.latitude, location.longitude))
                } else {
                    cont.resume(null)
                }
            }
            .addOnFailureListener {
                cont.resume(null)
            }
    }