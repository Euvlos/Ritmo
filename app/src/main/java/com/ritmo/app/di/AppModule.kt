package com.ritmo.app.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ritmo.app.data.local.GoalDao
import com.ritmo.app.data.local.RitmoDatabase
import com.ritmo.app.data.repository.RoomGoalRepository
import com.ritmo.app.domain.repository.GoalRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module @InstallIn(SingletonComponent::class)
object DatabaseModule {
    private val migration1To2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE goals ADD COLUMN recurrenceType TEXT NOT NULL DEFAULT 'DAILY'")
            db.execSQL("ALTER TABLE goals ADD COLUMN repeatInterval INTEGER NOT NULL DEFAULT 1")
            db.execSQL("ALTER TABLE goals ADD COLUMN timesPerDay INTEGER NOT NULL DEFAULT 1")
            db.execSQL("ALTER TABLE goals ADD COLUMN reminderTimeMinutes INTEGER DEFAULT NULL")
        }
    }
    private val migration2To3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE goals ADD COLUMN weekdaysMask INTEGER NOT NULL DEFAULT 127")
            db.execSQL("ALTER TABLE goals ADD COLUMN monthDay INTEGER NOT NULL DEFAULT 1")
        }
    }
    private val migration3To4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS schedule_slots (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, goalId INTEGER NOT NULL, localTimeMinutes INTEGER NOT NULL, sortOrder INTEGER NOT NULL, FOREIGN KEY(goalId) REFERENCES goals(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_schedule_slots_goalId ON schedule_slots(goalId)")
        }
    }
    private val migration4To5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS skipped_occurrences (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, goalId INTEGER NOT NULL, effectiveEpochDay INTEGER NOT NULL, slotTimeMinutes INTEGER, skippedAt INTEGER NOT NULL, FOREIGN KEY(goalId) REFERENCES goals(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_skipped_occurrences_goalId ON skipped_occurrences(goalId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_skipped_occurrences_goalId_effectiveEpochDay ON skipped_occurrences(goalId, effectiveEpochDay)")
        }
    }
    private val migration5To6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS snoozed_occurrences (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, goalId INTEGER NOT NULL, effectiveEpochDay INTEGER NOT NULL, slotTimeMinutes INTEGER, requestedAt INTEGER NOT NULL, snoozedUntil INTEGER NOT NULL, FOREIGN KEY(goalId) REFERENCES goals(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_snoozed_occurrences_goalId ON snoozed_occurrences(goalId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_snoozed_occurrences_goalId_effectiveEpochDay ON snoozed_occurrences(goalId, effectiveEpochDay)")
        }
    }
    private val migration6To7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) { db.execSQL("ALTER TABLE goals ADD COLUMN archivedEpochDay INTEGER DEFAULT NULL") }
    }
    private val migration7To8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE goals ADD COLUMN reminderMode TEXT NOT NULL DEFAULT 'NOTIFICATION'")
            db.execSQL("ALTER TABLE goals ADD COLUMN alarmSoundUri TEXT DEFAULT NULL")
            db.execSQL("ALTER TABLE goals ADD COLUMN alarmSoundName TEXT DEFAULT NULL")
        }
    }
    private val migration8To9 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE goals ADD COLUMN earlyCompletionMinutes INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE schedule_slots ADD COLUMN reminderMode TEXT NOT NULL DEFAULT 'NOTIFICATION'")
            db.execSQL("ALTER TABLE schedule_slots ADD COLUMN alarmSoundUri TEXT DEFAULT NULL")
            db.execSQL("ALTER TABLE schedule_slots ADD COLUMN alarmSoundName TEXT DEFAULT NULL")
            db.execSQL("UPDATE schedule_slots SET reminderMode = (SELECT reminderMode FROM goals WHERE goals.id = schedule_slots.goalId), alarmSoundUri = (SELECT alarmSoundUri FROM goals WHERE goals.id = schedule_slots.goalId), alarmSoundName = (SELECT alarmSoundName FROM goals WHERE goals.id = schedule_slots.goalId)")
        }
    }
    @Provides @Singleton fun database(@ApplicationContext context: Context): RitmoDatabase =
        Room.databaseBuilder(context, RitmoDatabase::class.java, "ritmo.db").addMigrations(migration1To2, migration2To3, migration3To4, migration4To5, migration5To6, migration6To7, migration7To8, migration8To9).build()
    @Provides fun goalDao(database: RitmoDatabase): GoalDao = database.goalDao()
}

@Module @InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton abstract fun goalRepository(impl: RoomGoalRepository): GoalRepository
}
