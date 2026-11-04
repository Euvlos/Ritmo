package com.ritmo.app.ui.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ritmo.app.domain.model.Goal
import com.ritmo.app.domain.model.GoalType
import com.ritmo.app.domain.model.RecurrenceType
import com.ritmo.app.domain.repository.GoalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GoalEditorState(
    val title: String = "",
    val type: GoalType = GoalType.SIMPLE,
    val targetValue: Int = 1,
    val unit: String = "раз",
    val saving: Boolean = false,
    val titleError: Boolean = false,
    val recurrenceType: RecurrenceType = RecurrenceType.INTERVAL_DAYS,
    val repeatInterval: Int = 1,
    val timesPerDay: Int = 1,
    val reminderTimeMinutes: Int? = null,
    val weekdaysMask: Int = 0b0011111,
    val monthDay: Int = 1,
    val bindToTimes: Boolean = false,
    val executionTimes: List<Int> = emptyList(),
)

@HiltViewModel
class GoalEditorViewModel @Inject constructor(private val repository: GoalRepository) : ViewModel() {
    private val _state = MutableStateFlow(GoalEditorState())
    val state = _state.asStateFlow()
    fun title(value: String) = _state.update { it.copy(title = value, titleError = false) }
    fun type(value: GoalType) = _state.update { it.copy(type = value, targetValue = if (value == GoalType.SIMPLE) 1 else maxOf(2, it.targetValue)) }
    fun target(value: Int) = _state.update { it.copy(targetValue = value.coerceIn(1, 9999)) }
    fun unit(value: String) = _state.update { it.copy(unit = value) }
    fun recurrence(value: RecurrenceType) = _state.update { it.copy(recurrenceType = value) }
    fun interval(value: Int) = _state.update { it.copy(repeatInterval = value.coerceIn(1, 365)) }
    fun times(value: Int) = _state.update {
        val count = value.coerceIn(1, 12)
        it.copy(timesPerDay = count, executionTimes = if (it.bindToTimes) resizeTimes(it.executionTimes, count) else emptyList())
    }
    fun reminder(value: Int?) = _state.update { it.copy(reminderTimeMinutes = value) }
    fun toggleWeekday(index: Int) = _state.update {
        val toggled = it.weekdaysMask xor (1 shl index)
        it.copy(weekdaysMask = if (toggled == 0) it.weekdaysMask else toggled)
    }
    fun monthDay(value: Int) = _state.update { it.copy(monthDay = value.coerceIn(1, 31)) }
    fun bindToTimes(enabled: Boolean) = _state.update {
        it.copy(bindToTimes = enabled, executionTimes = if (enabled) resizeTimes(it.executionTimes, it.timesPerDay) else emptyList(), reminderTimeMinutes = if (enabled) null else it.reminderTimeMinutes)
    }
    fun executionTime(index: Int, minutes: Int) = _state.update { state -> state.copy(executionTimes = state.executionTimes.toMutableList().also { it[index] = minutes }.sorted()) }
    fun specificDays(enabled: Boolean) = _state.update { it.copy(recurrenceType = if (enabled) RecurrenceType.INTERVAL_WEEKS else RecurrenceType.INTERVAL_DAYS, repeatInterval = 1) }
    fun specificMode(value: RecurrenceType) = _state.update { it.copy(recurrenceType = value) }
    fun save(onSaved: () -> Unit) {
        val value = state.value
        if (value.title.isBlank()) { _state.update { it.copy(titleError = true) }; return }
        viewModelScope.launch {
            _state.update { it.copy(saving = true) }
            repository.create(Goal(title = value.title, type = value.type, targetValue = value.targetValue, unit = value.unit.ifBlank { "раз" }, recurrenceType = value.recurrenceType, repeatInterval = value.repeatInterval, timesPerDay = value.timesPerDay, reminderTimeMinutes = value.reminderTimeMinutes, weekdaysMask = value.weekdaysMask, monthDay = value.monthDay, executionTimes = value.executionTimes))
            onSaved()
        }
    }

    private fun resizeTimes(existing: List<Int>, count: Int): List<Int> {
        if (existing.size >= count) return existing.take(count)
        val defaults = if (count == 1) listOf(9 * 60) else (0 until count).map { 8 * 60 + it * (12 * 60 / (count - 1)) }
        return (existing + defaults.drop(existing.size)).take(count).sorted()
    }
}
