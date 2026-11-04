package com.ritmo.app.domain.analytics

import com.ritmo.app.domain.model.Goal
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QualityAnalyzerTest {
    @Test fun `distinguishes complete partial and missed periods`() {
        val start = LocalDate.of(2026, 11, 1)
        val goal = Goal(title = "Чтение", startDate = start, timesPerDay = 2)
        val result = QualityAnalyzer.analyze(goal, start, start.plusDays(2), mapOf(start to 2, start.plusDays(1) to 1))
        assertEquals(1, result.completedPeriods)
        assertEquals(1, result.partialPeriods)
        assertEquals(1, result.missedPeriods)
        assertEquals(50, result.completionRate)
    }

    @Test fun `streak follows scheduled periods`() {
        val start = LocalDate.of(2026, 11, 1)
        val goal = Goal(title = "Прогулка", startDate = start)
        val result = QualityAnalyzer.analyze(goal, start, start.plusDays(3), (0L..3L).associate { start.plusDays(it) to 1 })
        assertEquals(4, result.currentStreak)
        assertEquals(4, result.bestStreak)
        assertTrue(result.qualityScore >= 90)
    }
}
