package com.ritmo.app.ui.statistics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ritmo.app.domain.analytics.QualityMetrics
import com.ritmo.app.domain.analytics.AnalyticsPeriod
import com.ritmo.app.domain.analytics.AnalyticsRange
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(onBack: () -> Unit, onGoal: (Long) -> Unit, viewModel: StatisticsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    Scaffold(topBar = { TopAppBar(title = { Text("Аналитика") }, navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Назад") } }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item { AnalyticsPeriodSelector(state.range, viewModel::period, viewModel::custom) }
            item { QualityHeader(state.metrics, state.trend) }
            item { InsightCard(state.insight) }
            item { MetricGrid("Выполнение" to "${state.metrics.completionRate}%", "Стабильность" to "${state.metrics.consistency}%", "Вовремя" to "${state.metrics.timeliness}%", "Тренд" to trendLabel(state.trend)) }
            item {
                Text("Что происходило", style = MaterialTheme.typography.titleLarge); Spacer(Modifier.height(10.dp))
                MetricGrid("Выполнено" to state.metrics.completedPeriods.toString(), "Частично" to state.metrics.partialPeriods.toString(), "Не выполнено" to state.metrics.missedPeriods.toString(), "Пропущено" to state.metrics.skippedOccurrences.toString(), "Отложено" to state.metrics.snoozedOccurrences.toString(), "Лучшая серия" to state.metrics.bestStreak.toString())
            }
            item { Text("Динамика выполнения", style = MaterialTheme.typography.titleLarge); Spacer(Modifier.height(10.dp)); BarChart(state.metrics.dailyProgress, Modifier.fillMaxWidth().height(150.dp)) }
            item { Text("По целям", style = MaterialTheme.typography.titleLarge) }
            items(state.goals, key = { it.goal.id }) { stat ->
                Card(Modifier.fillMaxWidth().clickable { onGoal(stat.goal.id) }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Column { Text(stat.goal.title, style = MaterialTheme.typography.titleMedium); Text(if (stat.goal.status == com.ritmo.app.domain.model.GoalStatus.ARCHIVED) "Удалена · ${stat.metrics.grade}" else stat.metrics.grade, color = MaterialTheme.colorScheme.onSurfaceVariant) }; Text("${stat.metrics.qualityScore}", style = MaterialTheme.typography.headlineSmall) }
                        LinearProgressIndicator({ stat.metrics.qualityScore / 100f }, Modifier.fillMaxWidth(), strokeCap = StrokeCap.Round)
                        Text("Выполнение ${stat.metrics.completionRate}% · ${trendLabel(stat.trend)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Пропущено ${stat.metrics.skippedOccurrences} · отложено ${stat.metrics.snoozedOccurrences}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(stat.insight, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsPeriodSelector(range: AnalyticsRange, select: (AnalyticsPeriod) -> Unit, custom: (java.time.LocalDate, java.time.LocalDate) -> Unit) {
    var showPicker by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(listOf(AnalyticsPeriod.DAY to "День", AnalyticsPeriod.WEEK to "Неделя", AnalyticsPeriod.MONTH to "Месяц", AnalyticsPeriod.YEAR to "Год", AnalyticsPeriod.CUSTOM to "Период")) { (period, label) ->
                FilterChip(range.period == period, { if (period == AnalyticsPeriod.CUSTOM) showPicker = true else select(period) }, { Text(label) })
            }
        }
        Text("${range.from.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))} — ${range.to.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (showPicker) {
        val picker = rememberDateRangePickerState(initialSelectedStartDateMillis = range.from.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(), initialSelectedEndDateMillis = range.to.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(onDismissRequest = { showPicker = false }, confirmButton = { TextButton({ val start = picker.selectedStartDateMillis; val end = picker.selectedEndDateMillis; if (start != null && end != null) custom(Instant.ofEpochMilli(start).atZone(ZoneOffset.UTC).toLocalDate(), Instant.ofEpochMilli(end).atZone(ZoneOffset.UTC).toLocalDate()); showPicker = false }, enabled = picker.selectedStartDateMillis != null && picker.selectedEndDateMillis != null) { Text("Выбрать") } }, dismissButton = { TextButton({ showPicker = false }) { Text("Отмена") } }) {
            DateRangePicker(picker, title = { Text("Выберите период", Modifier.padding(16.dp)) }, modifier = Modifier.height(500.dp))
        }
    }
}

@Composable
fun QualityHeader(metrics: QualityMetrics, trend: Int) = Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), shape = MaterialTheme.shapes.extraLarge) {
    Column(Modifier.fillMaxWidth().padding(22.dp)) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) { Text("Качество выполнения", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium); Text(trendLabel(trend), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer, maxLines = 1) }; Text(metrics.grade); Text("${metrics.qualityScore}/100", style = MaterialTheme.typography.displaySmall) }
}

@Composable
fun InsightCard(text: String) = Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) { Column(Modifier.fillMaxWidth().padding(18.dp)) { Text("Разбор", style = MaterialTheme.typography.titleMedium); Spacer(Modifier.height(4.dp)); Text(text) } }

@Composable
fun MetricGrid(vararg metrics: Pair<String, String>) {
    var help by remember { mutableStateOf<Pair<String, String>?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { metrics.toList().chunked(2).forEach { row -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { row.forEach { (label, value) -> Card(Modifier.weight(1f).height(104.dp).clickable { help = label to metricExplanation(label) }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) { Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) { Text(value, style = MaterialTheme.typography.titleLarge, maxLines = 1); Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) { Text(label, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, maxLines = 1); Icon(Icons.Rounded.Info, "Что это", Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) } } } }; if (row.size == 1) Spacer(Modifier.weight(1f)) } } }
    help?.let { (title, text) -> AlertDialog(onDismissRequest = { help = null }, confirmButton = { TextButton({ help = null }) { Text("Понятно") } }, title = { Text(title) }, text = { Text(text) }) }
}

fun trendLabel(trend: Int) = when { trend > 0 -> "+$trend баллов"; trend < 0 -> "$trend баллов"; else -> "0 баллов" }

private fun metricExplanation(label: String): String = when (label) {
    "Выполнение" -> "Доля выполненного объёма от всего запланированного. Частичное выполнение тоже учитывается."
    "Стабильность" -> "Насколько равномерно цель выполнялась без резких провалов между активными днями."
    "Вовремя" -> "Как часто цель выполнялась без переноса уведомления на другое время."
    "Тренд" -> "Изменение итоговой оценки качества относительно предыдущих 30 дней, в баллах."
    "Выполнено" -> "Активные дни, в которые выполнена вся заданная норма."
    "Частично" -> "Активные дни, в которые выполнена только часть заданной нормы."
    "Не выполнено" -> "Активные дни без выполнения и без явного нажатия «Пропустить»."
    "Пропущено" -> "Сколько раз нажата кнопка «Пропустить». Это считается осознанным невыполнением."
    "Отложено" -> "Сколько раз уведомление переносилось. Перенос снижает своевременность."
    "Текущая серия" -> "Количество последних активных дней подряд с полностью выполненной нормой."
    "Лучшая серия" -> "Самая длинная серия полностью выполненных активных дней за период."
    else -> "Показатель качества выполнения цели за выбранный период."
}

@Composable fun BarChart(values: List<Float>, modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary; val track = MaterialTheme.colorScheme.surfaceVariant
    Canvas(modifier) { if (values.isEmpty()) return@Canvas; val gap = 3.dp.toPx(); val width = (size.width - gap * (values.size - 1)) / values.size; values.forEachIndexed { index, value -> drawRoundRect(track, Offset(index * (width + gap), 0f), Size(width, size.height), cornerRadius = androidx.compose.ui.geometry.CornerRadius(width / 2)); val h = size.height * value.coerceIn(0f, 1f); drawRoundRect(primary, Offset(index * (width + gap), size.height - h), Size(width, h), cornerRadius = androidx.compose.ui.geometry.CornerRadius(width / 2)) } }
}
