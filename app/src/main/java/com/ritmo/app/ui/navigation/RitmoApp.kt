package com.ritmo.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ritmo.app.ui.goals.GoalEditorScreen
import com.ritmo.app.ui.home.HomeScreen
import com.ritmo.app.ui.calendar.CalendarScreen
import com.ritmo.app.ui.statistics.StatisticsScreen
import com.ritmo.app.ui.goaldetails.GoalDetailsScreen
import com.ritmo.app.ui.settings.SettingsScreen
import androidx.navigation.NavType
import androidx.navigation.navArgument

private const val HOME = "home"
private const val ADD_GOAL = "goal/new"
private const val CALENDAR = "calendar"
private const val STATISTICS = "statistics"
private const val GOAL_DETAILS = "goal/{goalId}"
private const val SETTINGS = "settings"

@Composable
fun RitmoApp() {
    val nav = rememberNavController()
    NavHost(nav, startDestination = HOME) {
        composable(HOME) { HomeScreen(onAddGoal = { nav.navigate(ADD_GOAL) }, onCalendar = { nav.navigate(CALENDAR) }, onStatistics = { nav.navigate(STATISTICS) }, onSettings = { nav.navigate(SETTINGS) }, onGoal = { nav.navigate("goal/$it") }) }
        composable(ADD_GOAL) { GoalEditorScreen(onBack = { nav.popBackStack() }) }
        composable(CALENDAR) { CalendarScreen(onBack = { nav.popBackStack() }, onGoal = { nav.navigate("goal/$it") }) }
        composable(STATISTICS) { StatisticsScreen(onBack = { nav.popBackStack() }, onGoal = { nav.navigate("goal/$it") }) }
        composable(GOAL_DETAILS, arguments = listOf(navArgument("goalId") { type = NavType.LongType })) { GoalDetailsScreen(onBack = { nav.popBackStack() }) }
        composable(SETTINGS) { SettingsScreen(onBack = { nav.popBackStack() }) }
    }
}
