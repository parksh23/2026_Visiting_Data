package kr.co.busanquest.util

import com.google.gson.Gson
import java.util.Base64
import kr.co.busanquest.data.remote.MissionVerifyRequestDto
import org.junit.Assert.*
import org.junit.Test

class LocalLocationPolicyTest {
    private val now = 30_000_000_000L
    private val good = LocationFix(35.1, 129.03, 10.0, now - 1_000_000_000L, false)
    private fun failure(fix: LocationFix) = LocalLocationPolicy.failure(fix, 35.1, 129.03, 300.0, 100.0, now)

    @Test fun freshRealLocationInsideBoundaryPasses() { assertNull(failure(good)) }
    @Test fun mockLocationAlwaysFailsEvenAtTarget() { assertNotNull(failure(good.copy(isMock = true))) }
    @Test fun oldAndFutureFixesFail() {
        assertNotNull(failure(good.copy(elapsedRealtimeNanos = now - 10_000_000_001)))
        assertNotNull(failure(good.copy(elapsedRealtimeNanos = now + 1)))
    }
    @Test fun invalidCoordinatesAndAccuracyFail() {
        for (value in listOf(Double.NaN, Double.POSITIVE_INFINITY, -1.0, 0.0, 101.0)) {
            assertNotNull(failure(good.copy(accuracyM = value)))
        }
        assertNotNull(failure(good.copy(latitude = Double.NaN)))
        assertNotNull(failure(good.copy(longitude = 181.0)))
    }
    @Test fun uncertaintyMustFitWithinBoundary() {
        val nearBoundary = good.copy(latitude = 35.1026, accuracyM = 30.0)
        assertTrue(LocalLocationPolicy.distanceM(nearBoundary.latitude, nearBoundary.longitude, 35.1, 129.03) < 300)
        assertNotNull(failure(nearBoundary))
        assertNotNull(failure(good.copy(latitude = 37.5)))
    }
    @Test fun invalidMissionPolicyFails() {
        assertNotNull(LocalLocationPolicy.failure(good, Double.NaN, 129.03, 300.0, 100.0, now))
        assertNotNull(LocalLocationPolicy.failure(good, 35.1, 129.03, 0.0, 100.0, now))
    }
    @Test fun serializationContainsNoCoordinatesAndMatchesServerHash() {
        val req = MissionVerifyRequestDto(1, "CURRENT_LOCATION", challengeId = "a".repeat(64), localPassed = true)
        val json = Gson().toJson(req)
        for (key in listOf("latitude", "longitude", "accuracy", "distance", "isMock")) assertFalse(json.contains(key))
        assertEquals("V-qTQ3l1j5_H9WQceYbK3OvHYmoXLemB_cyhomqIW5E",
            Base64.getUrlEncoder().withoutPadding().encodeToString(VerificationRequestHash.bytes(req)))
        assertFalse(VerificationRequestHash.bytes(req).contentEquals(VerificationRequestHash.bytes(req.copy(missionId = 2))))
    }
    @Test fun photoUrlIsBoundToProofAndUsesCanonicalField() {
        val req = MissionVerifyRequestDto(2, "PHOTO", imageUrl = "https://example.com/photo.jpg",
            challengeId = "a".repeat(64), localPassed = true)
        val json = Gson().toJson(req)
        assertTrue(json.contains("\"photo_url\""))
        assertFalse(json.contains("\"image\""))
        assertFalse(VerificationRequestHash.bytes(req).contentEquals(VerificationRequestHash.bytes(req.copy(imageUrl = "https://example.com/other.jpg"))))
    }
}
