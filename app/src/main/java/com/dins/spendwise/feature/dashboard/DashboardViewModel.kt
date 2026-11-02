package com.dins.spendwise.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dins.spendwise.domain.model.TransactionType
import com.dins.spendwise.domain.model.TransactionWithCategory
import com.dins.spendwise.domain.repository.BudgetRepository
import com.dins.spendwise.domain.repository.TransactionRepository
import com.dins.spendwise.domain.usecase.BudgetProgress
import com.dins.spendwise.domain.usecase.FinanceCalculations
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

data class DashboardUiState(
    val yearMonth: YearMonth = YearMonth.now(),
    val incomeMinor: Long = 0L,
    val expenseMinor: Long = 0L,
    val netMinor: Long = 0L,
    val budgetProgress: BudgetProgress = FinanceCalculations.budgetProgress(null, 0L),
    val recentTransactions: List<TransactionWithCategory> = emptyList(),
    val hasAnyTransactions: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val budgetRepository: BudgetRepository
) : ViewModel() {

    private val selectedMonth = MutableStateFlow(YearMonth.now())

    private val monthData = selectedMonth.flatMapLatest { month ->
        combine(
            transactionRepository.observeSumByTypeAndMonth(TransactionType.INCOME, month),
            transactionRepository.observeSumByTypeAndMonth(TransactionType.EXPENSE, month),
            budgetRepository.observeBudget(month),
            transactionRepository.observeRecentByMonth(month, 5),
            transactionRepository.observeByMonthWithCategory(month)
        ) { income, expense, budget, recent, allInMonth ->
            DashboardUiState(
                yearMonth = month,
                incomeMinor = income,
                expenseMinor = expense,
                netMinor = income - expense,
                budgetProgress = FinanceCalculations.budgetProgress(budget?.amountMinor, expense),
                recentTransactions = recent,
                hasAnyTransactions = allInMonth.isNotEmpty()
            )
        }
    }

    val uiState: StateFlow<DashboardUiState> = monthData.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardUiState()
    )

    fun previousMonth() {
        selectedMonth.value = selectedMonth.value.minusMonths(1)
    }

    fun nextMonth() {
        selectedMonth.value = selectedMonth.value.plusMonths(1)
    }
}
