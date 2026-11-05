package com.ritmo.app

import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ritmo.app.ui.navigation.RitmoApp
import com.ritmo.app.ui.theme.RitmoTheme
import dagger.hilt.android.AndroidEntryPoint
import com.ritmo.app.settings.SettingsStore
import com.ritmo.app.settings.applyAppLanguage
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var settingsStore: SettingsStore
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyAppLanguage(this, runBlocking { settingsStore.current().language })
        enableEdgeToEdge()
        setContent { val settings by settingsStore.settings.collectAsState(initial = runBlocking { settingsStore.current() }); RitmoTheme(settings.themeMode, settings.accent, settings.customColor) { RitmoApp(openSettings = intent?.data?.scheme == "ritmo") } }
    }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); recreate() }
}
