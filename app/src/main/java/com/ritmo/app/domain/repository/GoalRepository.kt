package com.ritmo.app.domain.repository

import com.ritmo.app.domain.model.DailyGoal
import com.ritmo.app.domain.model.Goal
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import java.time.Instant

data class CompletionRecord(val goalId: Long, val date: LocalDate, val value: Int, val completedAt: Instant)
data class SkipRecord(val goalId: Long, val date: LocalDate, val slotTimeMinutes: Int?, val skippedAt: Instant)
data class SnoozeRecord(val goalId: Long, val date: LocalDate, val slotTimeMinutes: Int?, val requestedAt: Instant, val snoozedUntil: Instant)

interface GoalRepository {
    fun observeDay(date: LocalDate): Flow<List<DailyGoal>>
    suspend fun create(goal: Goal): Long
    suspend fun get(goalId: Long): Goal?
    suspend fun update(goal: Goal)
    suspend fun delete(goal: Goal)
    suspend fun increment(goalId: Long, date: LocalDate)
    suspend fun decrement(goalId: Long, date: LocalDate)
    fun observeGoals(): Flow<List<Goal>>
    fun observeAnalyticsGoals(): Flow<List<Goal>>
    fun observeCompletions(from: LocalDate, to: LocalDate): Flow<List<CompletionRecord>>
    fun observeSkips(from: LocalDate, to: LocalDate): Flow<List<SkipRecord>>
    fun observeSnoozes(from: LocalDate, to: LocalDate): Flow<List<SnoozeRecord>>
}
