package com.dins.spendwise.core.navigation

object Routes {
    const val HOME = "home"
    const val TRANSACTIONS = "transactions"
    const val ANALYTICS = "analytics"
    const val SETTINGS = "settings"
    const val ADD_TRANSACTION = "add_transaction"
    const val EDIT_TRANSACTION = "edit_transaction/{transactionId}"
    const val TRANSACTION_DETAILS = "transaction_details/{transactionId}"

    fun editTransaction(id: Long) = "edit_transaction/$id"
    fun transactionDetails(id: Long) = "transaction_details/$id"

    val bottomBarDestinations = listOf(HOME, TRANSACTIONS, ANALYTICS, SETTINGS)
}
