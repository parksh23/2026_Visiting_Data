package com.example.busasnquest.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// object = 앱 전체에서 딱 하나만 존재하는 인스턴스 (모든 탭이 같은 걸 봄)
object UserRepository {

    private val _points = MutableStateFlow(2450)
    val points: StateFlow<Int> = _points.asStateFlow()

    // 포인트 적립
    fun addPoints(amount: Int) {
        _points.update { it + amount }
    }
}