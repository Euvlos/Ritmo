package com.ritmo.app.ui.goaldetails

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ritmo.app.domain.model.scheduleSummary
import com.ritmo.app.ui.statistics.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalDetailsScreen(onBack: () -> Unit, onEdit: () -> Unit, viewModel: GoalDetailsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState(); val goal = state.goal
    var confirmDelete by remember { mutableStateOf(false) }
    if (confirmDelete && goal != null) AlertDialog(onDismissRequest = { confirmDelete = false }, title = { Text("Удалить цель?") }, text = { Text("Цель «${goal.title}» исчезнет из активных целей, а её история останется в аналитике.") }, confirmButton = { TextButton(onClick = { confirmDelete = false; viewModel.delete(goal, onBack) }) { Text("Удалить", color = MaterialTheme.colorScheme.error) } }, dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Отмена") } })
    Scaffold(topBar = { TopAppBar(title = { Text(goal?.title ?: "Цель") }, navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Назад") } }, actions = { if (goal != null && goal.status == com.ritmo.app.domain.model.GoalStatus.ACTIVE) { IconButton(onClick = onEdit) { Icon(Icons.Rounded.Edit, "Редактировать цель") }; IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Rounded.Delete, "Удалить цель") } } }) }) { padding ->
        if (goal == null) { Box(Modifier.fillMaxSize().padding(padding).padding(20.dp)) { CircularProgressIndicator() }; return@Scaffold }
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { AnalyticsPeriodSelector(state.range, viewModel::period, viewModel::custom) }
            item { QualityHeader(state.metrics, state.trend) }
            item { InsightCard(state.insight) }
            item { MetricGrid("Выполнение" to "${state.metrics.completionRate}%", "Стабильность" to "${state.metrics.consistency}%", "Вовремя" to "${state.metrics.timeliness}%", "Тренд" to trendLabel(state.trend), "Пропущено" to state.metrics.skippedOccurrences.toString(), "Отложено" to state.metrics.snoozedOccurrences.toString(), "Текущая серия" to state.metrics.currentStreak.toString(), "Лучшая серия" to state.metrics.bestStreak.toString()) }
            item { Text("График выполнения", style = MaterialTheme.typography.titleLarge); Spacer(Modifier.height(10.dp)); BarChart(state.metrics.dailyProgress, Modifier.fillMaxWidth().height(170.dp)) }
            item { Text("Итоги периодов", style = MaterialTheme.typography.titleLarge); Spacer(Modifier.height(10.dp)); MetricGrid("Выполнено" to state.metrics.completedPeriods.toString(), "Частично" to state.metrics.partialPeriods.toString(), "Не выполнено" to state.metrics.missedPeriods.toString()) }
            item { HorizontalDivider(); Spacer(Modifier.height(16.dp)); Text("Расписание", style = MaterialTheme.typography.titleLarge); Spacer(Modifier.height(8.dp)); Text(goal.scheduleSummary()); if (goal.reminders.isNotEmpty()) goal.reminders.forEach { reminder -> Text(String.format(Locale.ROOT, "%02d:%02d · %s", reminder.timeMinutes / 60, reminder.timeMinutes % 60, reminderModeLabel(reminder.mode)), color = MaterialTheme.colorScheme.onSurfaceVariant) } else Text(goal.reminderTimeMinutes?.let { String.format(Locale.ROOT, "Напоминание в %02d:%02d · %s", it / 60, it % 60, reminderModeLabel(goal.reminderMode)) } ?: "Без отдельного напоминания", color = MaterialTheme.colorScheme.onSurfaceVariant); if (goal.executionTimes.isNotEmpty()) Text(if (goal.earlyCompletionMinutes == 0) "Отметка доступна с наступления времени" else "Можно отметить за ${goal.earlyCompletionMinutes} мин", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}
private fun reminderModeLabel(mode: com.ritmo.app.domain.model.ReminderMode) = when (mode) { com.ritmo.app.domain.model.ReminderMode.NOTIFICATION -> "тихое уведомление"; com.ritmo.app.domain.model.ReminderMode.VIBRATION -> "вибрация"; com.ritmo.app.domain.model.ReminderMode.ALARM -> "будильник" }
