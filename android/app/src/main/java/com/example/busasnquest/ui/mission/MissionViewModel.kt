package com.example.busasnquest.ui.mission

import androidx.lifecycle.ViewModel
import com.example.busasnquest.data.model.DistrictProgress
import com.example.busasnquest.data.model.districtProgressList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class MissionUiState(
    val districts: List<DistrictProgress> = emptyList(),
    val selectedTab: Int = 0
)

class MissionViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MissionUiState())
    val uiState: StateFlow<MissionUiState> = _uiState.asStateFlow()

    init {
        _uiState.update { it.copy(districts = districtProgressList) }
    }

    fun selectTab(index: Int) {
        _uiState.update { it.copy(selectedTab = index) }
    }
}