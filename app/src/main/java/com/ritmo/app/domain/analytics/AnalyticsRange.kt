package com.ritmo.app.domain.analytics

import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class AnalyticsPeriod { DAY, WEEK, MONTH, YEAR, CUSTOM }

data class AnalyticsRange(val period: AnalyticsPeriod, val from: LocalDate, val to: LocalDate) {
    val days: Long get() = ChronoUnit.DAYS.between(from, to) + 1
    val previousTo: LocalDate get() = from.minusDays(1)
    val previousFrom: LocalDate get() = from.minusDays(days)

    companion object {
        fun of(period: AnalyticsPeriod, today: LocalDate = LocalDate.now()) = when (period) {
            AnalyticsPeriod.DAY -> AnalyticsRange(period, today, today)
            AnalyticsPeriod.WEEK -> AnalyticsRange(period, today.minusDays(6), today)
            AnalyticsPeriod.MONTH -> AnalyticsRange(period, today.minusDays(29), today)
            AnalyticsPeriod.YEAR -> AnalyticsRange(period, today.minusDays(364), today)
            AnalyticsPeriod.CUSTOM -> AnalyticsRange(period, today.minusDays(29), today)
        }
    }
}
