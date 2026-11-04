package com.ritmo.app.notifications

import android.app.AlarmManager
import android.app.AlertDialog
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.ritmo.app.data.local.GoalDao
import com.ritmo.app.data.local.SnoozedOccurrenceEntity
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SnoozeActivity : ComponentActivity() {
    @Inject lateinit var goalDao: GoalDao
    private val requestCode get() = intent.getIntExtra(ReminderReceiver.REQUEST_CODE, 0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getSystemService(NotificationManager::class.java).cancel(requestCode)
        AlertDialog.Builder(this)
            .setTitle("Отложить цель")
            .setItems(arrayOf("На 15 минут", "На 30 минут", "На 1 час", "Другой интервал…", "До конкретного времени…")) { _, which ->
                when (which) {
                    0 -> scheduleAfterMinutes(15)
                    1 -> scheduleAfterMinutes(30)
                    2 -> scheduleAfterMinutes(60)
                    3 -> showCustomInterval()
                    else -> showTimePicker()
                }
            }
            .setNegativeButton("Отмена") { _, _ -> finish() }
            .setOnCancelListener { finish() }
            .show()
    }

    private fun showCustomInterval() {
        val input = EditText(this).apply { inputType = InputType.TYPE_CLASS_NUMBER; hint = "Минуты"; setText("20"); selectAll() }
        val box = FrameLayout(this).apply { val pad = (24 * resources.displayMetrics.density).toInt(); setPadding(pad, 0, pad, 0); addView(input) }
        AlertDialog.Builder(this).setTitle("Через сколько минут?").setView(box)
            .setPositiveButton("Отложить") { _, _ -> scheduleAfterMinutes(input.text.toString().toLongOrNull()?.coerceIn(1, 24 * 60) ?: 15) }
            .setNegativeButton("Отмена") { _, _ -> finish() }.setOnCancelListener { finish() }.show()
    }

    private fun showTimePicker() {
        val now = LocalTime.now()
        TimePickerDialog(this, { _, hour, minute ->
            var target = LocalDateTime.now().withHour(hour).withMinute(minute).withSecond(0).withNano(0)
            if (!target.isAfter(LocalDateTime.now())) target = target.plusDays(1)
            scheduleAt(target.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
        }, now.hour, now.minute, true).apply { setOnCancelListener { finish() } }.show()
    }

    private fun scheduleAfterMinutes(minutes: Long) = scheduleAt(System.currentTimeMillis() + minutes * 60_000L)

    private fun scheduleAt(timestamp: Long) {
        val retry = Intent(this, ReminderReceiver::class.java).putExtras(intent.extras ?: Bundle()).setAction(ReminderReceiver.ACTION_TRIGGER)
        val pending = PendingIntent.getBroadcast(this, requestCode + 1, retry, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val alarm = getSystemService(AlarmManager::class.java)
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S || alarm.canScheduleExactAlarms()) alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timestamp, pending)
        else alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timestamp, pending)
        val goalId = intent.getLongExtra(ReminderReceiver.GOAL_ID, 0)
        val slot = intent.getIntExtra(ReminderReceiver.REMINDER_MINUTES, -1).takeIf { it >= 0 }
        CoroutineScope(Dispatchers.IO).launch {
            goalDao.insertSnooze(SnoozedOccurrenceEntity(goalId = goalId, effectiveEpochDay = LocalDate.now().toEpochDay(), slotTimeMinutes = slot, requestedAt = Instant.now(), snoozedUntil = Instant.ofEpochMilli(timestamp)))
        }
        finish()
    }
}
