package com.ritmo.app.domain.analytics

import com.ritmo.app.domain.model.Goal
import com.ritmo.app.domain.model.isScheduledOn
import com.ritmo.app.domain.model.requiredProgress
import java.time.LocalDate

data class QualityMetrics(
    val completionRate: Int = 0,
    val qualityScore: Int = 0,
    val scheduledPeriods: Int = 0,
    val completedPeriods: Int = 0,
    val partialPeriods: Int = 0,
    val missedPeriods: Int = 0,
    val skippedOccurrences: Int = 0,
    val snoozedOccurrences: Int = 0,
    val timeliness: Int = 100,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val consistency: Int = 0,
    val dailyProgress: List<Float> = emptyList(),
) {
    val grade: String get() = when (qualityScore) { in 90..100 -> "Отлично"; in 75..89 -> "Хорошо"; in 55..74 -> "Нестабильно"; in 1..54 -> "Требует внимания"; else -> "Нет данных" }
}

object QualityAnalyzer {
    fun analyze(goal: Goal, from: LocalDate, to: LocalDate, completedByDate: Map<LocalDate, Int>, skipped: Int = 0, snoozed: Int = 0): QualityMetrics {
        val dates = generateSequence(from) { if (it < to) it.plusDays(1) else null }.toList()
        val scheduled = dates.filter(goal::isScheduledOn)
        if (scheduled.isEmpty()) return QualityMetrics(skippedOccurrences = skipped, snoozedOccurrences = snoozed, dailyProgress = dates.map { 0f })
        val ratios = scheduled.map { ((completedByDate[it] ?: 0).toFloat() / goal.requiredProgress).coerceIn(0f, 1f) }
        val full = ratios.count { it >= 1f }; val partial = ratios.count { it in 0.0001f..<1f }; val missed = ratios.count { it == 0f }
        val rate = (ratios.average() * 100).toInt()
        val average = ratios.average(); val variance = ratios.map { (it - average) * (it - average) }.average()
        val consistency = ((1.0 - kotlin.math.sqrt(variance)).coerceIn(0.0, 1.0) * 100).toInt()
        var best = 0; var run = 0
        ratios.forEach { if (it >= 1f) { run++; best = maxOf(best, run) } else run = 0 }
        var current = 0; for (ratio in ratios.asReversed()) { if (ratio >= 1f) current++ else break }
        val skipPenalty = if (scheduled.isEmpty()) 0 else (skipped * 100 / scheduled.size).coerceAtMost(100)
        val timeliness = (100 - snoozed * 100 / maxOf(1, completedByDate.values.sum())).coerceIn(0, 100)
        val score = (rate * .65 + consistency * .15 + (100 - skipPenalty) * .1 + timeliness * .1).toInt().coerceIn(0, 100)
        val byDate = scheduled.zip(ratios).toMap()
        return QualityMetrics(rate, score, scheduled.size, full, partial, missed, skipped, snoozed, timeliness, current, best, consistency, dates.map { byDate[it] ?: 0f })
    }

    fun insight(metrics: QualityMetrics, trendPoints: Int? = null): String = when {
        metrics.scheduledPeriods == 0 -> "Пока недостаточно запланированных периодов для оценки."
        metrics.completionRate >= 90 && metrics.consistency >= 80 -> "Ритм устойчивый: цель выполняется почти всегда и без заметных провалов."
        metrics.partialPeriods > metrics.completedPeriods -> "Цель часто начинается, но не доводится до полной нормы. Возможно, дневная нагрузка завышена."
        metrics.skippedOccurrences > metrics.completedPeriods -> "Пропусков больше, чем успешных периодов. Стоит пересмотреть время или частоту."
        metrics.snoozedOccurrences > metrics.completedPeriods / 2 -> "Цель часто откладывается. Вероятно, выбранное время плохо совпадает с реальным распорядком."
        trendPoints != null && trendPoints >= 10 -> "За последние 30 дней оценка качества выросла на $trendPoints баллов. Текущий ритм работает."
        trendPoints != null && trendPoints <= -10 -> "Оценка качества снизилась на ${-trendPoints} баллов относительно предыдущего периода."
        metrics.consistency < 45 -> "Выполнение сильно колеблется: хорошие периоды чередуются с провалами."
        metrics.currentStreak >= 5 -> "Сейчас идёт серия из ${metrics.currentStreak} успешных периодов — ритм закрепляется."
        else -> "Цель выполняется нерегулярно. Небольшая корректировка расписания может повысить стабильность."
    }
}
