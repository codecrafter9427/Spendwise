package com.dins.spendwise.feature.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dins.spendwise.domain.model.Category
import com.dins.spendwise.domain.model.TransactionType
import com.dins.spendwise.domain.model.TransactionWithCategory
import com.dins.spendwise.domain.repository.CategoryRepository
import com.dins.spendwise.domain.repository.TransactionRepository
import com.dins.spendwise.domain.usecase.TransactionFilters
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class TransactionListUiState(
    val yearMonth: YearMonth = YearMonth.now(),
    val query: String = "",
    val typeFilter: TransactionType? = null,
    val categoryIdFilter: Long? = null,
    val categories: List<Category> = emptyList(),
    val transactions: List<TransactionWithCategory> = emptyList()
)

@HiltViewModel
class TransactionListViewModel @Inject constructor(
    transactionRepository: TransactionRepository,
    categoryRepository: CategoryRepository
) : ViewModel() {

    private val filters = MutableStateFlow(
        FilterState(
            yearMonth = YearMonth.now(),
            query = "",
            type = null,
            categoryId = null
        )
    )

    private data class FilterState(
        val yearMonth: YearMonth,
        val query: String,
        val type: TransactionType?,
        val categoryId: Long?
    )

    val uiState: StateFlow<TransactionListUiState> = combine(
        filters,
        transactionRepository.observeAllWithCategory(),
        categoryRepository.observeAll()
    ) { filter, all, categories ->
        val filteredCategories = if (filter.type == null) {
            categories
        } else {
            categories.filter { it.type == filter.type }
        }
        val items = TransactionFilters.filter(
            items = all,
            yearMonth = filter.yearMonth,
            type = filter.type,
            categoryId = filter.categoryId,
            query = filter.query
        )
        TransactionListUiState(
            yearMonth = filter.yearMonth,
            query = filter.query,
            typeFilter = filter.type,
            categoryIdFilter = filter.categoryId,
            categories = filteredCategories,
            transactions = items
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TransactionListUiState()
    )

    fun previousMonth() {
        filters.update { it.copy(yearMonth = it.yearMonth.minusMonths(1)) }
    }

    fun nextMonth() {
        filters.update { it.copy(yearMonth = it.yearMonth.plusMonths(1)) }
    }

    fun onQueryChange(query: String) {
        filters.update { it.copy(query = query) }
    }

    fun onTypeFilterChange(type: TransactionType?) {
        filters.update { it.copy(type = type, categoryId = null) }
    }

    fun onCategoryFilterChange(categoryId: Long?) {
        filters.update { it.copy(categoryId = categoryId) }
    }
}
