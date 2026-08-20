package com.example.busasnquest

import com.example.busasnquest.data.model.MissionState
import com.example.busasnquest.data.model.MissionType
import com.example.busasnquest.data.model.toServerType
import com.example.busasnquest.data.repository.MissionRepository
import com.example.busasnquest.data.remote.MissionVerifyRequestDto
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MissionStateTest {

    @Test
    fun missionTypesMatchBackendContract() {
        assertEquals("PHOTO", MissionType.IMAGE_LOCATION.toServerType())
        assertEquals("CURRENT_LOCATION", MissionType.CURRENT_LOCATION.toServerType())
        assertEquals("RECEIPT", MissionType.RECEIPT.toServerType())
    }

    @Test
    fun locationVerificationSerializesCoordinatesAndAccuracy() {
        val json = Gson().toJson(
            MissionVerifyRequestDto(
                missionId = 7,
                missionType = "CURRENT_LOCATION",
                latitude = 35.1796,
                longitude = 129.0756,
                locationAccuracyMeters = 18.5f
            )
        )

        assertTrue(json.contains("\"mission_id\":7"))
        assertTrue(json.contains("\"location_accuracy_m\":18.5"))
        assertTrue(json.contains("\"latitude\":35.1796"))
        assertTrue(json.contains("\"longitude\":129.0756"))
    }

    @Test
    fun sessionResetRemovesPreviousUsersMissionState() {
        MissionRepository.resetForSession()
        val missionId = MissionRepository.missions.value.first().mission.id
        MissionRepository.setCompleted(missionId)
        assertEquals(
            MissionState.COMPLETED,
            MissionRepository.missions.value.first { it.mission.id == missionId }.state,
        )

        MissionRepository.resetForSession()

        assertFalse(
            MissionRepository.missions.value.any {
                it.mission.id == missionId && it.state == MissionState.COMPLETED
            }
        )
    }

    @Test
    fun inProgressMissionCanBeCancelledBeforeVerificationOnly() {
        MissionRepository.resetForSession()
        val missionId = MissionRepository.missions.value.first().mission.id

        MissionRepository.startMission(missionId)
        assertTrue(MissionRepository.cancelMission(missionId))
        assertEquals(
            MissionState.NOT_STARTED,
            MissionRepository.missions.value.first { it.mission.id == missionId }.state,
        )

        MissionRepository.setVerifying(missionId)
        assertFalse(MissionRepository.cancelMission(missionId))
        assertEquals(
            MissionState.VERIFYING,
            MissionRepository.missions.value.first { it.mission.id == missionId }.state,
        )
    }
}
