package com.mahendra.expensetracker

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteOpenHelper
import android.database.sqlite.SQLiteDatabase

class ExpenseDatabase(context: Context) : SQLiteOpenHelper(context, "expenses.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE expenses (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                day TEXT NOT NULL,
                description TEXT NOT NULL CHECK(length(description) BETWEEN 1 AND 100),
                amount_minor INTEGER NOT NULL CHECK(amount_minor BETWEEN 1 AND ${ExpenseInput.MAX_AMOUNT_MINOR})
            )"""
        )
        db.execSQL("CREATE INDEX expenses_day ON expenses(day)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    fun forDay(day: String): List<Expense> {
        val expenses = mutableListOf<Expense>()
        readableDatabase.query(
            "expenses", arrayOf("id", "description", "amount_minor"),
            "day = ?", arrayOf(day), null, null, "id DESC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                expenses.add(Expense(cursor.getLong(0), cursor.getString(1), cursor.getLong(2)))
            }
        }
        return expenses
    }

    fun add(day: String, description: String, amountMinor: Long) {
        val values = ContentValues().apply {
            put("day", day)
            put("description", description)
            put("amount_minor", amountMinor)
        }
        writableDatabase.insertOrThrow("expenses", null, values)
    }

    fun delete(id: Long) {
        writableDatabase.delete("expenses", "id = ?", arrayOf(id.toString()))
    }
}
