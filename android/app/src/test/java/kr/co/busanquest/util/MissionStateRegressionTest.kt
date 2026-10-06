package kr.co.busanquest.util

import kr.co.busanquest.data.model.MissionState
import kr.co.busanquest.data.repository.MissionRepository
import org.junit.Assert.assertEquals
import org.junit.Test

class MissionStateRegressionTest {
    @Test fun errorAndVerificationCannotStartUnstartedMission() {
        val item = MissionRepository.missions.value.first { it.state == MissionState.NOT_STARTED }
        MissionRepository.setError(item.mission.id, "permission denied")
        assertEquals(MissionState.NOT_STARTED, MissionRepository.missions.value.first { it.mission.id == item.mission.id }.state)
        MissionRepository.setVerifying(item.mission.id)
        assertEquals(MissionState.NOT_STARTED, MissionRepository.missions.value.first { it.mission.id == item.mission.id }.state)
        MissionRepository.setCompleted(item.mission.id)
        MissionRepository.setError(item.mission.id, "late failure")
        assertEquals(MissionState.COMPLETED, MissionRepository.missions.value.first { it.mission.id == item.mission.id }.state)
    }
}
