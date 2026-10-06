package kr.co.busanquest.util

import kotlin.math.*

object LocalLocationPolicy {
    fun validCoordinate(lat: Double, lng: Double): Boolean =
        lat.isFinite() && lng.isFinite() && lat in -90.0..90.0 && lng in -180.0..180.0

    fun distanceM(lat: Double, lng: Double, targetLat: Double, targetLng: Double): Double {
        val dLat = Math.toRadians(targetLat - lat)
        val dLng = Math.toRadians(targetLng - lng)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat)) *
            cos(Math.toRadians(targetLat)) * sin(dLng / 2).pow(2)
        return 6_371_000.0 * 2 * asin(sqrt(a.coerceIn(0.0, 1.0)))
    }

    fun failure(fix: LocationFix, targetLat: Double, targetLng: Double, radiusM: Double,
        maxAccuracyM: Double, nowNanos: Long): String? {
        if (!validCoordinate(targetLat, targetLng) || !radiusM.isFinite() || radiusM <= 0 ||
            !maxAccuracyM.isFinite() || maxAccuracyM <= 0) return "미션 인증 조건을 확인하지 못했습니다."
        if (fix.isMock) return "모의 위치가 감지되었습니다. 위치 조작 기능을 끄고 다시 시도해주세요."
        if (!validCoordinate(fix.latitude, fix.longitude) || !fix.accuracyM.isFinite() ||
            fix.accuracyM <= 0 || fix.accuracyM > maxAccuracyM) return "위치 정확도가 낮습니다. 야외에서 다시 시도해주세요."
        if (fix.elapsedRealtimeNanos <= 0 || fix.elapsedRealtimeNanos > nowNanos ||
            nowNanos - fix.elapsedRealtimeNanos > 10_000_000_000L) return "최신 위치를 확인하지 못했습니다. 다시 시도해주세요."
        if (distanceM(fix.latitude, fix.longitude, targetLat, targetLng) + fix.accuracyM > radiusM)
            return "미션 장소에 더 가까이 이동한 뒤 다시 인증해주세요."
        return null
    }
}
