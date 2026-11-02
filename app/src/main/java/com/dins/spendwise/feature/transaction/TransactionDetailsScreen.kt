package com.dins.spendwise.feature.transaction

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dins.spendwise.core.utils.DateUtils
import com.dins.spendwise.core.utils.Money
import com.dins.spendwise.domain.model.TransactionType
import com.dins.spendwise.ui.theme.LocalSpendWiseColors

@Composable
fun TransactionDetailsRoute(
    onDismiss: () -> Unit,
    onEdit: (Long) -> Unit,
    viewModel: TransactionDetailsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.deleted) {
        if (state.deleted) {
            viewModel.consumeDeleted()
            onDismiss()
        }
    }

    TransactionDetailsSheet(
        state = state,
        onDismiss = onDismiss,
        onEdit = {
            state.item?.transaction?.id?.let(onEdit)
        },
        onRequestDelete = viewModel::requestDelete,
        onDismissDelete = viewModel::dismissDelete,
        onConfirmDelete = viewModel::confirmDelete
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailsSheet(
    state: TransactionDetailsUiState,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onRequestDelete: () -> Unit,
    onDismissDelete: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val colors = LocalSpendWiseColors.current
    val item = state.item

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            Text(
                text = "Transaction details",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(16.dp))
            if (item == null) {
                Text("Transaction not found.")
            } else {
                val isIncome = item.transaction.type == TransactionType.INCOME
                val amountLabel = if (isIncome) {
                    "+${Money.formatInr(item.transaction.amountMinor)}"
                } else {
                    "−${Money.formatInr(item.transaction.amountMinor)}"
                }
                Text(
                    text = amountLabel,
                    style = MaterialTheme.typography.headlineMedium,
                    color = if (isIncome) colors.income else colors.expense,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                DetailRow("Type", if (isIncome) "Income" else "Expense")
                DetailRow("Category", item.category.name)
                DetailRow("Date", DateUtils.formatDisplayDate(item.transaction.transactionDate))
                DetailRow(
                    "Description",
                    item.transaction.description?.ifBlank { "—" } ?: "—"
                )
                if (state.errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = state.errorMessage,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onEdit,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Edit")
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onRequestDelete,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Delete")
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (state.showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = onDismissDelete,
            title = { Text("Delete transaction?") },
            text = { Text("This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = onConfirmDelete, enabled = !state.isDeleting) {
                    Text(if (state.isDeleting) "Deleting…" else "Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissDelete) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium
        )
    }
}
