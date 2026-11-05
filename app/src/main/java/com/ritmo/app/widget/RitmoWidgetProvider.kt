package com.ritmo.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import com.ritmo.app.MainActivity
import com.ritmo.app.R
import com.ritmo.app.data.local.CompletionEntity
import com.ritmo.app.data.local.GoalDao
import com.ritmo.app.data.local.GoalEntity
import com.ritmo.app.data.local.toDomain
import com.ritmo.app.domain.model.DailyGoal
import com.ritmo.app.domain.model.availableProgressAt
import com.ritmo.app.domain.model.isScheduledOn
import com.ritmo.app.domain.model.nextExecutionAfter
import com.ritmo.app.domain.model.requiredProgress
import com.ritmo.app.domain.model.GoalReminder
import dagger.hilt.android.AndroidEntryPoint
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@AndroidEntryPoint
class RitmoWidgetProvider : AppWidgetProvider() {
    @Inject lateinit var dao: GoalDao

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) = refresh(context, manager, ids)
    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: Bundle) = refresh(context, manager, intArrayOf(id))

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_COMPLETE) {
            val result = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try { completionMutex.withLock { complete(context, intent.getLongExtra(EXTRA_GOAL_ID, -1)) } } finally { result.finish() }
            }
        }
    }

    private fun refresh(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val result = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val goals = todayGoals().sortedWith(compareBy<DailyGoal> { it.isComplete }.thenBy { it.goal.title.lowercase() })
                val now = LocalTime.now(); val minute = now.hour * 60 + now.minute
                val open = PendingIntent.getActivity(context, 7100, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                ids.forEach { id ->
                    val height = manager.getAppWidgetOptions(id).getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 140)
                    val maxRows = ((height - 80) / 48).coerceIn(1, 6)
                    val views = RemoteViews(context.packageName, R.layout.ritmo_widget)
                    val done = goals.count { it.isComplete }
                    views.setTextViewText(R.id.widget_progress, context.getString(R.string.widget_progress_short, done, goals.size))
                    views.setProgressBar(R.id.widget_progress_bar, 100, if (goals.isEmpty()) 0 else done * 100 / goals.size, false)
                    views.setViewVisibility(R.id.widget_empty, if (goals.isEmpty()) View.VISIBLE else View.GONE)
                    views.setOnClickPendingIntent(R.id.widget_header, open)
                    views.removeAllViews(R.id.widget_rows)
                    goals.take(maxRows).forEach { daily ->
                        val goal = daily.goal; val available = !daily.isComplete && daily.completed < goal.availableProgressAt(minute)
                        val row = RemoteViews(context.packageName, R.layout.ritmo_widget_row)
                        row.setTextViewText(R.id.widget_row_title, goal.title)
                        val next = goal.nextExecutionAfter(minute)
                        val detail = if (daily.isComplete) context.getString(R.string.widget_completed) else if (!available && next != null) context.getString(R.string.widget_next_time, String.format(Locale.ROOT, "%02d:%02d", next / 60, next % 60)) else context.getString(R.string.widget_goal_progress, daily.completed, goal.requiredProgress)
                        row.setTextViewText(R.id.widget_row_detail, detail)
                        row.setTextViewText(R.id.widget_row_action, if (daily.isComplete) "✓" else if (available) "+" else "·")
                        row.setFloat(R.id.widget_row_action, "setAlpha", if (available) 1f else .45f)
                        row.setContentDescription(R.id.widget_row_action, context.getString(if (available) R.string.widget_mark_done else if (daily.isComplete) R.string.widget_completed else R.string.widget_not_available))
                        row.setOnClickPendingIntent(R.id.widget_row_open, open)
                        if (available) {
                            val action = Intent(context, RitmoWidgetProvider::class.java).setAction(ACTION_COMPLETE).setData(Uri.parse("ritmo://widget/goal/${goal.id}")).putExtra(EXTRA_GOAL_ID, goal.id)
                            row.setOnClickPendingIntent(R.id.widget_row_action, PendingIntent.getBroadcast(context, 0, action, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
                        }
                        views.addView(R.id.widget_rows, row)
                    }
                    val remaining = goals.size - maxRows
                    views.setViewVisibility(R.id.widget_more, if (remaining > 0) View.VISIBLE else View.GONE)
                    if (remaining > 0) { views.setTextViewText(R.id.widget_more, context.getString(R.string.widget_more_goals, remaining)); views.setOnClickPendingIntent(R.id.widget_more, open) }
                    manager.updateAppWidget(id, views)
                }
            } finally { result.finish() }
        }
    }

    private suspend fun complete(context: Context, goalId: Long) {
        val today = LocalDate.now(); val now = LocalTime.now(); val minute = now.hour * 60 + now.minute
        val goal = todayGoals().firstOrNull { it.goal.id == goalId }
        if (goal != null && !goal.isComplete && goal.completed < goal.goal.availableProgressAt(minute)) {
            dao.insertCompletion(CompletionEntity(goalId = goalId, effectiveEpochDay = today.toEpochDay(), completedAt = Instant.now()))
        }
        updateAll(context)
    }

    private suspend fun todayGoals(): List<DailyGoal> {
        val today = LocalDate.now(); val slots = dao.getSlots().groupBy { it.goalId }
        return dao.getForDay(today.toEpochDay()).map { row ->
            val goalSlots = slots[row.id].orEmpty()
            val goal = GoalEntity(row.id, row.title, row.type, row.targetValue, row.unit, row.color, row.startEpochDay, row.status, row.recurrenceType, row.repeatInterval, row.timesPerDay, row.reminderTimeMinutes, row.weekdaysMask, row.monthDay, row.archivedEpochDay, row.reminderMode, row.alarmSoundUri, row.alarmSoundName, row.earlyCompletionMinutes).toDomain().copy(executionTimes = goalSlots.map { it.localTimeMinutes }, reminders = goalSlots.map { GoalReminder(it.localTimeMinutes, it.reminderMode, it.alarmSoundUri, it.alarmSoundName) })
            DailyGoal(goal, row.completed)
        }.filter { it.goal.isScheduledOn(today) }
    }

    companion object {
        private const val ACTION_COMPLETE = "com.ritmo.app.widget.COMPLETE_GOAL"
        private const val EXTRA_GOAL_ID = "goal_id"
        private val completionMutex = Mutex()
        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context); val component = ComponentName(context, RitmoWidgetProvider::class.java)
            context.sendBroadcast(Intent(context, RitmoWidgetProvider::class.java).setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE).putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, manager.getAppWidgetIds(component)))
        }
    }
}
