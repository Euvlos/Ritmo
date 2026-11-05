package com.ritmo.app.ui.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ritmo.app.domain.model.Goal
import com.ritmo.app.domain.model.GoalType
import com.ritmo.app.domain.model.RecurrenceType
import com.ritmo.app.domain.model.ReminderMode
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.ritmo.app.domain.repository.GoalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GoalEditorState(
    val title: String = "",
    val type: GoalType = GoalType.SIMPLE,
    val targetValue: Int = 1,
    val unit: String = "раз",
    val saving: Boolean = false,
    val titleError: Boolean = false,
    val recurrenceType: RecurrenceType = RecurrenceType.INTERVAL_DAYS,
    val repeatInterval: Int = 1,
    val timesPerDay: Int = 1,
    val reminderTimeMinutes: Int? = null,
    val weekdaysMask: Int = 0b0011111,
    val monthDay: Int = 1,
    val bindToTimes: Boolean = false,
    val executionTimes: List<Int> = emptyList(),
    val goalId: Long = 0,
    val loading: Boolean = false,
    val reminderMode: ReminderMode = ReminderMode.NOTIFICATION,
    val alarmSoundUri: String? = null,
    val alarmSoundName: String? = null,
    val customSounds: List<AlarmSound> = emptyList(),
)
data class AlarmSound(val uri: String, val name: String)

@HiltViewModel
class GoalEditorViewModel @Inject constructor(private val repository: GoalRepository, @ApplicationContext private val context: Context) : ViewModel() {
    private var loadedGoal: Goal? = null
    private val _state = MutableStateFlow(GoalEditorState())
    val state = _state.asStateFlow()
    init { refreshSounds() }
    fun title(value: String) = _state.update { it.copy(title = value, titleError = false) }
    fun type(value: GoalType) = _state.update { it.copy(type = value, targetValue = if (value == GoalType.SIMPLE) 1 else maxOf(2, it.targetValue)) }
    fun target(value: Int) = _state.update { it.copy(targetValue = value.coerceIn(1, 9999)) }
    fun unit(value: String) = _state.update { it.copy(unit = value) }
    fun recurrence(value: RecurrenceType) = _state.update { it.copy(recurrenceType = value) }
    fun interval(value: Int) = _state.update { it.copy(repeatInterval = value.coerceIn(1, 365)) }
    fun times(value: Int) = _state.update {
        val count = value.coerceIn(1, 12)
        it.copy(timesPerDay = count, executionTimes = if (it.bindToTimes) resizeTimes(it.executionTimes, count) else emptyList())
    }
    fun reminder(value: Int?) = _state.update { it.copy(reminderTimeMinutes = value) }
    fun reminderMode(value: ReminderMode) = _state.update { it.copy(reminderMode = value) }
    fun alarmSound(uri: String?, name: String?) = _state.update { it.copy(alarmSoundUri = uri, alarmSoundName = name) }
    fun load(goalId: Long) {
        if (goalId == 0L || state.value.goalId == goalId || state.value.loading) return
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            repository.get(goalId)?.let { goal ->
                loadedGoal = goal
                _state.value = GoalEditorState(title = goal.title, type = goal.type, targetValue = goal.targetValue, unit = goal.unit, recurrenceType = goal.recurrenceType, repeatInterval = goal.repeatInterval, timesPerDay = goal.timesPerDay, reminderTimeMinutes = goal.reminderTimeMinutes, weekdaysMask = goal.weekdaysMask, monthDay = goal.monthDay, bindToTimes = goal.executionTimes.isNotEmpty(), executionTimes = goal.executionTimes, goalId = goal.id, reminderMode = goal.reminderMode, alarmSoundUri = goal.alarmSoundUri, alarmSoundName = goal.alarmSoundName, customSounds = state.value.customSounds)
            } ?: _state.update { it.copy(loading = false) }
        }
    }
    fun importSound(uri: Uri) = viewModelScope.launch {
        val name = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null } ?: "Своя мелодия"
        val safeName = name.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val target = File(context.filesDir, "alarm_sounds/${System.currentTimeMillis()}_$safeName")
        withContext(Dispatchers.IO) { target.parentFile?.mkdirs(); context.contentResolver.openInputStream(uri)?.use { input -> target.outputStream().use(input::copyTo) } }
        alarmSound(Uri.fromFile(target).toString(), name)
        refreshSounds()
    }
    private fun refreshSounds() {
        val sounds = File(context.filesDir, "alarm_sounds").listFiles().orEmpty().sortedByDescending { it.lastModified() }.map { file -> AlarmSound(Uri.fromFile(file).toString(), file.name.substringAfter('_').replace('_', ' ')) }
        _state.update { it.copy(customSounds = sounds) }
    }
    fun toggleWeekday(index: Int) = _state.update {
        val toggled = it.weekdaysMask xor (1 shl index)
        it.copy(weekdaysMask = if (toggled == 0) it.weekdaysMask else toggled)
    }
    fun monthDay(value: Int) = _state.update { it.copy(monthDay = value.coerceIn(1, 31)) }
    fun bindToTimes(enabled: Boolean) = _state.update {
        it.copy(bindToTimes = enabled, executionTimes = if (enabled) resizeTimes(it.executionTimes, it.timesPerDay) else emptyList(), reminderTimeMinutes = if (enabled) null else it.reminderTimeMinutes)
    }
    fun executionTime(index: Int, minutes: Int) = _state.update { state -> state.copy(executionTimes = state.executionTimes.toMutableList().also { it[index] = minutes }.sorted()) }
    fun specificDays(enabled: Boolean) = _state.update { it.copy(recurrenceType = if (enabled) RecurrenceType.INTERVAL_WEEKS else RecurrenceType.INTERVAL_DAYS, repeatInterval = 1) }
    fun specificMode(value: RecurrenceType) = _state.update { it.copy(recurrenceType = value) }
    fun save(onSaved: () -> Unit) {
        val value = state.value
        if (value.title.isBlank()) { _state.update { it.copy(titleError = true) }; return }
        viewModelScope.launch {
            _state.update { it.copy(saving = true) }
            val goal = (loadedGoal ?: Goal(title = value.title)).copy(id = value.goalId, title = value.title, type = value.type, targetValue = value.targetValue, unit = value.unit.ifBlank { "раз" }, recurrenceType = value.recurrenceType, repeatInterval = value.repeatInterval, timesPerDay = value.timesPerDay, reminderTimeMinutes = value.reminderTimeMinutes, weekdaysMask = value.weekdaysMask, monthDay = value.monthDay, executionTimes = value.executionTimes, reminderMode = value.reminderMode, alarmSoundUri = value.alarmSoundUri, alarmSoundName = value.alarmSoundName)
            if (value.goalId == 0L) repository.create(goal) else repository.update(goal)
            onSaved()
        }
    }

    private fun resizeTimes(existing: List<Int>, count: Int): List<Int> {
        if (existing.size >= count) return existing.take(count)
        val defaults = if (count == 1) listOf(9 * 60) else (0 until count).map { 8 * 60 + it * (12 * 60 / (count - 1)) }
        return (existing + defaults.drop(existing.size)).take(count).sorted()
    }
}
