package com.example.busasnquest.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.busasnquest.data.model.MissionState
import com.example.busasnquest.data.repository.MissionRepository
import com.example.busasnquest.data.repository.MissionWithState
import com.example.busasnquest.data.repository.UserRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted

data class ProfileUiState(
    val name: String = "부산갈매기",
    val intro: String = "부산을 사랑하는 여행자",
    val points: Int = 0,
    val completedCount: Int = 0,
    val savedCount: Int = 0,
    val completedMissions: List<MissionWithState> = emptyList()
)

class ProfileViewModel : ViewModel() {

    val uiState: StateFlow<ProfileUiState> =
        combine(UserRepository.points, MissionRepository.missions) { points, missions ->
            val completed = missions.filter { it.state == MissionState.COMPLETED }
            ProfileUiState(
                points = points,
                completedCount = completed.size,
                completedMissions = completed
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ProfileUiState()
        )
}