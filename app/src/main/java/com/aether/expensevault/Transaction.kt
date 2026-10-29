package com.aether.expensevault

enum class TransactionType {
    EXPENSE,
    INCOME,
    SUBSCRIPTION
}

data class Transaction(
    val id: String,
    val title: String,
    val amount: Double,
    val category: String,
    val type: TransactionType,
    val date: Long,
    val currency: String = "USD",
    val note: String = ""
)
