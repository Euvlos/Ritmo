package com.ritmo.app.domain.scheduling

import com.ritmo.app.domain.model.Goal
import com.ritmo.app.domain.model.RecurrenceType
import com.ritmo.app.domain.model.isScheduledOn
import java.time.LocalDate
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecurrenceTest {
    private val start = LocalDate.of(2026, 11, 4)

    @Test fun `daily goal is scheduled every day from start`() {
        val goal = Goal(title = "Чтение", startDate = start)
        assertFalse(goal.isScheduledOn(start.minusDays(1)))
        assertTrue(goal.isScheduledOn(start))
        assertTrue(goal.isScheduledOn(start.plusDays(9)))
    }

    @Test fun `interval goal uses fixed start date as anchor`() {
        val goal = Goal(title = "Тренировка", startDate = start, recurrenceType = RecurrenceType.INTERVAL_DAYS, repeatInterval = 2)
        assertTrue(goal.isScheduledOn(start))
        assertFalse(goal.isScheduledOn(start.plusDays(1)))
        assertTrue(goal.isScheduledOn(start.plusDays(2)))
        assertFalse(goal.isScheduledOn(start.plusDays(3)))
    }

    @Test fun `weekly interval respects selected weekdays`() {
        val mondayAndFriday = (1 shl 0) or (1 shl 4)
        val monday = LocalDate.of(2026, 11, 2)
        val goal = Goal(title = "Спорт", startDate = monday, recurrenceType = RecurrenceType.INTERVAL_WEEKS, repeatInterval = 2, weekdaysMask = mondayAndFriday)
        assertTrue(goal.isScheduledOn(monday))
        assertTrue(goal.isScheduledOn(monday.plusDays(4)))
        assertFalse(goal.isScheduledOn(monday.plusWeeks(1)))
        assertTrue(goal.isScheduledOn(monday.plusWeeks(2)))
    }

    @Test fun `monthly day is clamped to last day`() {
        val goal = Goal(title = "Итоги", startDate = LocalDate.of(2026, 1, 31), recurrenceType = RecurrenceType.MONTHLY, monthDay = 31)
        assertTrue(goal.isScheduledOn(LocalDate.of(2026, 2, 28)))
        assertTrue(goal.isScheduledOn(LocalDate.of(2026, 3, 31)))
        assertFalse(goal.isScheduledOn(LocalDate.of(2026, 3, 30)))
    }
}
