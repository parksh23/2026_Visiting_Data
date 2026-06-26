package com.example.busasnquest.util

import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface

// 사진에서 읽어낸 위치
data class PhotoLatLng(
    val latitude: Double,
    val longitude: Double
)

// 사진(Uri)에서 GPS 좌표를 꺼낸다. 위치정보가 없으면 null.
fun readPhotoLocation(context: Context, uri: Uri): PhotoLatLng? {
    return try {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val exif = ExifInterface(stream)
            val latLng = exif.latLong  // [위도, 경도] 또는 null
            if (latLng != null) {
                PhotoLatLng(latLng[0], latLng[1])
            } else {
                null  // 위치정보가 없는 사진
            }
        }
    } catch (e: Exception) {
        null
    }
}