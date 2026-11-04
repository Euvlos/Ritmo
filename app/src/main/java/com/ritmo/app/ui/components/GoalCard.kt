package com.ritmo.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import com.ritmo.app.domain.model.DailyGoal
import com.ritmo.app.domain.model.GoalType
import com.ritmo.app.domain.model.RecurrenceType
import com.ritmo.app.domain.model.requiredProgress
import com.ritmo.app.domain.model.scheduleSummary
import com.ritmo.app.domain.model.availableProgressAt
import com.ritmo.app.domain.model.nextExecutionAfter
import java.util.Locale

@Composable
fun GoalCard(goal: DailyGoal, currentMinute: Int, onIncrement: () -> Unit, onDecrement: () -> Unit, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val available = goal.goal.availableProgressAt(currentMinute)
    val canComplete = goal.completed < available && (!goal.isComplete || goal.goal.type == GoalType.COUNTER)
    Card(modifier.fillMaxWidth().clickable(onClick = onOpen), shape = MaterialTheme.shapes.extraLarge, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = MaterialTheme.shapes.large, color = Color(goal.goal.color).copy(alpha = .16f), modifier = Modifier.size(48.dp)) {
                Box(contentAlignment = Alignment.Center) { Text(goal.goal.title.take(1).uppercase(), color = Color(goal.goal.color), style = MaterialTheme.typography.titleLarge) }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(goal.goal.title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(5.dp))
                Text("${goal.completed} / ${goal.goal.requiredProgress} ${goal.goal.unit}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                if (!canComplete && !goal.isComplete) goal.goal.nextExecutionAfter(currentMinute)?.let { time ->
                    Text(String.format(Locale.ROOT, "Следующее · %02d:%02d", time / 60, time % 60), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, maxLines = 1)
                }
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator({ goal.progress }, Modifier.fillMaxWidth(), strokeCap = androidx.compose.ui.graphics.StrokeCap.Round)
            }
            Spacer(Modifier.width(12.dp))
            if (goal.goal.type == GoalType.COUNTER) {
                IconButton(onDecrement, enabled = goal.completed > 0) { Icon(Icons.Rounded.Remove, contentDescription = "Уменьшить") }
            }
            FilledIconButton(onIncrement, enabled = canComplete, modifier = Modifier.semantics { contentDescription = if (goal.isComplete) "Выполнено" else if (canComplete) "Выполнить" else "Ещё рано" }) {
                Icon(Icons.Rounded.Check, contentDescription = null)
            }
        }
    }
}
