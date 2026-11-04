package com.ritmo.app.ui.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ritmo.app.domain.analytics.*
import com.ritmo.app.domain.model.Goal
import com.ritmo.app.domain.repository.GoalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.*

data class GoalStat(val goal: Goal, val metrics: QualityMetrics, val trend: Int, val insight: String)
data class StatisticsState(val metrics: QualityMetrics = QualityMetrics(), val trend: Int = 0, val insight: String = "Недостаточно данных", val goals: List<GoalStat> = emptyList(), val range: AnalyticsRange = AnalyticsRange.of(AnalyticsPeriod.MONTH))

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class StatisticsViewModel @Inject constructor(private val repository: GoalRepository) : ViewModel() {
    private val range = MutableStateFlow(AnalyticsRange.of(AnalyticsPeriod.MONTH))
    fun period(value: AnalyticsPeriod) { range.value = AnalyticsRange.of(value) }
    fun custom(from: LocalDate, to: LocalDate) { range.value = AnalyticsRange(AnalyticsPeriod.CUSTOM, minOf(from, to), maxOf(from, to)) }

    val state = range.flatMapLatest { selected ->
        combine(repository.observeAnalyticsGoals(), repository.observeCompletions(selected.previousFrom, selected.to), repository.observeSkips(selected.previousFrom, selected.to), repository.observeSnoozes(selected.previousFrom, selected.to)) { goals, completions, skips, snoozes ->
            val current = goals.map { goal ->
                fun values(start: LocalDate, end: LocalDate) = completions.filter { it.goalId == goal.id && !it.date.isBefore(start) && !it.date.isAfter(end) }.groupBy { it.date }.mapValues { it.value.sumOf { row -> row.value } }
                val metrics = QualityAnalyzer.analyze(goal, selected.from, selected.to, values(selected.from, selected.to), skips.count { it.goalId == goal.id && !it.date.isBefore(selected.from) }, snoozes.count { it.goalId == goal.id && !it.date.isBefore(selected.from) })
                val previous = QualityAnalyzer.analyze(goal, selected.previousFrom, selected.previousTo, values(selected.previousFrom, selected.previousTo), skips.count { it.goalId == goal.id && it.date in selected.previousFrom..selected.previousTo }, snoozes.count { it.goalId == goal.id && it.date in selected.previousFrom..selected.previousTo })
                val trend = if (previous.scheduledPeriods == 0) 0 else metrics.qualityScore - previous.qualityScore
                GoalStat(goal, metrics, trend, QualityAnalyzer.insight(metrics, trend))
            }
            val periods = current.sumOf { it.metrics.scheduledPeriods }
            fun weighted(value: (GoalStat) -> Int) = if (periods == 0) 0 else current.sumOf { value(it) * it.metrics.scheduledPeriods } / periods
            val aggregate = QualityMetrics(
                completionRate = weighted { it.metrics.completionRate }, qualityScore = weighted { it.metrics.qualityScore }, scheduledPeriods = periods,
                completedPeriods = current.sumOf { it.metrics.completedPeriods }, partialPeriods = current.sumOf { it.metrics.partialPeriods }, missedPeriods = current.sumOf { it.metrics.missedPeriods },
                skippedOccurrences = current.sumOf { it.metrics.skippedOccurrences }, snoozedOccurrences = current.sumOf { it.metrics.snoozedOccurrences }, timeliness = weighted { it.metrics.timeliness },
                currentStreak = current.maxOfOrNull { it.metrics.currentStreak } ?: 0, bestStreak = current.maxOfOrNull { it.metrics.bestStreak } ?: 0, consistency = weighted { it.metrics.consistency },
                dailyProgress = (0 until selected.days.toInt()).map { index -> current.mapNotNull { it.metrics.dailyProgress.getOrNull(index) }.let { if (it.isEmpty()) 0f else it.average().toFloat() } },
            )
            val trend = if (current.isEmpty()) 0 else current.map { it.trend }.average().toInt()
            StatisticsState(aggregate, trend, QualityAnalyzer.insight(aggregate, trend), current.sortedBy { it.metrics.qualityScore }, selected)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatisticsState())
}
