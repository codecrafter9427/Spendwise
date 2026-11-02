package com.dins.spendwise.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dins.spendwise.domain.model.ThemePreference
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment

@Composable
fun SettingsRoute(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsScreen(
        state = state,
        onThemeSelected = viewModel::setTheme
    )
}

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onThemeSelected: (ThemePreference) -> Unit
) {
    var showAbout by remember { mutableStateOf(false) }
    var showPrivacy by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Appearance",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        ThemeOption(
            label = "System default",
            selected = state.themePreference == ThemePreference.SYSTEM,
            onClick = { onThemeSelected(ThemePreference.SYSTEM) }
        )
        ThemeOption(
            label = "Light",
            selected = state.themePreference == ThemePreference.LIGHT,
            onClick = { onThemeSelected(ThemePreference.LIGHT) }
        )
        ThemeOption(
            label = "Dark",
            selected = state.themePreference == ThemePreference.DARK,
            onClick = { onThemeSelected(ThemePreference.DARK) }
        )

        Spacer(modifier = Modifier.height(20.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Currency",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Indian Rupee (₹)",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 4.dp)
        )
        Text(
            text = "MVP uses INR only. Amounts are stored locally in paise.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
        )

        Spacer(modifier = Modifier.height(20.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(8.dp))

        SettingsClickRow(title = "About SpendWise") { showAbout = true }
        SettingsClickRow(title = "Privacy information") { showPrivacy = true }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Version ${state.appVersion}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
        )
    }

    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            title = { Text("About SpendWise") },
            text = {
                Text(
                    "SpendWise is an offline-first personal finance app for tracking income, " +
                        "expenses, monthly budgets, and spending by category. " +
                        "All data stays on this device."
                )
            },
            confirmButton = {
                TextButton(onClick = { showAbout = false }) { Text("OK") }
            }
        )
    }

    if (showPrivacy) {
        AlertDialog(
            onDismissRequest = { showPrivacy = false },
            title = { Text("Privacy information") },
            text = {
                Text(
                    "SpendWise stores your financial records only on this device using local storage. " +
                        "The MVP does not create accounts, sync to the cloud, send analytics, show ads, " +
                        "or request network access. Uninstalling the app removes locally stored data " +
                        "unless you have a device-level backup enabled by Android."
                )
            },
            confirmButton = {
                TextButton(onClick = { showPrivacy = false }) { Text("OK") }
            }
        )
    }
}

@Composable
private fun ThemeOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun SettingsClickRow(title: String, onClick: () -> Unit) {
    Text(
        text = title,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp)
    )
}
