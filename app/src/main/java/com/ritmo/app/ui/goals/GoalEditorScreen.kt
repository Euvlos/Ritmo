package com.ritmo.app.ui.goals

import android.Manifest
import android.app.TimePickerDialog
import android.os.Build
import android.provider.Settings
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ritmo.app.domain.model.GoalType
import com.ritmo.app.domain.model.RecurrenceType
import com.ritmo.app.domain.model.ReminderMode
import com.ritmo.app.domain.model.dayWord
import com.ritmo.app.domain.model.timesWord
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalEditorScreen(goalId: Long = 0, onBack: () -> Unit, viewModel: GoalEditorViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState(); val context = LocalContext.current
    LaunchedEffect(goalId) { viewModel.load(goalId) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    val customSound = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let(viewModel::importSound) }
    val systemSound = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        result.data?.getParcelableExtra<Uri>(android.media.RingtoneManager.EXTRA_RINGTONE_PICKED_URI)?.let { uri -> viewModel.alarmSound(uri.toString(), android.media.RingtoneManager.getRingtone(context, uri)?.getTitle(context) ?: "Системный будильник") }
    }
    Scaffold(
        topBar = { TopAppBar(title = { Column { Text(if (goalId == 0L) "Новая цель" else "Редактировать цель"); Text("Настрой под свой ритм", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) } }, navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Назад") } }) },
        bottomBar = { Surface(shadowElevation = 10.dp) { Button({ viewModel.save(onBack) }, Modifier.navigationBarsPadding().padding(16.dp).fillMaxWidth().height(56.dp), enabled = !state.saving && !state.loading) { if (state.saving || state.loading) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp) else { Icon(Icons.Rounded.Check, null); Spacer(Modifier.width(8.dp)); Text(if (goalId == 0L) "Создать цель" else "Сохранить") } } } },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            PreviewCard(state)
            SectionCard("Что отслеживаем", Icons.Rounded.Flag) {
                OutlinedTextField(state.title, viewModel::title, Modifier.fillMaxWidth(), placeholder = { Text("Например, выпить воду") }, label = { Text("Название цели") }, singleLine = true, isError = state.titleError, supportingText = if (state.titleError) {{ Text("Название не должно быть пустым") }} else null)
                Text("Способ выполнения", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ChoiceTile("Отметка", "Сделано или нет", Icons.Rounded.CheckCircle, state.type == GoalType.SIMPLE, { viewModel.type(GoalType.SIMPLE) }, Modifier.weight(1f))
                    ChoiceTile("Количество", "Счётчик значения", Icons.Rounded.AddCircle, state.type == GoalType.COUNTER, { viewModel.type(GoalType.COUNTER) }, Modifier.weight(1f))
                }
                if (state.type == GoalType.COUNTER) Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(state.targetValue.toString(), { it.toIntOrNull()?.let(viewModel::target) }, Modifier.weight(1f), label = { Text("Цель") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                    OutlinedTextField(state.unit, viewModel::unit, Modifier.weight(1f), label = { Text("Единица") }, singleLine = true)
                }
            }
            SectionCard("Когда выполнять", Icons.Rounded.Repeat) {
                if (state.recurrenceType == RecurrenceType.INTERVAL_DAYS || state.recurrenceType == RecurrenceType.DAILY) {
                    LabeledSlider("Раз во сколько дней", state.repeatInterval, 1..30, viewModel::interval, if (state.repeatInterval == 1) "Каждый день" else "Раз в ${state.repeatInterval} ${dayWord(state.repeatInterval)}")
                    LabeledSlider("Сколько раз в активный день", state.timesPerDay, 1..12, viewModel::times, timesWord(state.timesPerDay))
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text("Конкретные дни", fontWeight = FontWeight.SemiBold); Text("Дни недели или число месяца", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    Switch(state.recurrenceType == RecurrenceType.INTERVAL_WEEKS || state.recurrenceType == RecurrenceType.MONTHLY, viewModel::specificDays)
                }
                if (state.recurrenceType == RecurrenceType.INTERVAL_WEEKS || state.recurrenceType == RecurrenceType.MONTHLY) {
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        SegmentedButton(state.recurrenceType == RecurrenceType.INTERVAL_WEEKS, { viewModel.specificMode(RecurrenceType.INTERVAL_WEEKS) }, SegmentedButtonDefaults.itemShape(0, 2)) { Text("Дни недели") }
                        SegmentedButton(state.recurrenceType == RecurrenceType.MONTHLY, { viewModel.specificMode(RecurrenceType.MONTHLY) }, SegmentedButtonDefaults.itemShape(1, 2)) { Text("Число месяца") }
                    }
                    if (state.recurrenceType == RecurrenceType.INTERVAL_WEEKS) WeekdayPicker(state.weekdaysMask, viewModel::toggleWeekday)
                    else LabeledSlider("Число месяца", state.monthDay, 1..31, viewModel::monthDay, if (state.monthDay > 28) "В коротком месяце — последний день" else "${state.monthDay} число")
                    LabeledSlider("Сколько раз в выбранный день", state.timesPerDay, 1..12, viewModel::times, timesWord(state.timesPerDay))
                }
                HorizontalDivider()
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text("Задать время выполнения", fontWeight = FontWeight.SemiBold); Text("Каждое выполнение получит свой слот и уведомление", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    Switch(state.bindToTimes, { enabled -> viewModel.bindToTimes(enabled); if (enabled && Build.VERSION.SDK_INT >= 33) permission.launch(Manifest.permission.POST_NOTIFICATIONS); if (enabled) requestExactAlarmAccess(context) })
                }
                if (state.bindToTimes) {
                    state.executionTimes.forEachIndexed { index, time ->
                        Surface(Modifier.fillMaxWidth().clickable { pickTime(context, time) { viewModel.executionTime(index, it) } }, shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.secondaryContainer) {
                            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Surface(Modifier.size(32.dp), CircleShape, MaterialTheme.colorScheme.secondary) { Box(contentAlignment = Alignment.Center) { Text("${index + 1}", color = MaterialTheme.colorScheme.onSecondary) } }; Spacer(Modifier.width(12.dp)); Text("Выполнение ${index + 1}", Modifier.weight(1f)); Text(String.format(Locale.ROOT, "%02d:%02d", time / 60, time % 60), style = MaterialTheme.typography.titleMedium); Spacer(Modifier.width(6.dp)); Icon(Icons.Rounded.Edit, null, Modifier.size(18.dp)) }
                        }
                    }
                }
            }
            if (!state.bindToTimes) SectionCard("Отдельное напоминание", Icons.Rounded.Notifications) {
                Text("Необязательно. Напоминание придёт только в запланированный день.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (state.reminderTimeMinutes == null) {
                    OutlinedButton({ pickTime(context, 9 * 60) { viewModel.reminder(it); if (Build.VERSION.SDK_INT >= 33) permission.launch(Manifest.permission.POST_NOTIFICATIONS); requestExactAlarmAccess(context) } }, Modifier.fillMaxWidth().height(52.dp)) { Icon(Icons.Rounded.AddAlarm, null); Spacer(Modifier.width(8.dp)); Text("Добавить время") }
                } else {
                    val time = state.reminderTimeMinutes!!
                    Surface(Modifier.fillMaxWidth().clickable { pickTime(context, time, viewModel::reminder) }, color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.large) { Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Rounded.Alarm, null); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(String.format(Locale.ROOT, "%02d:%02d", time / 60, time % 60), style = MaterialTheme.typography.titleLarge); Text("Нажми, чтобы изменить", style = MaterialTheme.typography.bodySmall) }; IconButton({ viewModel.reminder(null) }) { Icon(Icons.Rounded.Close, "Убрать") } } }
                }
            }
            if (state.bindToTimes || state.reminderTimeMinutes != null) SectionCard("Тип напоминания", Icons.Rounded.Alarm) {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    listOf(ReminderMode.NOTIFICATION to "Тихое", ReminderMode.VIBRATION to "Вибрация", ReminderMode.ALARM to "Будильник").forEachIndexed { index, item ->
                        SegmentedButton(state.reminderMode == item.first, { viewModel.reminderMode(item.first) }, SegmentedButtonDefaults.itemShape(index, 3)) { Text(item.second) }
                    }
                }
                Text(when (state.reminderMode) { ReminderMode.NOTIFICATION -> "Обычное уведомление без вибрации"; ReminderMode.VIBRATION -> "Уведомление с вибрацией"; ReminderMode.ALARM -> "Полноэкранный будильник со звуком" }, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (state.reminderMode == ReminderMode.ALARM) {
                    Text("Мелодия: ${state.alarmSoundName ?: "Стандартный звук будильника"}", fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton({ systemSound.launch(Intent(android.media.RingtoneManager.ACTION_RINGTONE_PICKER).putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_TYPE, android.media.RingtoneManager.TYPE_ALARM).putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)) }, Modifier.weight(1f)) { Text("Системная") }
                        OutlinedButton({ customSound.launch(arrayOf("audio/*")) }, Modifier.weight(1f)) { Text("С устройства") }
                    }
                    if (state.customSounds.isNotEmpty()) {
                        Text("Моя библиотека", style = MaterialTheme.typography.labelLarge)
                        state.customSounds.forEach { sound -> FilterChip(state.alarmSoundUri == sound.uri, { viewModel.alarmSound(sound.uri, sound.name) }, { Text(sound.name) }, leadingIcon = { Icon(Icons.Rounded.MusicNote, null) }) }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable private fun PreviewCard(state: GoalEditorState) { Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), shape = MaterialTheme.shapes.extraLarge) { Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) { Surface(Modifier.size(48.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primary) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Bolt, null, tint = MaterialTheme.colorScheme.onPrimary) } }; Spacer(Modifier.width(14.dp)); Column { Text(state.title.ifBlank { "Твоя новая цель" }, style = MaterialTheme.typography.titleMedium); Text(scheduleText(state), color = MaterialTheme.colorScheme.onPrimaryContainer.copy(.72f)) } } } }

@Composable private fun SectionCard(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) { Card(shape = MaterialTheme.shapes.extraLarge, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(10.dp)); Text(title, style = MaterialTheme.typography.titleLarge) }; content() } } }

@Composable private fun ChoiceTile(title: String, subtitle: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) { Surface(modifier.clickable(onClick = onClick), shape = MaterialTheme.shapes.large, color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) { Icon(icon, null, tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant); Text(title, fontWeight = FontWeight.SemiBold); Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }

@Composable private fun RecurrenceGrid(selected: RecurrenceType, select: (RecurrenceType) -> Unit) { val options = listOf(RecurrenceType.DAILY to Triple("Ежедневно", "Каждый день", Icons.Rounded.Today), RecurrenceType.INTERVAL_DAYS to Triple("По дням", "Раз в N дней", Icons.Rounded.DateRange), RecurrenceType.INTERVAL_WEEKS to Triple("По неделям", "Выбранные дни", Icons.Rounded.ViewWeek), RecurrenceType.MONTHLY to Triple("По месяцам", "Конкретное число", Icons.Rounded.CalendarMonth)); Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { options.chunked(2).forEach { row -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { row.forEach { (type, text) -> ChoiceTile(text.first, text.second, text.third, selected == type, { select(type) }, Modifier.weight(1f)) } } } } }

@Composable private fun NumberStepper(label: String, value: Int, set: (Int) -> Unit, hint: String) { Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) { Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(label, fontWeight = FontWeight.SemiBold); Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }; FilledTonalIconButton({ set(value - 1) }, enabled = value > 1) { Icon(Icons.Rounded.Remove, "Уменьшить") }; Text(value.toString(), Modifier.width(44.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center, style = MaterialTheme.typography.titleMedium); FilledTonalIconButton({ set(value + 1) }) { Icon(Icons.Rounded.Add, "Увеличить") } } } }

@Composable private fun LabeledSlider(label: String, value: Int, range: IntRange, set: (Int) -> Unit, summary: String) {
    Column {
        Text(label, fontWeight = FontWeight.SemiBold)
        Text(summary, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
        Slider(value.toFloat(), { set(it.toInt()) }, valueRange = range.first.toFloat()..range.last.toFloat(), steps = (range.last - range.first - 1).coerceAtLeast(0))
    }
}

@Composable private fun WeekdayPicker(mask: Int, toggle: (Int) -> Unit) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { listOf("П","В","С","Ч","П","С","В").forEachIndexed { i, text -> FilterChip(mask and (1 shl i) != 0, { toggle(i) }, { Text(text) }, shape = CircleShape) } } }

private fun scheduleText(state: GoalEditorState) = when (state.recurrenceType) { RecurrenceType.DAILY -> if (state.timesPerDay == 1) "Каждый день" else "${timesWord(state.timesPerDay)} в день"; RecurrenceType.INTERVAL_DAYS -> if (state.repeatInterval == 1) "Каждый день" else "Раз в ${state.repeatInterval} ${dayWord(state.repeatInterval)}"; RecurrenceType.INTERVAL_WEEKS -> if (state.repeatInterval == 1) "Каждую неделю" else "Раз в ${state.repeatInterval} недели"; RecurrenceType.MONTHLY -> if (state.repeatInterval == 1) "${state.monthDay} числа каждого месяца" else "${state.monthDay} числа раз в ${state.repeatInterval} месяца" }
private fun pickTime(context: android.content.Context, current: Int, result: (Int) -> Unit) { TimePickerDialog(context, { _, h, m -> result(h * 60 + m) }, current / 60, current % 60, true).show() }
private fun requestExactAlarmAccess(context: android.content.Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val alarm = context.getSystemService(android.app.AlarmManager::class.java)
        if (!alarm.canScheduleExactAlarms()) context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")))
    }
}
