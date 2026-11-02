package com.dins.spendwise.feature.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dins.spendwise.core.designsystem.EmptyStateMessage
import com.dins.spendwise.core.designsystem.MonthSelector
import com.dins.spendwise.core.utils.Money
import com.dins.spendwise.domain.usecase.CategoryExpenseShare

private val chartPalette = listOf(
    Color(0xFF0F766E),
    Color(0xFF2563EB),
    Color(0xFFD97706),
    Color(0xFFBE123C),
    Color(0xFF7C3AED),
    Color(0xFF059669),
    Color(0xFF0891B2),
    Color(0xFFCA8A04),
    Color(0xFFDB2777),
    Color(0xFF4B5563),
    Color(0xFF65A30D),
    Color(0xFFEA580C)
)

@Composable
fun AnalyticsRoute(
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    AnalyticsScreen(
        state = state,
        onPreviousMonth = viewModel::previousMonth,
        onNextMonth = viewModel::nextMonth
    )
}

@Composable
fun AnalyticsScreen(
    state: AnalyticsUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = "Analytics",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Expense distribution",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
        )
        Spacer(modifier = Modifier.height(12.dp))
        MonthSelector(
            yearMonth = state.yearMonth,
            onPrevious = onPreviousMonth,
            onNext = onNextMonth
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (state.shares.isEmpty()) {
            EmptyStateMessage(
                title = "No expenses this month",
                subtitle = "Add expense transactions to see category breakdowns."
            )
        } else {
            Text(
                text = "Total expenses ${Money.formatInr(state.totalExpenseMinor)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(16.dp))
            DonutChart(
                shares = state.shares,
                modifier = Modifier
                    .size(220.dp)
                    .align(Alignment.CenterHorizontally)
                    .semantics {
                        contentDescription = "Donut chart of expense categories for the selected month"
                    }
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "By category",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            state.shares.forEachIndexed { index, share ->
                CategoryShareRow(
                    share = share,
                    color = chartPalette[index % chartPalette.size]
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun DonutChart(
    shares: List<CategoryExpenseShare>,
    modifier: Modifier = Modifier
) {
    val total = shares.sumOf { it.amountMinor }.toFloat().coerceAtLeast(1f)
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = size.minDimension * 0.18f, cap = StrokeCap.Butt)
        var startAngle = -90f
        shares.forEachIndexed { index, share ->
            val sweep = (share.amountMinor / total) * 360f
            drawArc(
                color = chartPalette[index % chartPalette.size],
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = false,
                style = stroke,
                topLeft = Offset(stroke.width / 2, stroke.width / 2),
                size = Size(size.width - stroke.width, size.height - stroke.width)
            )
            startAngle += sweep
        }
    }
}

@Composable
private fun CategoryShareRow(
    share: CategoryExpenseShare,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .semantics {
                contentDescription =
                    "${share.category.name}: ${Money.formatInr(share.amountMinor)}, ${"%.1f".format(share.percentOfTotal)} percent"
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(share.category.name, style = MaterialTheme.typography.titleMedium)
            Text(
                text = "%.1f%%".format(share.percentOfTotal),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
            )
        }
        Text(
            text = Money.formatInr(share.amountMinor),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}
