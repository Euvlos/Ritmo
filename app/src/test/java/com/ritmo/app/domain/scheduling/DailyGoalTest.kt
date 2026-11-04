package com.ritmo.app.domain.scheduling

import com.ritmo.app.domain.model.DailyGoal
import com.ritmo.app.domain.model.Goal
import com.ritmo.app.domain.model.GoalType
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
}
