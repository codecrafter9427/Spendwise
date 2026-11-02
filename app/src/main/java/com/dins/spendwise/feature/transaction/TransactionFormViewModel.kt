package com.dins.spendwise.feature.transaction

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dins.spendwise.core.utils.Money
import com.dins.spendwise.domain.model.Category
import com.dins.spendwise.domain.model.Transaction
import com.dins.spendwise.domain.model.TransactionType
import com.dins.spendwise.domain.repository.CategoryRepository
import com.dins.spendwise.domain.repository.TransactionRepository
import com.dins.spendwise.domain.usecase.TransactionFormInput
import com.dins.spendwise.domain.usecase.TransactionValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TransactionFormUiState(
    val isEdit: Boolean = false,
    val type: TransactionType = TransactionType.EXPENSE,
    val amountText: String = "",
    val selectedCategoryId: Long? = null,
    val description: String = "",
    val date: LocalDate = LocalDate.now(),
    val categories: List<Category> = emptyList(),
    val amountError: String? = null,
    val categoryError: String? = null,
    val descriptionError: String? = null,
    val dateError: String? = null,
    val canSave: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val saved: Boolean = false,
    val existingCreatedAt: Long? = null,
    val existingId: Long? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TransactionFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val editId: Long? = savedStateHandle.get<Long>("transactionId")

    private val form = MutableStateFlow(
        TransactionFormUiState(isEdit = editId != null, existingId = editId)
    )

    private val categoriesFlow = form.flatMapLatest {
        categoryRepository.observeByType(it.type)
    }

    val uiState: StateFlow<TransactionFormUiState> = combine(form, categoriesFlow) { state, categories ->
        val validation = TransactionValidator.validate(
            TransactionFormInput(
                type = state.type,
                amountText = state.amountText,
                category = categories.find { it.id == state.selectedCategoryId },
                description = state.description,
                dateEpochDay = state.date.toEpochDay()
            )
        )
        val categoryMismatch = validation.categoryError?.contains("match") == true
        val showCategoryError = categoryMismatch ||
            (state.selectedCategoryId == null && state.amountText.isNotBlank())
        state.copy(
            categories = categories,
            amountError = if (state.amountText.isBlank()) null else validation.amountError,
            categoryError = validation.categoryError.takeIf { showCategoryError },
            descriptionError = validation.descriptionError,
            dateError = validation.dateError,
            canSave = validation.isValid && !state.isSaving
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TransactionFormUiState(isEdit = editId != null, existingId = editId)
    )

    init {
        if (editId != null) {
            viewModelScope.launch {
                val item = transactionRepository.observeById(editId).first { it != null }
                item?.let { loaded ->
                    form.update {
                        it.copy(
                            type = loaded.transaction.type,
                            amountText = Money.minorToInputString(loaded.transaction.amountMinor),
                            selectedCategoryId = loaded.transaction.categoryId,
                            description = loaded.transaction.description.orEmpty(),
                            date = loaded.transaction.transactionDate,
                            existingCreatedAt = loaded.transaction.createdAt,
                            existingId = loaded.transaction.id
                        )
                    }
                }
            }
        }
    }

    fun onTypeChange(type: TransactionType) {
        form.update {
            it.copy(
                type = type,
                selectedCategoryId = null,
                errorMessage = null,
                saved = false
            )
        }
    }

    fun onAmountChange(value: String) {
        form.update { it.copy(amountText = value, errorMessage = null, saved = false) }
    }

    fun onCategorySelected(categoryId: Long) {
        form.update { it.copy(selectedCategoryId = categoryId, errorMessage = null, saved = false) }
    }

    fun onDescriptionChange(value: String) {
        form.update { it.copy(description = value, errorMessage = null, saved = false) }
    }

    fun onDateChange(date: LocalDate) {
        form.update { it.copy(date = date, errorMessage = null, saved = false) }
    }

    fun save() {
        val current = form.value
        val categories = uiState.value.categories
        val category = categories.find { it.id == current.selectedCategoryId }
        val validation = TransactionValidator.validate(
            TransactionFormInput(
                type = current.type,
                amountText = current.amountText,
                category = category,
                description = current.description,
                dateEpochDay = current.date.toEpochDay()
            )
        )
        if (!validation.isValid) {
            form.update {
                it.copy(
                    amountError = validation.amountError,
                    categoryError = validation.categoryError,
                    descriptionError = validation.descriptionError,
                    dateError = validation.dateError
                )
            }
            return
        }
        val now = System.currentTimeMillis()
        val transaction = Transaction(
            id = current.existingId ?: 0L,
            type = current.type,
            amountMinor = validation.amountMinor!!,
            categoryId = category!!.id,
            description = validation.trimmedDescription,
            transactionDate = current.date,
            createdAt = current.existingCreatedAt ?: now,
            updatedAt = now
        )
        viewModelScope.launch {
            form.update { it.copy(isSaving = true, errorMessage = null) }
            val result = if (current.isEdit) {
                transactionRepository.update(transaction).map { }
            } else {
                transactionRepository.add(transaction).map { }
            }
            result
                .onSuccess {
                    form.update { it.copy(isSaving = false, saved = true) }
                }
                .onFailure { error ->
                    form.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = error.message ?: "Could not save transaction"
                        )
                    }
                }
        }
    }

    fun consumeSaved() {
        form.update { it.copy(saved = false) }
    }
}
