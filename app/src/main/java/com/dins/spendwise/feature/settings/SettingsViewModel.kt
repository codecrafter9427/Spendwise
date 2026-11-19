package com.dins.spendwise.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dins.spendwise.BuildConfig
import com.dins.spendwise.data.remote.AccountImportService
import com.dins.spendwise.data.remote.AuthRepository
import com.dins.spendwise.data.remote.AuthSession
import com.dins.spendwise.data.remote.ImportResult
import com.dins.spendwise.domain.model.ThemePreference
import com.dins.spendwise.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val themePreference: ThemePreference = ThemePreference.SYSTEM,
    val appVersion: String = BuildConfig.VERSION_NAME,
    val authConfigured: Boolean = false,
    val session: AuthSession? = null,
    val emailInput: String = "",
    val passwordInput: String = "",
    val authBusy: Boolean = false,
    val importBusy: Boolean = false,
    val message: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val authRepository: AuthRepository,
    private val importService: AccountImportService
) : ViewModel() {

    private val formState = MutableStateFlow(SettingsUiState(authConfigured = authRepository.isConfigured()))

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.observeThemePreference(),
        authRepository.sessionFlow,
        formState
    ) { theme, session, form ->
        form.copy(
            themePreference = theme,
            appVersion = BuildConfig.VERSION_NAME,
            authConfigured = authRepository.isConfigured(),
            session = session
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState(authConfigured = authRepository.isConfigured())
    )

    fun setTheme(preference: ThemePreference) {
        viewModelScope.launch {
            settingsRepository.setThemePreference(preference)
        }
    }

    fun onEmailChange(value: String) {
        formState.update { it.copy(emailInput = value, errorMessage = null, message = null) }
    }

    fun onPasswordChange(value: String) {
        formState.update { it.copy(passwordInput = value, errorMessage = null, message = null) }
    }

    fun signIn() {
        val email = formState.value.emailInput
        val password = formState.value.passwordInput
        viewModelScope.launch {
            formState.update { it.copy(authBusy = true, errorMessage = null, message = null) }
            val result = authRepository.signIn(email, password)
            formState.update { state ->
                result.fold(
                    onSuccess = {
                        state.copy(
                            authBusy = false,
                            passwordInput = "",
                            message = "Signed in. You can upload local data next."
                        )
                    },
                    onFailure = { error ->
                        state.copy(
                            authBusy = false,
                            errorMessage = error.message ?: "Sign in failed"
                        )
                    }
                )
            }
        }
    }

    fun signUp() {
        val email = formState.value.emailInput
        val password = formState.value.passwordInput
        viewModelScope.launch {
            formState.update { it.copy(authBusy = true, errorMessage = null, message = null) }
            val result = authRepository.signUp(email, password)
            formState.update { state ->
                result.fold(
                    onSuccess = {
                        state.copy(
                            authBusy = false,
                            passwordInput = "",
                            message = "Account created and signed in."
                        )
                    },
                    onFailure = { error ->
                        state.copy(
                            authBusy = false,
                            errorMessage = error.message ?: "Sign up failed"
                        )
                    }
                )
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            formState.update {
                it.copy(message = "Signed out. Local data remains on this device.", errorMessage = null)
            }
        }
    }

    fun importLocalData() {
        viewModelScope.launch {
            formState.update { it.copy(importBusy = true, errorMessage = null, message = null) }
            val result = importService.importIfNeeded(force = true)
            formState.update { state ->
                result.fold(
                    onSuccess = { summary: ImportResult ->
                        state.copy(
                            importBusy = false,
                            message = "Import finished. Tx imported=${summary.importedTransactions}, " +
                                "skipped=${summary.skippedTransactions}, failed=${summary.failedTransactions}. " +
                                "Budgets imported=${summary.importedBudgets}, failed=${summary.failedBudgets}."
                        )
                    },
                    onFailure = { error ->
                        state.copy(
                            importBusy = false,
                            errorMessage = error.message ?: "Import failed"
                        )
                    }
                )
            }
        }
    }

    fun clearMessage() {
        formState.update { it.copy(message = null, errorMessage = null) }
    }
}
