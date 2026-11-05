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
import com.ritmo.app.domain.model.GoalReminder
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
            val grouped = slots.groupBy { it.goalId }
            rows.map { row ->
                val goalSlots = grouped[row.id].orEmpty()
                DailyGoal(
                    com.ritmo.app.data.local.GoalEntity(row.id, row.title, row.type, row.targetValue, row.unit, row.color, row.startEpochDay, row.status, row.recurrenceType, row.repeatInterval, row.timesPerDay, row.reminderTimeMinutes, row.weekdaysMask, row.monthDay, row.archivedEpochDay, row.reminderMode, row.alarmSoundUri, row.alarmSoundName, row.earlyCompletionMinutes).toDomain().copy(executionTimes = goalSlots.map { it.localTimeMinutes }, reminders = goalSlots.map { GoalReminder(it.localTimeMinutes, it.reminderMode, it.alarmSoundUri, it.alarmSoundName) }),
                    row.completed,
                )
            }.filter { it.goal.isScheduledOn(date) }
        }

    override suspend fun create(goal: Goal): Long {
        val id = dao.insertGoal(goal.toEntity())
        if (goal.executionTimes.isNotEmpty()) dao.insertSlots(goal.executionTimes.mapIndexed { index, time -> goal.reminders.getOrNull(index)?.let { ScheduleSlotEntity(goalId = id, localTimeMinutes = time, sortOrder = index, reminderMode = it.mode, alarmSoundUri = it.soundUri, alarmSoundName = it.soundName) } ?: ScheduleSlotEntity(goalId = id, localTimeMinutes = time, sortOrder = index) })
        if (goal.reminderTimeMinutes != null || goal.executionTimes.isNotEmpty()) reminders.schedule(goal.copy(id = id))
        RitmoWidgetProvider.updateAll(context)
        return id
    }
    override suspend fun get(goalId: Long): Goal? = dao.getGoal(goalId)?.toDomain()?.let { goal -> val slots = dao.getSlots(goalId); goal.copy(executionTimes = slots.map { it.localTimeMinutes }, reminders = slots.map { GoalReminder(it.localTimeMinutes, it.reminderMode, it.alarmSoundUri, it.alarmSoundName) }) }
    override suspend fun update(goal: Goal) {
        val previous = get(goal.id)
        if (previous != null) reminders.cancel(previous)
        dao.updateGoal(goal.toEntity())
        dao.deleteSlots(goal.id)
        if (goal.executionTimes.isNotEmpty()) dao.insertSlots(goal.executionTimes.mapIndexed { index, time -> goal.reminders.getOrNull(index)?.let { ScheduleSlotEntity(goalId = goal.id, localTimeMinutes = time, sortOrder = index, reminderMode = it.mode, alarmSoundUri = it.soundUri, alarmSoundName = it.soundName) } ?: ScheduleSlotEntity(goalId = goal.id, localTimeMinutes = time, sortOrder = index) })
        if (goal.reminderTimeMinutes != null || goal.executionTimes.isNotEmpty()) reminders.schedule(goal)
        RitmoWidgetProvider.updateAll(context)
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
        val grouped = slots.groupBy { it.goalId }
        goals.map { entity -> val goalSlots = grouped[entity.id].orEmpty(); entity.toDomain().copy(executionTimes = goalSlots.map { it.localTimeMinutes }, reminders = goalSlots.map { GoalReminder(it.localTimeMinutes, it.reminderMode, it.alarmSoundUri, it.alarmSoundName) }) }
    }
    override fun observeAnalyticsGoals(): Flow<List<Goal>> = combine(dao.observeAnalyticsGoals(), dao.observeSlots()) { goals, slots ->
        val grouped = slots.groupBy { it.goalId }
        goals.map { entity -> val goalSlots = grouped[entity.id].orEmpty(); entity.toDomain().copy(executionTimes = goalSlots.map { it.localTimeMinutes }, reminders = goalSlots.map { GoalReminder(it.localTimeMinutes, it.reminderMode, it.alarmSoundUri, it.alarmSoundName) }) }
    }
    override fun observeCompletions(from: LocalDate, to: LocalDate): Flow<List<CompletionRecord>> =
        dao.observeCompletions(from.toEpochDay(), to.toEpochDay()).map { rows -> rows.map { CompletionRecord(it.goalId, LocalDate.ofEpochDay(it.effectiveEpochDay), it.value, it.completedAt) } }
    override fun observeSkips(from: LocalDate, to: LocalDate): Flow<List<SkipRecord>> =
        dao.observeSkips(from.toEpochDay(), to.toEpochDay()).map { rows -> rows.map { SkipRecord(it.goalId, LocalDate.ofEpochDay(it.effectiveEpochDay), it.slotTimeMinutes, it.skippedAt) } }
    override fun observeSnoozes(from: LocalDate, to: LocalDate): Flow<List<SnoozeRecord>> =
        dao.observeSnoozes(from.toEpochDay(), to.toEpochDay()).map { rows -> rows.map { SnoozeRecord(it.goalId, LocalDate.ofEpochDay(it.effectiveEpochDay), it.slotTimeMinutes, it.requestedAt, it.snoozedUntil) } }
}
