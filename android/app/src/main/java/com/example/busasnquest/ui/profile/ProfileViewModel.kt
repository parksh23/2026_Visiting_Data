package com.example.busasnquest.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.busasnquest.data.model.MissionState
import com.example.busasnquest.data.remote.RetrofitInstance
import com.example.busasnquest.data.remote.UserProfileDto
import com.example.busasnquest.data.repository.MissionRepository
import com.example.busasnquest.data.repository.MissionWithState
import com.example.busasnquest.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProfileUiState(
    val name: String = "부산갈매기",
    val intro: String = "부산을 사랑하는 여행자",
    val points: Int = 0,
    val completedCount: Int = 0,
    val savedCount: Int = 0,
    val completedMissions: List<MissionWithState> = emptyList(),
    val savedMissions: List<MissionWithState> = emptyList()
)

class ProfileViewModel : ViewModel() {

    private val remoteProfile = MutableStateFlow<UserProfileDto?>(null)

    val uiState: StateFlow<ProfileUiState> =
        combine(
            remoteProfile,
            UserRepository.points,
            MissionRepository.missions
        ) { profile, localPoints, missions ->

            val completed = missions.filter { it.state == MissionState.COMPLETED }
            val saved = missions.filter { it.saved }

            ProfileUiState(
                name = profile?.name ?: "부산갈매기",
                intro = "부산을 사랑하는 여행자",
                points = profile?.points?.toPointInt() ?: localPoints,
                completedCount = profile?.completedMissions ?: completed.size,
                savedCount = profile?.savedMissions ?: saved.size,
                completedMissions = completed,
                savedMissions = saved
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ProfileUiState()
        )

    init {
        loadMyProfile()
    }

    private fun loadMyProfile() {
        viewModelScope.launch {
            try {
                remoteProfile.value = RetrofitInstance.api.getMyProfile()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

private fun String.toPointInt(): Int {
    return this.filter { it.isDigit() }.toIntOrNull() ?: 0
}