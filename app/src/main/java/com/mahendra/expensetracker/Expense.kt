package com.mahendra.expensetracker

import java.math.BigDecimal

data class Expense(val id: Long, val description: String, val amountMinor: Long)

object ExpenseInput {
    const val MAX_AMOUNT_MINOR = 99_999_999_999L

    fun amountMinor(input: String): Long? {
        val text = input.trim()
        if (!Regex("[0-9]{1,9}([.,][0-9]{1,2})?").matches(text)) return null
        val minor = BigDecimal(text.replace(',', '.')).movePointRight(2).longValueExact()
        return minor.takeIf { it in 1..MAX_AMOUNT_MINOR }
    }

    fun description(input: String): String? =
        input.trim().takeIf { it.isNotEmpty() && it.length <= 100 }

    fun total(expenses: List<Expense>): BigDecimal =
        expenses.fold(BigDecimal.ZERO) { sum, expense ->
            sum + BigDecimal.valueOf(expense.amountMinor, 2)
        }
}
