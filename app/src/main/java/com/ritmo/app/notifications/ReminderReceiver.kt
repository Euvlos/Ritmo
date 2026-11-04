package com.ritmo.app.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.ritmo.app.MainActivity
import com.ritmo.app.R
import com.ritmo.app.domain.model.Goal
import com.ritmo.app.domain.model.RecurrenceType
import com.ritmo.app.data.local.GoalDao
import com.ritmo.app.data.local.CompletionEntity
import com.ritmo.app.data.local.SkippedOccurrenceEntity
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDate
import javax.inject.Inject
import java.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.ritmo.app.widget.RitmoWidgetProvider

@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {
    @Inject lateinit var scheduler: ReminderScheduler
    @Inject lateinit var goalDao: GoalDao

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(GOAL_ID, 0)
        val title = intent.getStringExtra(GOAL_TITLE) ?: return
        val required = intent.getIntExtra(REQUIRED, 1)
        val minutes = intent.getIntExtra(REMINDER_MINUTES, -1)
        val requestCode = intent.getIntExtra(REQUEST_CODE, id.toInt())
        val manager = context.getSystemService(NotificationManager::class.java)
        if (intent.action == ACTION_COMPLETE) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    goalDao.insertCompletion(CompletionEntity(goalId = id, effectiveEpochDay = LocalDate.now().toEpochDay(), completedAt = Instant.now()))
                    RitmoWidgetProvider.updateAll(context)
                    manager.cancel(requestCode)
                } finally { pendingResult.finish() }
            }
            return
        }
        if (intent.action == ACTION_SKIP) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    goalDao.insertSkip(SkippedOccurrenceEntity(goalId = id, effectiveEpochDay = LocalDate.now().toEpochDay(), slotTimeMinutes = minutes.takeIf { it >= 0 }, skippedAt = Instant.now()))
                    manager.cancel(requestCode)
                } finally { pendingResult.finish() }
            }
            return
        }
        if (intent.action == ACTION_SNOOZE) {
            val retry = Intent(context, ReminderReceiver::class.java).putExtras(intent.extras ?: android.os.Bundle()).setAction(ACTION_TRIGGER)
            val pending = PendingIntent.getBroadcast(context, requestCode + 1, retry, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val alarm = context.getSystemService(AlarmManager::class.java)
            val triggerAt = System.currentTimeMillis() + 15 * 60_000L
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S || alarm.canScheduleExactAlarms()) alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            else alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            manager.cancel(requestCode)
            return
        }
        manager.createNotificationChannel(NotificationChannel(CHANNEL, context.getString(R.string.reminders), NotificationManager.IMPORTANCE_DEFAULT))
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED || android.os.Build.VERSION.SDK_INT < 33) {
            val open = PendingIntent.getActivity(context, id.toInt(), Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val complete = PendingIntent.getBroadcast(context, requestCode + 10, Intent(context, ReminderReceiver::class.java).putExtras(intent.extras ?: android.os.Bundle()).setAction(ACTION_COMPLETE), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val snooze = PendingIntent.getActivity(context, requestCode + 20, Intent(context, SnoozeActivity::class.java).putExtras(intent.extras ?: android.os.Bundle()), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val skip = PendingIntent.getBroadcast(context, requestCode + 30, Intent(context, ReminderReceiver::class.java).putExtras(intent.extras ?: android.os.Bundle()).setAction(ACTION_SKIP), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val completeAction = NotificationCompat.Action(0, "Выполнено", complete)
            manager.notify(requestCode, NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(context.resources.getQuantityString(R.plurals.reminder_progress, required, required))
                .setContentIntent(open)
                .addAction(completeAction)
                .addAction(0, "Отложить", snooze)
                .addAction(0, "Пропустить", skip)
                .extend(NotificationCompat.WearableExtender().addAction(completeAction))
                .setAutoCancel(true).build())
        }
        scheduler.schedule(Goal(
            id = id, title = title, timesPerDay = required,
            startDate = LocalDate.ofEpochDay(intent.getLongExtra(START_DAY, LocalDate.now().toEpochDay())),
            recurrenceType = RecurrenceType.valueOf(intent.getStringExtra(RECURRENCE) ?: RecurrenceType.DAILY.name),
            repeatInterval = intent.getIntExtra(REPEAT_INTERVAL, 1), reminderTimeMinutes = minutes.takeIf { it >= 0 },
            weekdaysMask = intent.getIntExtra(WEEKDAYS, 127), monthDay = intent.getIntExtra(MONTH_DAY, 1),
            executionTimes = listOfNotNull(minutes.takeIf { it >= 0 }),
        ))
    }

    companion object {
        const val GOAL_ID = "goal_id"; const val GOAL_TITLE = "goal_title"; const val REQUIRED = "required"
        const val REMINDER_MINUTES = "reminder_minutes"; const val START_DAY = "start_day"
        const val REPEAT_INTERVAL = "repeat_interval"; const val RECURRENCE = "recurrence"
        const val WEEKDAYS = "weekdays"; const val MONTH_DAY = "month_day"; const val REQUEST_CODE = "request_code"; private const val CHANNEL = "goal_reminders"
        const val ACTION_TRIGGER = "com.ritmo.app.reminder.TRIGGER"
        const val ACTION_COMPLETE = "com.ritmo.app.reminder.COMPLETE"
        const val ACTION_SNOOZE = "com.ritmo.app.reminder.SNOOZE"
        const val ACTION_SKIP = "com.ritmo.app.reminder.SKIP"
    }
}
