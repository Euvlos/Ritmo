package com.ritmo.app.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import com.ritmo.app.domain.model.Goal
import com.ritmo.app.domain.model.isScheduledOn
import com.ritmo.app.domain.model.requiredProgress
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderScheduler @Inject constructor(@ApplicationContext private val context: Context) {
    fun cancel(goal: Goal) {
        val alarm = context.getSystemService(AlarmManager::class.java)
        val notifications = context.getSystemService(NotificationManager::class.java)
        goal.executionTimes.ifEmpty { listOfNotNull(goal.reminderTimeMinutes) }.distinct().forEach { minutes ->
            val code = ((goal.id * 1009 + minutes).and(0x7fffffff)).toInt()
            listOf(code to null, (code + 1) to ReminderReceiver.ACTION_TRIGGER).forEach { (requestCode, action) ->
                val intent = Intent(context, ReminderReceiver::class.java).setAction(action)
                PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)?.let { alarm.cancel(it); it.cancel() }
            }
            notifications.cancel(code)
        }
    }
    fun schedule(goal: Goal, after: LocalDateTime = LocalDateTime.now()) {
        val times = goal.executionTimes.ifEmpty { listOfNotNull(goal.reminderTimeMinutes) }
        times.forEach { scheduleAt(goal, it, after) }
    }

    private fun scheduleAt(goal: Goal, minutes: Int, after: LocalDateTime) {
        val time = LocalTime.of(minutes / 60, minutes % 60)
        var date = maxOf(LocalDate.now(), goal.startDate)
        var next = LocalDateTime.of(date, time)
        if (!next.isAfter(after)) { date = date.plusDays(1); next = LocalDateTime.of(date, time) }
        while (!goal.isScheduledOn(date)) { date = date.plusDays(1); next = LocalDateTime.of(date, time) }
        val intent = Intent(context, ReminderReceiver::class.java)
            .putExtra(ReminderReceiver.GOAL_ID, goal.id)
            .putExtra(ReminderReceiver.GOAL_TITLE, goal.title)
            .putExtra(ReminderReceiver.REQUIRED, goal.requiredProgress)
            .putExtra(ReminderReceiver.REMINDER_MINUTES, minutes)
            .putExtra(ReminderReceiver.START_DAY, goal.startDate.toEpochDay())
            .putExtra(ReminderReceiver.REPEAT_INTERVAL, goal.repeatInterval)
            .putExtra(ReminderReceiver.RECURRENCE, goal.recurrenceType.name)
            .putExtra(ReminderReceiver.WEEKDAYS, goal.weekdaysMask)
            .putExtra(ReminderReceiver.MONTH_DAY, goal.monthDay)
            .putExtra(ReminderReceiver.REMINDER_MODE, goal.reminderMode.name)
            .putExtra(ReminderReceiver.SOUND_URI, goal.alarmSoundUri)
            .putExtra(ReminderReceiver.SOUND_NAME, goal.alarmSoundName)
        val requestCode = ((goal.id * 1009 + minutes).and(0x7fffffff)).toInt()
        intent.putExtra(ReminderReceiver.REQUEST_CODE, requestCode)
        val pending = PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val alarm = context.getSystemService(AlarmManager::class.java)
        val triggerAt = next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S || alarm.canScheduleExactAlarms()) {
            alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        } else {
            alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        }
    }
}
