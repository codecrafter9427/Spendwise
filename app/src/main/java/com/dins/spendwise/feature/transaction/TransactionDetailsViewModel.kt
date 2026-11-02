package com.dins.spendwise.feature.transaction

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dins.spendwise.domain.model.TransactionWithCategory
import com.dins.spendwise.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TransactionDetailsUiState(
    val item: TransactionWithCategory? = null,
    val showDeleteConfirm: Boolean = false,
    val isDeleting: Boolean = false,
    val errorMessage: String? = null,
    val deleted: Boolean = false
)

@HiltViewModel
class TransactionDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val transactionId: Long = checkNotNull(savedStateHandle["transactionId"])
    private val flags = MutableStateFlow(
        DetailsFlags(
            showDeleteConfirm = false,
            isDeleting = false,
            errorMessage = null,
            deleted = false
        )
    )

    private data class DetailsFlags(
        val showDeleteConfirm: Boolean,
        val isDeleting: Boolean,
        val errorMessage: String?,
        val deleted: Boolean
    )

    val uiState: StateFlow<TransactionDetailsUiState> = combine(
        transactionRepository.observeById(transactionId),
        flags
    ) { item, flag ->
        TransactionDetailsUiState(
            item = item,
            showDeleteConfirm = flag.showDeleteConfirm,
            isDeleting = flag.isDeleting,
            errorMessage = flag.errorMessage,
            deleted = flag.deleted
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TransactionDetailsUiState()
    )

    fun requestDelete() {
        flags.update { it.copy(showDeleteConfirm = true, errorMessage = null) }
    }

    fun dismissDelete() {
        flags.update { it.copy(showDeleteConfirm = false) }
    }

    fun confirmDelete() {
        viewModelScope.launch {
            flags.update { it.copy(isDeleting = true, errorMessage = null) }
            transactionRepository.delete(transactionId)
                .onSuccess {
                    flags.update {
                        it.copy(
                            isDeleting = false,
                            showDeleteConfirm = false,
                            deleted = true
                        )
                    }
                }
                .onFailure { error ->
                    flags.update {
                        it.copy(
                            isDeleting = false,
                            showDeleteConfirm = false,
                            errorMessage = error.message ?: "Could not delete transaction"
                        )
                    }
                }
        }
    }

    fun consumeDeleted() {
        flags.update { it.copy(deleted = false) }
    }
}
