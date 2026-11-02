package com.dins.spendwise.feature.budget

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dins.spendwise.core.utils.DateUtils
import java.time.YearMonth

@Composable
fun BudgetDialogHost(
    visible: Boolean,
    yearMonth: YearMonth,
    existingAmountMinor: Long?,
    onDismiss: () -> Unit,
    viewModel: BudgetViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(visible, yearMonth, existingAmountMinor) {
        if (visible) {
            viewModel.prepare(yearMonth, existingAmountMinor)
        }
    }

    LaunchedEffect(state.saved) {
        if (state.saved) {
            viewModel.consumeSaved()
            onDismiss()
        }
    }

    if (visible) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Monthly budget") },
            text = {
                Column {
                    Text(
                        text = DateUtils.formatMonthYear(yearMonth),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = state.amountText,
                        onValueChange = viewModel::onAmountChange,
                        label = { Text("Budget amount (₹)") },
                        isError = state.amountError != null,
                        supportingText = {
                            Text(state.amountError ?: state.errorMessage.orEmpty())
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = viewModel::save,
                    enabled = !state.isSaving
                ) {
                    Text(if (state.isSaving) "Saving…" else "Save")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }
}
