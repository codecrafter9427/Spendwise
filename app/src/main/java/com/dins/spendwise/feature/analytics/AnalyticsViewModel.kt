package com.dins.spendwise.feature.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dins.spendwise.domain.repository.TransactionRepository
import com.dins.spendwise.domain.usecase.CategoryExpenseShare
import com.dins.spendwise.domain.usecase.FinanceCalculations
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class AnalyticsUiState(
    val yearMonth: YearMonth = YearMonth.now(),
    val totalExpenseMinor: Long = 0L,
    val shares: List<CategoryExpenseShare> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val selectedMonth = MutableStateFlow(YearMonth.now())

    val uiState: StateFlow<AnalyticsUiState> = selectedMonth.flatMapLatest { month ->
        transactionRepository.observeByMonthWithCategory(month).map { items ->
            val shares = FinanceCalculations.categoryExpenseShares(items, month)
            AnalyticsUiState(
                yearMonth = month,
                totalExpenseMinor = shares.sumOf { it.amountMinor },
                shares = shares
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AnalyticsUiState()
    )

    fun previousMonth() {
        selectedMonth.value = selectedMonth.value.minusMonths(1)
    }

    fun nextMonth() {
        selectedMonth.value = selectedMonth.value.plusMonths(1)
    }
}
