package com.dins.spendwise.feature.transaction

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dins.spendwise.core.designsystem.EmptyStateMessage
import com.dins.spendwise.core.designsystem.MonthSelector
import com.dins.spendwise.core.designsystem.TransactionListItem
import com.dins.spendwise.domain.model.TransactionType

@Composable
fun TransactionListRoute(
    onTransactionClick: (Long) -> Unit,
    viewModel: TransactionListViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TransactionListScreen(
        state = state,
        onPreviousMonth = viewModel::previousMonth,
        onNextMonth = viewModel::nextMonth,
        onQueryChange = viewModel::onQueryChange,
        onTypeFilterChange = viewModel::onTypeFilterChange,
        onCategoryFilterChange = viewModel::onCategoryFilterChange,
        onTransactionClick = onTransactionClick
    )
}

@Composable
fun TransactionListScreen(
    state: TransactionListUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onQueryChange: (String) -> Unit,
    onTypeFilterChange: (TransactionType?) -> Unit,
    onCategoryFilterChange: (Long?) -> Unit,
    onTransactionClick: (Long) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = "Transactions",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        MonthSelector(
            yearMonth = state.yearMonth,
            onPrevious = onPreviousMonth,
            onNext = onNextMonth
        )
        OutlinedTextField(
            value = state.query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Search category or description") },
            leadingIcon = {
                Icon(Icons.Filled.Search, contentDescription = null)
            }
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = state.typeFilter == null,
                onClick = { onTypeFilterChange(null) },
                label = { Text("All") }
            )
            FilterChip(
                selected = state.typeFilter == TransactionType.INCOME,
                onClick = { onTypeFilterChange(TransactionType.INCOME) },
                label = { Text("Income") }
            )
            FilterChip(
                selected = state.typeFilter == TransactionType.EXPENSE,
                onClick = { onTypeFilterChange(TransactionType.EXPENSE) },
                label = { Text("Expense") }
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = state.categoryIdFilter == null,
                onClick = { onCategoryFilterChange(null) },
                label = { Text("All categories") }
            )
            state.categories.forEach { category ->
                FilterChip(
                    selected = state.categoryIdFilter == category.id,
                    onClick = { onCategoryFilterChange(category.id) },
                    label = { Text(category.name) }
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        if (state.transactions.isEmpty()) {
            EmptyStateMessage(
                title = "No matching transactions",
                subtitle = "Try another month, clear filters, or add a new transaction."
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(
                    items = state.transactions,
                    key = { it.transaction.id }
                ) { item ->
                    TransactionListItem(
                        item = item,
                        onClick = { onTransactionClick(item.transaction.id) }
                    )
                }
                item { Spacer(modifier = Modifier.height(88.dp)) }
            }
        }
    }
}
