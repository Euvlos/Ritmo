package com.ritmo.app.data.repository

import com.ritmo.app.data.local.CompletionEntity
import com.ritmo.app.data.local.GoalDao
import com.ritmo.app.data.local.toDomain
import com.ritmo.app.data.local.toEntity
import com.ritmo.app.domain.model.DailyGoal
import com.ritmo.app.domain.model.Goal
import com.ritmo.app.domain.repository.GoalRepository
import com.ritmo.app.domain.repository.CompletionRecord
import com.ritmo.app.domain.repository.SkipRecord
import com.ritmo.app.domain.repository.SnoozeRecord
import java.time.Instant
import java.time.LocalDate
import com.ritmo.app.domain.model.isScheduledOn
import com.ritmo.app.notifications.ReminderScheduler
import com.ritmo.app.data.local.ScheduleSlotEntity
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import com.ritmo.app.widget.RitmoWidgetProvider

class RoomGoalRepository @Inject constructor(private val dao: GoalDao, private val reminders: ReminderScheduler, @ApplicationContext private val context: Context) : GoalRepository {
    override fun observeDay(date: LocalDate): Flow<List<DailyGoal>> =
        combine(dao.observeForDay(date.toEpochDay()), dao.observeSlots()) { rows, slots ->
            val times = slots.groupBy { it.goalId }.mapValues { it.value.map { slot -> slot.localTimeMinutes } }
            rows.map { row ->
                DailyGoal(
                    com.ritmo.app.data.local.GoalEntity(row.id, row.title, row.type, row.targetValue, row.unit, row.color, row.startEpochDay, row.status, row.recurrenceType, row.repeatInterval, row.timesPerDay, row.reminderTimeMinutes, row.weekdaysMask, row.monthDay).toDomain().copy(executionTimes = times[row.id].orEmpty()),
                    row.completed,
                )
            }.filter { it.goal.isScheduledOn(date) }
        }

    override suspend fun create(goal: Goal): Long {
        val id = dao.insertGoal(goal.toEntity())
        if (goal.executionTimes.isNotEmpty()) dao.insertSlots(goal.executionTimes.mapIndexed { index, time -> ScheduleSlotEntity(goalId = id, localTimeMinutes = time, sortOrder = index) })
        if (goal.reminderTimeMinutes != null || goal.executionTimes.isNotEmpty()) reminders.schedule(goal.copy(id = id))
        RitmoWidgetProvider.updateAll(context)
        return id
    }
    override suspend fun increment(goalId: Long, date: LocalDate) {
        dao.insertCompletion(CompletionEntity(goalId = goalId, effectiveEpochDay = date.toEpochDay(), completedAt = Instant.now()))
        RitmoWidgetProvider.updateAll(context)
    }
    override suspend fun delete(goal: Goal) {
        dao.archiveGoal(goal.id, LocalDate.now().toEpochDay())
        reminders.cancel(goal)
        RitmoWidgetProvider.updateAll(context)
    }
    override suspend fun decrement(goalId: Long, date: LocalDate) { dao.undoLast(goalId, date.toEpochDay()); RitmoWidgetProvider.updateAll(context) }
    override fun observeGoals(): Flow<List<Goal>> = combine(dao.observeGoals(), dao.observeSlots()) { goals, slots ->
        val times = slots.groupBy { it.goalId }.mapValues { it.value.map { slot -> slot.localTimeMinutes } }
        goals.map { entity -> entity.toDomain().copy(executionTimes = times[entity.id].orEmpty()) }
    }
    override fun observeAnalyticsGoals(): Flow<List<Goal>> = combine(dao.observeAnalyticsGoals(), dao.observeSlots()) { goals, slots ->
        val times = slots.groupBy { it.goalId }.mapValues { it.value.map { slot -> slot.localTimeMinutes } }
        goals.map { entity -> entity.toDomain().copy(executionTimes = times[entity.id].orEmpty()) }
    }
    override fun observeCompletions(from: LocalDate, to: LocalDate): Flow<List<CompletionRecord>> =
        dao.observeCompletions(from.toEpochDay(), to.toEpochDay()).map { rows -> rows.map { CompletionRecord(it.goalId, LocalDate.ofEpochDay(it.effectiveEpochDay), it.value, it.completedAt) } }
    override fun observeSkips(from: LocalDate, to: LocalDate): Flow<List<SkipRecord>> =
        dao.observeSkips(from.toEpochDay(), to.toEpochDay()).map { rows -> rows.map { SkipRecord(it.goalId, LocalDate.ofEpochDay(it.effectiveEpochDay), it.slotTimeMinutes, it.skippedAt) } }
    override fun observeSnoozes(from: LocalDate, to: LocalDate): Flow<List<SnoozeRecord>> =
        dao.observeSnoozes(from.toEpochDay(), to.toEpochDay()).map { rows -> rows.map { SnoozeRecord(it.goalId, LocalDate.ofEpochDay(it.effectiveEpochDay), it.slotTimeMinutes, it.requestedAt, it.snoozedUntil) } }
}
