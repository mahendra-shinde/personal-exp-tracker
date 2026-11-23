package com.mahendra.expensetracker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal

class ExpenseInputTest {
    @Test fun convertsAmountsWithoutFloatingPointRounding() {
        assertEquals(10L, ExpenseInput.amountMinor("0.10"))
        assertEquals(12345L, ExpenseInput.amountMinor(" 123.45 "))
        assertEquals(12345L, ExpenseInput.amountMinor("123,45"))
        assertEquals(100L, ExpenseInput.amountMinor("1"))
        assertEquals(ExpenseInput.MAX_AMOUNT_MINOR, ExpenseInput.amountMinor("999999999.99"))
    }

    @Test fun rejectsInvalidAndOutOfRangeAmounts() {
        listOf("", " ", "0", "0.00", "-1", "+1", "NaN", "1e3", "1.001",
            "1,234.56", "1.234,56", "1000000000", ".", "1.", "1\n2").forEach {
            assertNull(it, ExpenseInput.amountMinor(it))
        }
    }

    @Test fun descriptionsAreTrimmedAndRequired() {
        assertEquals("Lunch", ExpenseInput.description(" Lunch "))
        assertEquals("a".repeat(100), ExpenseInput.description("a".repeat(100)))
        assertNull(ExpenseInput.description(" \n "))
        assertNull(ExpenseInput.description("a".repeat(101)))
    }

    @Test fun dailyTotalsAreExactAndDoNotOverflowLong() {
        assertEquals(BigDecimal("0.30"), ExpenseInput.total(listOf(
            Expense(1, "First", 10), Expense(2, "Second", 20)
        )))
        assertEquals(BigDecimal.ZERO, ExpenseInput.total(emptyList()))
        assertEquals(BigDecimal.valueOf(Long.MAX_VALUE, 2).multiply(BigDecimal(2)),
            ExpenseInput.total(listOf(
                Expense(1, "First", Long.MAX_VALUE), Expense(2, "Second", Long.MAX_VALUE)
            )))
    }
}
