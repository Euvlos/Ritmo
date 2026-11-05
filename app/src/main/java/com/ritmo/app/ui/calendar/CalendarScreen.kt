package com.ritmo.app.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.ritmo.app.domain.model.scheduleSummary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(onBack: () -> Unit, onGoal: (Long) -> Unit, viewModel: CalendarViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    Scaffold(topBar = { TopAppBar(title = { Text("Календарь") }, navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Назад") } }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(viewModel::previous) { Icon(Icons.Rounded.ChevronLeft, "Предыдущий месяц") }
                    Text(state.month.format(DateTimeFormatter.ofPattern("LLLL yyyy", Locale.forLanguageTag("ru"))).replaceFirstChar { it.uppercase() }, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    IconButton(viewModel::next) { Icon(Icons.Rounded.ChevronRight, "Следующий месяц") }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth()) { listOf("Пн","Вт","Ср","Чт","Пт","Сб","Вс").forEach { Text(it, Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                    state.days.chunked(7).forEach { week -> Row(Modifier.fillMaxWidth()) { week.forEach { day -> DayCell(day, day.date == state.selected, viewModel::select, Modifier.weight(1f)) } } }
                }
            }
            item { Text(state.selected.format(DateTimeFormatter.ofPattern("d MMMM", Locale.forLanguageTag("ru"))), style = MaterialTheme.typography.titleLarge) }
            if (state.selectedGoals.isEmpty()) item { Text("На этот день целей нет", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(state.selectedGoals, key = { it.id }) { goal -> ListItem(headlineContent = { Text(goal.title) }, supportingContent = { Text(goal.scheduleSummary()) }, modifier = Modifier.clip(MaterialTheme.shapes.large).clickable { onGoal(goal.id) }, colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) }
        }
    }
}

@Composable private fun DayCell(day: CalendarDay, selected: Boolean, select: (LocalDate) -> Unit, modifier: Modifier = Modifier) {
    val intensity = if (day.scheduled == 0) 0f else .12f + day.progress * .88f
    val color = if (day.scheduled == 0) Color.Transparent else MaterialTheme.colorScheme.primary.copy(alpha = intensity)
    val outline = if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier
    Box(modifier.aspectRatio(1f).padding(3.dp).then(outline).clip(CircleShape).background(color).clickable { select(day.date) }, contentAlignment = Alignment.Center) {
        Text(day.date.dayOfMonth.toString(), color = when { day.progress >= .55f -> MaterialTheme.colorScheme.onPrimary; !day.inMonth -> MaterialTheme.colorScheme.onSurface.copy(.35f); else -> MaterialTheme.colorScheme.onSurface }, fontWeight = if (day.date == LocalDate.now() || selected) FontWeight.Bold else FontWeight.Normal)
    }
}
