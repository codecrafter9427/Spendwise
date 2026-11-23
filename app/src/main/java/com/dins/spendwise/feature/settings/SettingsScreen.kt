package com.dins.spendwise.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dins.spendwise.domain.model.ThemePreference

@Composable
fun SettingsRoute(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsScreen(
        state = state,
        onThemeSelected = viewModel::setTheme,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onSignIn = viewModel::signIn,
        onSignUp = viewModel::signUp,
        onSignOut = viewModel::signOut,
        onImport = viewModel::importLocalData,
        onSyncNow = viewModel::syncNow,
        onClearMessage = viewModel::clearMessage
    )
}

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onThemeSelected: (ThemePreference) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSignIn: () -> Unit,
    onSignUp: () -> Unit,
    onSignOut: () -> Unit,
    onImport: () -> Unit,
    onSyncNow: () -> Unit,
    onClearMessage: () -> Unit
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
            text = "Account",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (!state.authConfigured) {
            Text(
                text = "Add spendwise.supabase.url and spendwise.supabase.anonKey to local.properties, then sync Gradle.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        } else if (state.session == null) {
            OutlinedTextField(
                value = state.emailInput,
                onValueChange = onEmailChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Email") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = state.passwordInput,
                onValueChange = onPasswordChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row {
                Button(
                    onClick = onSignIn,
                    enabled = !state.authBusy,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (state.authBusy) "Please wait…" else "Sign in")
                }
                Spacer(modifier = Modifier.padding(6.dp))
                OutlinedButton(
                    onClick = onSignUp,
                    enabled = !state.authBusy,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Sign up")
                }
            }
        } else {
            Text(
                text = "Signed in as ${state.session.email ?: state.session.userId ?: "user"}",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Local records stay on device. Upload once to link them to your cloud account.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Pending sync: ${state.pendingOutbox} · Conflicts: ${state.conflictOutbox}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onImport,
                enabled = !state.importBusy && !state.syncBusy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (state.importBusy) "Uploading…" else "Upload local data to cloud")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onSyncNow,
                enabled = !state.syncBusy && !state.importBusy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (state.syncBusy) "Syncing…" else "Sync now")
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onSignOut,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Sign out")
            }
        }

        state.message?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable(onClick = onClearMessage)
            )
        }
        state.errorMessage?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.clickable(onClick = onClearMessage)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
        HorizontalDivider()
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
            text = "Amounts are stored as integer paise. Cloud sync uses the same SpendWise API as web.",
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
                    "SpendWise tracks income, expenses, budgets, and category spending. " +
                        "It works offline locally and can optionally sign in to sync with the SpendWise API."
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
                    "Local records remain on this device. When you sign in and upload, selected data is " +
                        "sent to your SpendWise backend account using a Supabase access token. " +
                        "The app never embeds a service-role key."
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
