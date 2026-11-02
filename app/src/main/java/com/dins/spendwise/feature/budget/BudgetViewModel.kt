package com.dins.spendwise.feature.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dins.spendwise.core.utils.Money
import com.dins.spendwise.domain.repository.BudgetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BudgetDialogUiState(
    val yearMonth: YearMonth = YearMonth.now(),
    val amountText: String = "",
    val amountError: String? = null,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val saved: Boolean = false
)

@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BudgetDialogUiState())
    val uiState: StateFlow<BudgetDialogUiState> = _uiState.asStateFlow()

    fun prepare(yearMonth: YearMonth, existingAmountMinor: Long?) {
        _uiState.value = BudgetDialogUiState(
            yearMonth = yearMonth,
            amountText = existingAmountMinor?.let { Money.minorToInputString(it) }.orEmpty()
        )
    }

    fun onAmountChange(value: String) {
        _uiState.update {
            it.copy(
                amountText = value,
                amountError = null,
                errorMessage = null,
                saved = false
            )
        }
    }

    fun save() {
        val current = _uiState.value
        val parsed = Money.parseToMinor(current.amountText)
        if (parsed.isFailure) {
            _uiState.update { it.copy(amountError = parsed.exceptionOrNull()?.message) }
            return
        }
        val amountMinor = parsed.getOrThrow()
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            budgetRepository.setBudget(current.yearMonth, amountMinor)
                .onSuccess {
                    _uiState.update { it.copy(isSaving = false, saved = true) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = error.message ?: "Could not save budget"
                        )
                    }
                }
        }
    }

    fun consumeSaved() {
        _uiState.update { it.copy(saved = false) }
    }
}
