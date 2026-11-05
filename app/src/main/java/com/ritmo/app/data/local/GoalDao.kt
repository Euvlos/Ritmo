package com.ritmo.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Update
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class GoalWithProgress(
    val id: Long,
    val title: String,
    val type: com.ritmo.app.domain.model.GoalType,
    val targetValue: Int,
    val unit: String,
    val color: Long,
    val startEpochDay: Long,
    val status: com.ritmo.app.domain.model.GoalStatus,
    val recurrenceType: com.ritmo.app.domain.model.RecurrenceType,
    val repeatInterval: Int,
    val timesPerDay: Int,
    val reminderTimeMinutes: Int?,
    val weekdaysMask: Int,
    val monthDay: Int,
    val archivedEpochDay: Long?,
    val reminderMode: com.ritmo.app.domain.model.ReminderMode,
    val alarmSoundUri: String?,
    val alarmSoundName: String?,
    val completed: Int,
)

@Dao
interface GoalDao {
    @Query("""
        SELECT g.*, CAST(COALESCE(SUM(c.value), 0) AS INTEGER) AS completed
        FROM goals g LEFT JOIN completions c
          ON c.goalId = g.id AND c.effectiveEpochDay = :epochDay
        WHERE g.status = 'ACTIVE' AND g.startEpochDay <= :epochDay
        GROUP BY g.id ORDER BY g.id DESC
    """)
    fun observeForDay(epochDay: Long): Flow<List<GoalWithProgress>>

    @Query("""
        SELECT g.*, CAST(COALESCE(SUM(c.value), 0) AS INTEGER) AS completed
        FROM goals g LEFT JOIN completions c ON c.goalId = g.id AND c.effectiveEpochDay = :epochDay
        WHERE g.status = 'ACTIVE' AND g.startEpochDay <= :epochDay
        GROUP BY g.id ORDER BY g.id DESC
    """)
    suspend fun getForDay(epochDay: Long): List<GoalWithProgress>

    @Insert suspend fun insertGoal(goal: GoalEntity): Long
    @Update suspend fun updateGoal(goal: GoalEntity)
    @Insert suspend fun insertCompletion(completion: CompletionEntity): Long
    @Insert suspend fun insertSlots(slots: List<ScheduleSlotEntity>)
    @Insert suspend fun insertSkip(skip: SkippedOccurrenceEntity): Long
    @Insert suspend fun insertSnooze(snooze: SnoozedOccurrenceEntity): Long

    @Query("SELECT * FROM goals WHERE id = :id LIMIT 1") suspend fun getGoal(id: Long): GoalEntity?
    @Query("SELECT * FROM schedule_slots WHERE goalId = :goalId ORDER BY sortOrder") suspend fun getSlots(goalId: Long): List<ScheduleSlotEntity>
    @Query("DELETE FROM schedule_slots WHERE goalId = :goalId") suspend fun deleteSlots(goalId: Long)
    @Query("UPDATE goals SET status = 'ARCHIVED', archivedEpochDay = :epochDay WHERE id = :id AND status = 'ACTIVE'") suspend fun archiveGoal(id: Long, epochDay: Long)

    @Query("SELECT * FROM goals WHERE status = 'ACTIVE' ORDER BY id DESC") fun observeGoals(): Flow<List<GoalEntity>>
    @Query("SELECT * FROM goals WHERE status IN ('ACTIVE', 'ARCHIVED') ORDER BY id DESC") fun observeAnalyticsGoals(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM completions WHERE effectiveEpochDay BETWEEN :from AND :to ORDER BY effectiveEpochDay")
    fun observeCompletions(from: Long, to: Long): Flow<List<CompletionEntity>>

    @Query("SELECT * FROM schedule_slots ORDER BY goalId, sortOrder")
    fun observeSlots(): Flow<List<ScheduleSlotEntity>>
    @Query("SELECT * FROM schedule_slots ORDER BY goalId, sortOrder") suspend fun getSlots(): List<ScheduleSlotEntity>

    @Query("SELECT * FROM skipped_occurrences WHERE effectiveEpochDay BETWEEN :from AND :to ORDER BY effectiveEpochDay")
    fun observeSkips(from: Long, to: Long): Flow<List<SkippedOccurrenceEntity>>

    @Query("SELECT * FROM snoozed_occurrences WHERE effectiveEpochDay BETWEEN :from AND :to ORDER BY effectiveEpochDay")
    fun observeSnoozes(from: Long, to: Long): Flow<List<SnoozedOccurrenceEntity>>

    @Query("DELETE FROM completions WHERE id = (SELECT id FROM completions WHERE goalId = :goalId AND effectiveEpochDay = :epochDay ORDER BY completedAt DESC LIMIT 1)")
    suspend fun undoLast(goalId: Long, epochDay: Long)
}
