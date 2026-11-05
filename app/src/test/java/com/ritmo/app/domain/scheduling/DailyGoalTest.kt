package com.ritmo.app.domain.scheduling

import com.ritmo.app.domain.model.DailyGoal
import com.ritmo.app.domain.model.Goal
import com.ritmo.app.domain.model.GoalType
import com.ritmo.app.domain.model.availableProgressAt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyGoalTest {
    @Test fun `counter progress is capped visually`() {
        val goal = DailyGoal(Goal(title = "Вода", type = GoalType.COUNTER, targetValue = 8), completed = 10)
        assertEquals(1f, goal.progress)
        assertTrue(goal.isComplete)
    }

    @Test fun `partial counter is not complete`() {
        val goal = DailyGoal(Goal(title = "Вода", type = GoalType.COUNTER, targetValue = 8), completed = 5)
        assertEquals(0.625f, goal.progress)
        assertFalse(goal.isComplete)
    }

    @Test fun `execution becomes available within configured early window`() {
        val goal = Goal(title = "Сон", timesPerDay = 2, executionTimes = listOf(8 * 60, 22 * 60), earlyCompletionMinutes = 30)
        assertEquals(0, goal.availableProgressAt(7 * 60 + 29))
        assertEquals(1, goal.availableProgressAt(7 * 60 + 30))
        assertEquals(2, goal.availableProgressAt(21 * 60 + 30))
    }
}
