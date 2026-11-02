package com.dins.spendwise.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dins.spendwise.BuildConfig
import com.dins.spendwise.domain.model.ThemePreference
import com.dins.spendwise.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val themePreference: ThemePreference = ThemePreference.SYSTEM,
    val appVersion: String = BuildConfig.VERSION_NAME
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = settingsRepository.observeThemePreference()
        .map { SettingsUiState(themePreference = it, appVersion = BuildConfig.VERSION_NAME) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SettingsUiState()
        )

    fun setTheme(preference: ThemePreference) {
        viewModelScope.launch {
            settingsRepository.setThemePreference(preference)
        }
    }
}
