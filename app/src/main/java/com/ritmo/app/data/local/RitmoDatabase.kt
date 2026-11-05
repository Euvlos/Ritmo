package com.ritmo.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [GoalEntity::class, CompletionEntity::class, ScheduleSlotEntity::class, SkippedOccurrenceEntity::class, SnoozedOccurrenceEntity::class], version = 8, exportSchema = false)
@TypeConverters(Converters::class)
abstract class RitmoDatabase : RoomDatabase() {
    abstract fun goalDao(): GoalDao
}
