package com.ritmo.app.data.local

import androidx.room.TypeConverter
import com.ritmo.app.domain.model.GoalStatus
import com.ritmo.app.domain.model.GoalType
import com.ritmo.app.domain.model.RecurrenceType
import java.time.Instant

class Converters {
    @TypeConverter fun instant(value: Long): Instant = Instant.ofEpochMilli(value)
    @TypeConverter fun instant(value: Instant): Long = value.toEpochMilli()
    @TypeConverter fun goalType(value: String): GoalType = GoalType.valueOf(value)
    @TypeConverter fun goalType(value: GoalType): String = value.name
    @TypeConverter fun goalStatus(value: String): GoalStatus = GoalStatus.valueOf(value)
    @TypeConverter fun goalStatus(value: GoalStatus): String = value.name
    @TypeConverter fun recurrenceType(value: String): RecurrenceType = RecurrenceType.valueOf(value)
    @TypeConverter fun recurrenceType(value: RecurrenceType): String = value.name
}
