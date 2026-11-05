package com.ritmo.app.ui.settings

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ritmo.app.R
import com.ritmo.app.settings.*
import com.ritmo.app.updates.*
import com.ritmo.app.BuildConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState(); val context = LocalContext.current
    val updateState by viewModel.updates.state.collectAsState()
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.settings)) }, navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back)) } }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            item {
                SettingsCard(stringResource(R.string.appearance), Icons.Rounded.Palette) {
                    Text(stringResource(R.string.theme), style = MaterialTheme.typography.titleMedium)
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        listOf(ThemeMode.SYSTEM to R.string.theme_system, ThemeMode.LIGHT to R.string.theme_light, ThemeMode.DARK to R.string.theme_dark).forEachIndexed { index, option ->
                            SegmentedButton(state.themeMode == option.first, { viewModel.theme(option.first) }, SegmentedButtonDefaults.itemShape(index, 3)) { Text(stringResource(option.second)) }
                        }
                    }
                    Text(stringResource(R.string.accent_color), style = MaterialTheme.typography.titleMedium)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        listOf(AccentTheme.OCEAN to Color(0xFF006C84), AccentTheme.VIOLET to Color(0xFF6750A4), AccentTheme.FOREST to Color(0xFF386A20), AccentTheme.SUNSET to Color(0xFF9C4235)).forEach { (accent, color) ->
                            FilledIconButton({ viewModel.accent(accent) }, colors = IconButtonDefaults.filledIconButtonColors(containerColor = color)) { if (state.accent == accent) Icon(Icons.Rounded.Check, null, tint = Color.White) else Spacer(Modifier.size(24.dp)) }
                        }
                    }
                    var red by remember(state.customColor) { mutableFloatStateOf(((state.customColor shr 16) and 0xFF).toFloat()) }
                    var green by remember(state.customColor) { mutableFloatStateOf(((state.customColor shr 8) and 0xFF).toFloat()) }
                    var blue by remember(state.customColor) { mutableFloatStateOf((state.customColor and 0xFF).toFloat()) }
                    Text(stringResource(R.string.custom_color), style = MaterialTheme.typography.titleMedium)
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Surface(Modifier.size(48.dp), shape = MaterialTheme.shapes.large, color = Color(red.toInt(), green.toInt(), blue.toInt())) {}
                        Text("#%02X%02X%02X".format(red.toInt(), green.toInt(), blue.toInt()), style = MaterialTheme.typography.titleMedium)
                    }
                    ColorSlider("R", red) { red = it; viewModel.customColor(rgb(red, green, blue)) }
                    ColorSlider("G", green) { green = it; viewModel.customColor(rgb(red, green, blue)) }
                    ColorSlider("B", blue) { blue = it; viewModel.customColor(rgb(red, green, blue)) }
                }
            }
            item {
                SettingsCard("Обновления", Icons.Rounded.SystemUpdate) {
                    Text("Установлена версия ${BuildConfig.VERSION_NAME}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    when (val update = updateState) {
                        UpdateState.Idle -> Button(viewModel::checkUpdates, Modifier.fillMaxWidth()) { Text("Проверить обновления") }
                        UpdateState.Checking -> LinearProgressIndicator(Modifier.fillMaxWidth())
                        UpdateState.UpToDate -> { Text("Установлена последняя версия"); OutlinedButton(viewModel::checkUpdates, Modifier.fillMaxWidth()) { Text("Проверить снова") } }
                        is UpdateState.Available -> { Text("Доступна версия ${update.info.version}", style = MaterialTheme.typography.titleMedium); Button({ viewModel.install(update.info) }, Modifier.fillMaxWidth()) { Icon(Icons.Rounded.Download, null); Spacer(Modifier.width(8.dp)); Text("Скачать и установить") } }
                        UpdateState.Downloading -> { Text("Скачиваем APK…"); LinearProgressIndicator(Modifier.fillMaxWidth()) }
                        is UpdateState.Error -> { Text(update.message, color = MaterialTheme.colorScheme.error); OutlinedButton(viewModel::checkUpdates, Modifier.fillMaxWidth()) { Text("Повторить") } }
                    }
                    Text("Проверка и загрузка выполняются напрямую через GitHub Releases.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item {
                SettingsCard(stringResource(R.string.language), Icons.Rounded.Language) {
                    LanguageRow("Русский", "RU", state.language == "ru") { viewModel.language("ru") { applyAppLanguage(context, "ru"); (context as? Activity)?.recreate() } }
                    LanguageRow("English", "EN", state.language == "en") { viewModel.language("en") { applyAppLanguage(context, "en"); (context as? Activity)?.recreate() } }
                }
            }
            item { Text(stringResource(R.string.widget_hint), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable private fun SettingsCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, content: @Composable ColumnScope.() -> Unit) = Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer), shape = MaterialTheme.shapes.extraLarge) { Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary); Text(title, style = MaterialTheme.typography.titleLarge) }; content() } }
@Composable private fun LanguageRow(title: String, code: String, selected: Boolean, click: () -> Unit) = ListItem(headlineContent = { Text(title) }, leadingContent = { Text(code) }, trailingContent = { RadioButton(selected, click) }, modifier = Modifier.fillMaxWidth(), colors = ListItemDefaults.colors(containerColor = Color.Transparent))
@Composable private fun ColorSlider(label: String, value: Float, change: (Float) -> Unit) = Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) { Text(label, Modifier.width(24.dp)); Slider(value, change, Modifier.weight(1f), valueRange = 0f..255f) }
private fun rgb(red: Float, green: Float, blue: Float): Long = 0xFF000000 or (red.toLong() shl 16) or (green.toLong() shl 8) or blue.toLong()
