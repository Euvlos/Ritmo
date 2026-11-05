package com.ritmo.app.domain.model

import java.time.LocalDate

enum class GoalType { SIMPLE, COUNTER }
enum class GoalStatus { ACTIVE, PAUSED, ARCHIVED }
enum class RecurrenceType { DAILY, INTERVAL_DAYS, INTERVAL_WEEKS, MONTHLY }
enum class ReminderMode { NOTIFICATION, VIBRATION, ALARM }

data class Goal(
    val id: Long = 0,
    val title: String,
    val type: GoalType = GoalType.SIMPLE,
    val targetValue: Int = 1,
    val unit: String = "раз",
    val color: Long = 0xFF6750A4,
    val startDate: LocalDate = LocalDate.now(),
    val status: GoalStatus = GoalStatus.ACTIVE,
    val recurrenceType: RecurrenceType = RecurrenceType.DAILY,
    val repeatInterval: Int = 1,
    val timesPerDay: Int = 1,
    val reminderTimeMinutes: Int? = null,
    val weekdaysMask: Int = 0b1111111,
    val monthDay: Int = 1,
    val executionTimes: List<Int> = emptyList(),
    val archivedDate: LocalDate? = null,
    val reminderMode: ReminderMode = ReminderMode.NOTIFICATION,
    val alarmSoundUri: String? = null,
    val alarmSoundName: String? = null,
    val reminders: List<GoalReminder> = emptyList(),
    val earlyCompletionMinutes: Int = 0,
)

data class GoalReminder(
    val timeMinutes: Int,
    val mode: ReminderMode = ReminderMode.NOTIFICATION,
    val soundUri: String? = null,
    val soundName: String? = null,
)

fun Goal.isScheduledOn(date: LocalDate): Boolean = when {
    date < startDate -> false
    archivedDate != null && date > archivedDate -> false
    recurrenceType == RecurrenceType.DAILY -> true
    recurrenceType == RecurrenceType.INTERVAL_DAYS -> (date.toEpochDay() - startDate.toEpochDay()) % repeatInterval.coerceAtLeast(1) == 0L
    recurrenceType == RecurrenceType.INTERVAL_WEEKS -> {
        val startWeek = startDate.minusDays((startDate.dayOfWeek.value - 1).toLong())
        val dateWeek = date.minusDays((date.dayOfWeek.value - 1).toLong())
        val weeks = java.time.temporal.ChronoUnit.WEEKS.between(startWeek, dateWeek)
        weeks % repeatInterval.coerceAtLeast(1) == 0L && weekdaysMask and (1 shl (date.dayOfWeek.value - 1)) != 0
    }
    recurrenceType == RecurrenceType.MONTHLY -> {
        val months = java.time.temporal.ChronoUnit.MONTHS.between(java.time.YearMonth.from(startDate), java.time.YearMonth.from(date))
        val expectedDay = monthDay.coerceAtMost(date.lengthOfMonth())
        months % repeatInterval.coerceAtLeast(1) == 0L && date.dayOfMonth == expectedDay
    }
    else -> false
}

fun Goal.scheduleSummary(): String = when (recurrenceType) {
    RecurrenceType.DAILY -> if (timesPerDay == 1) "Каждый день" else "${timesWord(timesPerDay)} в день"
    RecurrenceType.INTERVAL_DAYS -> if (repeatInterval == 1) "Каждый день" else "Раз в $repeatInterval ${dayWord(repeatInterval)}"
    RecurrenceType.INTERVAL_WEEKS -> (if (repeatInterval == 1) "Каждую неделю" else "Раз в $repeatInterval недели") + " · " + listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс").filterIndexed { i, _ -> weekdaysMask and (1 shl i) != 0 }.joinToString(", ")
    RecurrenceType.MONTHLY -> (if (repeatInterval == 1) "Каждый месяц" else "Раз в $repeatInterval месяца") + " · $monthDay число"
} + if (executionTimes.isEmpty()) "" else " · " + executionTimes.joinToString(", ") { "%02d:%02d".format(it / 60, it % 60) }

fun dayWord(value: Int): String = when {
    value % 100 in 11..14 -> "дней"
    value % 10 == 1 -> "день"
    value % 10 in 2..4 -> "дня"
    else -> "дней"
}

fun timesWord(value: Int): String = when {
    value % 100 in 11..14 -> "$value раз"
    value % 10 == 1 -> "$value раз"
    value % 10 in 2..4 -> "$value раза"
    else -> "$value раз"
}

val Goal.requiredProgress: Int get() = if (type == GoalType.SIMPLE) timesPerDay.coerceAtLeast(1) else targetValue.coerceAtLeast(1)

fun Goal.availableProgressAt(minuteOfDay: Int): Int {
    if (executionTimes.isEmpty()) return requiredProgress
    val availableSlots = executionTimes.count { it <= minuteOfDay + earlyCompletionMinutes }
    if (availableSlots == 0) return 0
    return if (type == GoalType.SIMPLE) availableSlots.coerceAtMost(requiredProgress)
    else kotlin.math.ceil(requiredProgress.toDouble() * availableSlots / executionTimes.size).toInt().coerceAtMost(requiredProgress)
}

fun Goal.nextExecutionAfter(minuteOfDay: Int): Int? = executionTimes.sorted().firstOrNull { it > minuteOfDay }

data class DailyGoal(
    val goal: Goal,
    val completed: Int,
) {
    val progress: Float get() = (completed.toFloat() / goal.requiredProgress).coerceIn(0f, 1f)
    val isComplete: Boolean get() = completed >= goal.requiredProgress
}
