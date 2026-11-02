package com.dins.spendwise.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dins.spendwise.core.designsystem.EmptyStateMessage
import com.dins.spendwise.core.designsystem.MonthSelector
import com.dins.spendwise.core.designsystem.SummaryCard
import com.dins.spendwise.core.designsystem.TransactionListItem
import com.dins.spendwise.core.utils.Money
import com.dins.spendwise.domain.model.BudgetStatus
import com.dins.spendwise.ui.theme.LocalSpendWiseColors

@Composable
fun DashboardRoute(
    onViewAllTransactions: () -> Unit,
    onTransactionClick: (Long) -> Unit,
    onSetBudget: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    DashboardScreen(
        state = state,
        onPreviousMonth = viewModel::previousMonth,
        onNextMonth = viewModel::nextMonth,
        onViewAllTransactions = onViewAllTransactions,
        onTransactionClick = onTransactionClick,
        onSetBudget = onSetBudget
    )
}

@Composable
fun DashboardScreen(
    state: DashboardUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onViewAllTransactions: () -> Unit,
    onTransactionClick: (Long) -> Unit,
    onSetBudget: () -> Unit
) {
    val colors = LocalSpendWiseColors.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = "SpendWise",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Monthly overview",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
        )
        Spacer(modifier = Modifier.height(12.dp))
        MonthSelector(
            yearMonth = state.yearMonth,
            onPrevious = onPreviousMonth,
            onNext = onNextMonth
        )
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SummaryCard(
                title = "Income",
                amountMinor = state.incomeMinor,
                amountColor = colors.income,
                modifier = Modifier.weight(1f)
            )
            SummaryCard(
                title = "Expenses",
                amountMinor = state.expenseMinor,
                amountColor = colors.expense,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Net monthly balance",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    text = Money.formatInr(state.netMinor),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        state.netMinor > 0 -> colors.income
                        state.netMinor < 0 -> colors.expense
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
                Text(
                    text = "Income minus expenses for this month. Not your bank balance.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        BudgetSection(
            state = state,
            onSetBudget = onSetBudget
        )

        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Recent transactions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            TextButton(onClick = onViewAllTransactions) {
                Text("View all")
            }
        }

        if (!state.hasAnyTransactions) {
            EmptyStateMessage(
                title = "No transactions yet",
                subtitle = "Tap + to record your first income or expense for this month."
            )
        } else {
            state.recentTransactions.forEach { item ->
                TransactionListItem(
                    item = item,
                    onClick = { onTransactionClick(item.transaction.id) }
                )
            }
        }
        Spacer(modifier = Modifier.height(88.dp))
    }
}

@Composable
private fun BudgetSection(
    state: DashboardUiState,
    onSetBudget: () -> Unit
) {
    val colors = LocalSpendWiseColors.current
    val progress = state.budgetProgress
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Monthly budget",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (progress.status == BudgetStatus.UNSET) {
                Text(
                    text = "No budget set for this month.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onSetBudget) {
                    Text("Set budget")
                }
            } else {
                val remaining = progress.remainingMinor ?: 0L
                val utilization = ((progress.utilizationPercent ?: 0.0) / 100.0)
                    .toFloat()
                    .coerceAtLeast(0f)
                val statusLabel = when (progress.status) {
                    BudgetStatus.WITHIN -> "Within budget"
                    BudgetStatus.APPROACHING -> "Approaching budget"
                    BudgetStatus.NEAR_LIMIT -> "Near budget limit"
                    BudgetStatus.EXCEEDED -> "Budget exceeded"
                    BudgetStatus.UNSET -> ""
                }
                val statusColor = when (progress.status) {
                    BudgetStatus.WITHIN -> colors.income
                    BudgetStatus.APPROACHING -> colors.warning
                    BudgetStatus.NEAR_LIMIT, BudgetStatus.EXCEEDED -> colors.danger
                    BudgetStatus.UNSET -> MaterialTheme.colorScheme.onSurface
                }
                Text(
                    text = "Budget ${Money.formatInr(progress.budgetMinor ?: 0L)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Spent ${Money.formatInr(progress.expenseMinor)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = if (remaining >= 0) {
                        "Remaining ${Money.formatInr(remaining)}"
                    } else {
                        "Exceeded by ${Money.formatInr(-remaining)}"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (remaining < 0) colors.danger else MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { utilization.coerceAtMost(1.2f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription =
                                "Budget utilization ${"%.0f".format(progress.utilizationPercent ?: 0.0)} percent"
                        },
                    color = statusColor
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "$statusLabel · ${"%.0f".format(progress.utilizationPercent ?: 0.0)}%",
                    style = MaterialTheme.typography.labelLarge,
                    color = statusColor
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(onClick = onSetBudget) {
                    Text("Edit budget")
                }
            }
        }
    }
}
