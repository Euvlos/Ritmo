package com.ritmo.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
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
import dagger.hilt.android.AndroidEntryPoint
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RitmoWidgetProvider : AppWidgetProvider() {
    @Inject lateinit var dao: GoalDao

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) = refresh(context, manager, ids)
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_COMPLETE_NEXT) {
            val result = goAsync()
            CoroutineScope(Dispatchers.IO).launch { try { completeNext(context) } finally { result.finish() } }
        }
    }

    private fun refresh(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val result = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val goals = todayGoals(); val done = goals.count { it.isComplete }
                ids.forEach { id ->
                    val views = RemoteViews(context.packageName, R.layout.ritmo_widget)
                    views.setTextViewText(R.id.widget_progress, if (goals.isEmpty()) context.getString(R.string.widget_empty) else context.getString(R.string.widget_done, done, goals.size))
                    views.setTextViewText(R.id.widget_goals, goals.take(3).joinToString("\n") { (if (it.isComplete) "✓ " else "○ ") + it.goal.title })
                    views.setOnClickPendingIntent(R.id.widget_root, PendingIntent.getActivity(context, 7100, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
                    views.setOnClickPendingIntent(R.id.widget_complete, PendingIntent.getBroadcast(context, 7101, Intent(context, RitmoWidgetProvider::class.java).setAction(ACTION_COMPLETE_NEXT), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
                    manager.updateAppWidget(id, views)
                }
            } finally { result.finish() }
        }
    }

    private suspend fun completeNext(context: Context) {
        val today = LocalDate.now(); val now = LocalTime.now(); val minute = now.hour * 60 + now.minute
        val next = todayGoals().firstOrNull { !it.isComplete && it.completed < it.goal.availableProgressAt(minute) } ?: return updateAll(context)
        dao.insertCompletion(CompletionEntity(goalId = next.goal.id, effectiveEpochDay = today.toEpochDay(), completedAt = Instant.now()))
        updateAll(context)
    }

    private suspend fun todayGoals(): List<DailyGoal> {
        val today = LocalDate.now(); val times = dao.getSlots().groupBy { it.goalId }.mapValues { row -> row.value.map { it.localTimeMinutes } }
        return dao.getForDay(today.toEpochDay()).map { row ->
            val goal = GoalEntity(row.id, row.title, row.type, row.targetValue, row.unit, row.color, row.startEpochDay, row.status, row.recurrenceType, row.repeatInterval, row.timesPerDay, row.reminderTimeMinutes, row.weekdaysMask, row.monthDay).toDomain().copy(executionTimes = times[row.id].orEmpty())
            DailyGoal(goal, row.completed)
        }.filter { it.goal.isScheduledOn(today) }
    }

    companion object {
        private const val ACTION_COMPLETE_NEXT = "com.ritmo.app.widget.COMPLETE_NEXT"
        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context); val component = ComponentName(context, RitmoWidgetProvider::class.java)
            context.sendBroadcast(Intent(context, RitmoWidgetProvider::class.java).setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE).putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, manager.getAppWidgetIds(component)))
        }
    }
}
