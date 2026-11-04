package com.ritmo.app.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ritmo.app.domain.model.Goal
import com.ritmo.app.domain.model.isScheduledOn
import com.ritmo.app.domain.model.requiredProgress
import com.ritmo.app.domain.repository.GoalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.flow.*

data class CalendarDay(val date: LocalDate, val inMonth: Boolean, val scheduled: Int, val completed: Int) {
    val progress: Float get() = if (scheduled == 0) 0f else (completed.toFloat() / scheduled).coerceIn(0f, 1f)
}
data class CalendarState(val month: YearMonth = YearMonth.now(), val selected: LocalDate = LocalDate.now(), val days: List<CalendarDay> = emptyList(), val selectedGoals: List<Goal> = emptyList())

@HiltViewModel
class CalendarViewModel @Inject constructor(private val repository: GoalRepository) : ViewModel() {
    private val month = MutableStateFlow(YearMonth.now())
    private val selected = MutableStateFlow(LocalDate.now())
    val state = month.flatMapLatest { ym ->
        val first = ym.atDay(1)
        val gridStart = first.minusDays(((first.dayOfWeek.value - DayOfWeek.MONDAY.value + 7) % 7).toLong())
        val gridEnd = gridStart.plusDays(41)
        combine(repository.observeGoals(), repository.observeCompletions(gridStart, gridEnd), selected) { goals, completions, chosen ->
            val completed = completions.groupBy { it.date }.mapValues { (_, rows) -> rows.sumOf { it.value } }
            val days = (0L..41L).map { offset ->
                val date = gridStart.plusDays(offset)
                CalendarDay(date, YearMonth.from(date) == ym, goals.filter { it.isScheduledOn(date) }.sumOf { it.requiredProgress }, completed[date] ?: 0)
            }
            CalendarState(ym, chosen, days, goals.filter { it.isScheduledOn(chosen) })
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CalendarState())
    fun previous() { month.update { it.minusMonths(1) } }
    fun next() { month.update { it.plusMonths(1) } }
    fun select(date: LocalDate) { selected.value = date; if (YearMonth.from(date) != month.value) month.value = YearMonth.from(date) }
}
