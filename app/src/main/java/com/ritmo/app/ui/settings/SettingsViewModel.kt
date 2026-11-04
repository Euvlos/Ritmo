package com.ritmo.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ritmo.app.settings.*
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(private val store: SettingsStore) : ViewModel() {
    val state = store.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())
    fun theme(value: ThemeMode) = viewModelScope.launch { store.setTheme(value) }
    fun accent(value: AccentTheme) = viewModelScope.launch { store.setAccent(value) }
    fun customColor(value: Long) = viewModelScope.launch { store.setCustomColor(value) }
    fun language(value: String, applied: () -> Unit) = viewModelScope.launch { store.setLanguage(value); applied() }
}
