package com.ritmo.app.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ritmo.app.R
import com.ritmo.app.ui.components.GoalCard
import com.ritmo.app.ui.components.ProgressRing
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(onAddGoal: () -> Unit, onCalendar: () -> Unit, onStatistics: () -> Unit, onSettings: () -> Unit, onGoal: (Long) -> Unit, viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    Scaffold(
        floatingActionButton = { ExtendedFloatingActionButton(onClick = onAddGoal, icon = { Icon(Icons.Rounded.Add, null) }, text = { Text(stringResource(R.string.add_goal)) }) },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(true, {}, icon = { Icon(Icons.Rounded.Home, null) }, label = { Text("Сегодня") })
                NavigationBarItem(false, onCalendar, icon = { Icon(Icons.Rounded.CalendarMonth, null) }, label = { Text("Календарь") })
                NavigationBarItem(false, onStatistics, icon = { Icon(Icons.Rounded.BarChart, null) }, label = { Text("Аналитика") })
                NavigationBarItem(false, onSettings, icon = { Icon(Icons.Rounded.Settings, null) }, label = { Text(stringResource(R.string.settings)) })
            }
        },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(greeting(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(stringResource(R.string.today), style = MaterialTheme.typography.headlineMedium)
                        Text(LocalDate.now().format(DateTimeFormatter.ofPattern("d MMMM, EEEE", Locale("ru"))), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    AssistChip(onClick = onStatistics, label = { Text("30 дней") }, leadingIcon = { Icon(Icons.Rounded.BarChart, null, Modifier.size(18.dp)) })
                }
            }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), shape = MaterialTheme.shapes.extraLarge) {
                    Row(Modifier.fillMaxWidth().padding(22.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.daily_progress), style = MaterialTheme.typography.titleLarge)
                            Spacer(Modifier.height(6.dp))
                            Text(stringResource(R.string.done_of_total, state.done, state.goals.size), color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = .72f))
                        }
                        ProgressRing(state.progress, Modifier.size(96.dp))
                    }
                }
            }
            if (state.goals.isEmpty()) item {
                Box(Modifier.fillParentMaxHeight(.42f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.empty_today), style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(6.dp))
                        Text(stringResource(R.string.empty_today_hint), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            items(state.goals, key = { it.goal.id }) { goal -> GoalCard(goal, state.currentMinute, { viewModel.increment(goal) }, { viewModel.decrement(goal) }, { onGoal(goal.goal.id) }) }
            item { Spacer(Modifier.height(76.dp)) }
        }
    }
}

@Composable private fun greeting(): String = when (LocalTime.now().hour) {
    in 5..11 -> stringResource(R.string.good_morning)
    in 12..17 -> stringResource(R.string.good_afternoon)
    else -> stringResource(R.string.good_evening)
}
