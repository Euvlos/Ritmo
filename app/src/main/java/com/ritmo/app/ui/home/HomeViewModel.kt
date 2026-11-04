package com.ritmo.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ritmo.app.domain.model.DailyGoal
import com.ritmo.app.domain.repository.GoalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.ritmo.app.domain.model.availableProgressAt

data class HomeUiState(val goals: List<DailyGoal> = emptyList(), val currentMinute: Int = 0) {
    val done: Int get() = goals.count { it.isComplete }
    val progress: Float get() = if (goals.isEmpty()) 0f else done.toFloat() / goals.size
}

@HiltViewModel
class HomeViewModel @Inject constructor(private val repository: GoalRepository) : ViewModel() {
    private val today = LocalDate.now()
    private val clock = flow {
        while (true) { val now = LocalTime.now(); emit(now.hour * 60 + now.minute); delay(30_000) }
    }
    val state: StateFlow<HomeUiState> = combine(repository.observeDay(today), clock) { goals, minute -> HomeUiState(goals, minute) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun increment(goal: DailyGoal) = viewModelScope.launch {
        val now = LocalTime.now(); val allowed = goal.goal.availableProgressAt(now.hour * 60 + now.minute)
        if (goal.completed < allowed && (!goal.isComplete || goal.goal.type.name == "COUNTER")) repository.increment(goal.goal.id, today)
    }
    fun decrement(goal: DailyGoal) = viewModelScope.launch {
        if (goal.completed > 0) repository.decrement(goal.goal.id, today)
    }
}
