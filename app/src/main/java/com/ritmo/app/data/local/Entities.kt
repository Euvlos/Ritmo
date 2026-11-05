package com.ritmo.app.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ritmo.app.domain.model.Goal
import com.ritmo.app.domain.model.GoalStatus
import com.ritmo.app.domain.model.GoalType
import com.ritmo.app.domain.model.RecurrenceType
import com.ritmo.app.domain.model.ReminderMode
import java.time.Instant
import java.time.LocalDate

@Entity(tableName = "goals", indices = [Index("status"), Index("startEpochDay")])
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val type: GoalType,
    val targetValue: Int,
    val unit: String,
    val color: Long,
    val startEpochDay: Long,
    val status: GoalStatus,
    val recurrenceType: RecurrenceType = RecurrenceType.DAILY,
    val repeatInterval: Int = 1,
    val timesPerDay: Int = 1,
    val reminderTimeMinutes: Int? = null,
    val weekdaysMask: Int = 0b1111111,
    val monthDay: Int = 1,
    val archivedEpochDay: Long? = null,
    val reminderMode: ReminderMode = ReminderMode.NOTIFICATION,
    val alarmSoundUri: String? = null,
    val alarmSoundName: String? = null,
    val earlyCompletionMinutes: Int = 0,
)

@Entity(
    tableName = "completions",
    foreignKeys = [ForeignKey(
        entity = GoalEntity::class,
        parentColumns = ["id"], childColumns = ["goalId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("goalId"), Index(value = ["goalId", "effectiveEpochDay"])],
)
data class CompletionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val goalId: Long,
    val effectiveEpochDay: Long,
    val completedAt: Instant,
    val value: Int = 1,
)

@Entity(
    tableName = "schedule_slots",
    foreignKeys = [ForeignKey(entity = GoalEntity::class, parentColumns = ["id"], childColumns = ["goalId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("goalId")],
)
data class ScheduleSlotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val goalId: Long,
    val localTimeMinutes: Int,
    val sortOrder: Int,
    val reminderMode: ReminderMode = ReminderMode.NOTIFICATION,
    val alarmSoundUri: String? = null,
    val alarmSoundName: String? = null,
)

@Entity(
    tableName = "skipped_occurrences",
    foreignKeys = [ForeignKey(entity = GoalEntity::class, parentColumns = ["id"], childColumns = ["goalId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("goalId"), Index(value = ["goalId", "effectiveEpochDay"])],
)
data class SkippedOccurrenceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val goalId: Long,
    val effectiveEpochDay: Long,
    val slotTimeMinutes: Int?,
    val skippedAt: Instant,
)

@Entity(
    tableName = "snoozed_occurrences",
    foreignKeys = [ForeignKey(entity = GoalEntity::class, parentColumns = ["id"], childColumns = ["goalId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("goalId"), Index(value = ["goalId", "effectiveEpochDay"])],
)
data class SnoozedOccurrenceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val goalId: Long,
    val effectiveEpochDay: Long,
    val slotTimeMinutes: Int?,
    val requestedAt: Instant,
    val snoozedUntil: Instant,
)

fun GoalEntity.toDomain() = Goal(id, title, type, targetValue, unit, color, LocalDate.ofEpochDay(startEpochDay), status, recurrenceType, repeatInterval, timesPerDay, reminderTimeMinutes, weekdaysMask, monthDay, archivedDate = archivedEpochDay?.let(LocalDate::ofEpochDay), reminderMode = reminderMode, alarmSoundUri = alarmSoundUri, alarmSoundName = alarmSoundName, earlyCompletionMinutes = earlyCompletionMinutes)
fun Goal.toEntity() = GoalEntity(id, title.trim(), type, targetValue.coerceAtLeast(1), unit.trim(), color, startDate.toEpochDay(), status, recurrenceType, repeatInterval.coerceAtLeast(1), timesPerDay.coerceAtLeast(1), reminderTimeMinutes, weekdaysMask, monthDay.coerceIn(1, 31), archivedEpochDay = archivedDate?.toEpochDay(), reminderMode = reminderMode, alarmSoundUri = alarmSoundUri, alarmSoundName = alarmSoundName, earlyCompletionMinutes = earlyCompletionMinutes)
