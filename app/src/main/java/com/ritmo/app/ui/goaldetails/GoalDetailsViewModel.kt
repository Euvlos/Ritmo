package com.ritmo.app.ui.goaldetails

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ritmo.app.domain.analytics.*
import com.ritmo.app.domain.model.Goal
import com.ritmo.app.domain.repository.GoalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class GoalDetailsState(val goal: Goal? = null, val metrics: QualityMetrics = QualityMetrics(), val trend: Int = 0, val insight: String = "", val range: AnalyticsRange = AnalyticsRange.of(AnalyticsPeriod.MONTH))

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class GoalDetailsViewModel @Inject constructor(savedState: SavedStateHandle, private val repository: GoalRepository) : ViewModel() {
    private val id: Long = checkNotNull(savedState["goalId"])
    private val range = MutableStateFlow(AnalyticsRange.of(AnalyticsPeriod.MONTH))
    fun period(value: AnalyticsPeriod) { range.value = AnalyticsRange.of(value) }
    fun custom(from: LocalDate, to: LocalDate) { range.value = AnalyticsRange(AnalyticsPeriod.CUSTOM, minOf(from, to), maxOf(from, to)) }
    fun delete(goal: Goal, onDeleted: () -> Unit) = viewModelScope.launch { repository.delete(goal); onDeleted() }

    val state = range.flatMapLatest { selected -> combine(repository.observeAnalyticsGoals(), repository.observeCompletions(selected.previousFrom, selected.to), repository.observeSkips(selected.previousFrom, selected.to), repository.observeSnoozes(selected.previousFrom, selected.to)) { goals, records, skips, snoozes ->
        val goal = goals.firstOrNull { it.id == id } ?: return@combine GoalDetailsState(range = selected)
        fun values(start: LocalDate, end: LocalDate) = records.filter { it.goalId == id && it.date in start..end }.groupBy { it.date }.mapValues { it.value.sumOf { row -> row.value } }
        val metrics = QualityAnalyzer.analyze(goal, selected.from, selected.to, values(selected.from, selected.to), skips.count { it.goalId == id && it.date in selected.from..selected.to }, snoozes.count { it.goalId == id && it.date in selected.from..selected.to })
        val previous = QualityAnalyzer.analyze(goal, selected.previousFrom, selected.previousTo, values(selected.previousFrom, selected.previousTo), skips.count { it.goalId == id && it.date in selected.previousFrom..selected.previousTo }, snoozes.count { it.goalId == id && it.date in selected.previousFrom..selected.previousTo })
        val trend = if (previous.scheduledPeriods == 0) 0 else metrics.qualityScore - previous.qualityScore
        GoalDetailsState(goal, metrics, trend, QualityAnalyzer.insight(metrics, trend), selected)
    }}.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GoalDetailsState())
}
