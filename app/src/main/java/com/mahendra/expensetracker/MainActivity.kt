package com.mahendra.expensetracker

import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.database.sqlite.SQLiteException
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputFilter
import android.text.InputType
import android.view.Gravity
import android.view.WindowInsets
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.math.BigDecimal
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Currency
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private class Storage(context: android.content.Context) {
        val database = ExpenseDatabase(context)
        val worker = Executors.newSingleThreadExecutor()
        var saveError = false
    }

    private lateinit var store: Storage
    private val main = Handler(Looper.getMainLooper())
    private val selectedDate = Calendar.getInstance(Locale.US)
    private lateinit var dateButton: Button
    private lateinit var totalView: TextView
    private lateinit var entries: LinearLayout
    private lateinit var money: NumberFormat
    private lateinit var currency: Currency
    private var descriptionField: EditText? = null
    private var amountField: EditText? = null
    private var entryDialog: AlertDialog? = null
    private var refreshGeneration = 0
    private var saveSubmitted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        store = lastNonConfigurationInstance as? Storage ?: Storage(applicationContext)
        savedInstanceState?.let { selectedDate.timeInMillis = it.getLong("selectedDate") }
        val preferences = getSharedPreferences("settings", MODE_PRIVATE)
        val currencyCode = preferences.getString("currency", null) ?: run {
            val code = runCatching { Currency.getInstance(Locale.getDefault()).currencyCode }
                .getOrDefault("USD")
            preferences.edit().putString("currency", code).apply()
            code
        }
        currency = Currency.getInstance(currencyCode)
        money = NumberFormat.getCurrencyInstance().apply {
            currency = this@MainActivity.currency
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }

        val root = column().apply { setPadding(dp(16), dp(16), dp(16), dp(16)) }
        root.setOnApplyWindowInsetsListener { view, insets ->
            val bars = if (android.os.Build.VERSION.SDK_INT >= 30) {
                insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.ime())
            } else null
            @Suppress("DEPRECATION")
            view.setPadding(
                dp(16) + (bars?.left ?: insets.systemWindowInsetLeft),
                dp(16) + (bars?.top ?: insets.systemWindowInsetTop),
                dp(16) + (bars?.right ?: insets.systemWindowInsetRight),
                dp(16) + (bars?.bottom ?: insets.systemWindowInsetBottom)
            )
            insets
        }
        root.addView(label(getString(R.string.app_name), 26f))
        dateButton = Button(this).apply {
            contentDescription = getString(R.string.choose_date)
            setOnClickListener { chooseDate() }
        }
        root.addView(dateButton)
        totalView = label("", 22f)
        root.addView(totalView)
        root.addView(Button(this).apply {
            setText(R.string.add_expense)
            setOnClickListener { showEntryDialog() }
        })
        entries = column()
        root.addView(ScrollView(this).apply { addView(entries) },
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        root.addView(label(getString(R.string.offline_notice), 12f))
        setContentView(root)
        root.requestApplyInsets()
        refresh()
        if (savedInstanceState?.getBoolean("entryOpen") == true) {
            showEntryDialog(
                savedInstanceState.getString("description", ""),
                savedInstanceState.getString("amount", "")
            )
        }
    }

    private fun dayKey(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(selectedDate.time)

    private fun chooseDate() {
        DatePickerDialog(this, { _, year, month, day ->
            selectedDate.set(year, month, day)
            refresh()
        }, selectedDate.get(Calendar.YEAR), selectedDate.get(Calendar.MONTH),
            selectedDate.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun refresh() {
        val day = dayKey()
        val generation = ++refreshGeneration
        dateButton.text = java.text.DateFormat.getDateInstance(java.text.DateFormat.FULL)
            .format(selectedDate.time)
        entries.removeAllViews()
        totalView.text = getString(R.string.daily_total, money.format(BigDecimal.ZERO))
        storage({ store.database.forDay(day) }) { expenses ->
            if (generation != refreshGeneration) return@storage
            entries.removeAllViews()
            if (store.saveError) {
                store.saveError = false
                Toast.makeText(this, R.string.storage_error, Toast.LENGTH_LONG).show()
            }
            totalView.text = getString(R.string.daily_total, money.format(ExpenseInput.total(expenses)))
            if (expenses.isEmpty()) {
                entries.addView(label(getString(R.string.empty_day), 16f))
            }
            expenses.forEach { expense ->
                val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
                val detail = column().apply {
                    addView(label(expense.description, 18f))
                    addView(label(money.format(BigDecimal.valueOf(expense.amountMinor, 2)), 16f))
                }
                row.addView(detail, LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                row.addView(Button(this).apply {
                    setText(R.string.delete)
                    contentDescription = getString(R.string.delete_accessibility, expense.description)
                    setOnClickListener { confirmDelete(expense) }
                })
                entries.addView(row)
            }
        }
    }

    private fun showEntryDialog(description: String = "", amount: String = "") {
        saveSubmitted = false
        val day = dayKey()
        val form = column().apply { setPadding(dp(20), dp(8), dp(20), dp(8)) }
        val descriptionInput = EditText(this).apply {
            hint = getString(R.string.description_hint)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            filters = arrayOf(InputFilter.LengthFilter(100))
            setText(description)
        }
        val amountInput = EditText(this).apply {
            hint = getString(R.string.amount_hint, currency.currencyCode)
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            filters = arrayOf(InputFilter.LengthFilter(12))
            setText(amount)
        }
        descriptionField = descriptionInput
        amountField = amountInput
        form.addView(descriptionInput)
        form.addView(amountInput)
        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.add_expense)
            .setMessage(dateButton.text)
            .setView(form)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.save, null)
            .create()
        entryDialog = dialog
        dialog.setOnDismissListener {
            entryDialog = null
            descriptionField = null
            amountField = null
        }
        dialog.setOnShowListener {
            val save = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            save.setOnClickListener {
                val validDescription = ExpenseInput.description(descriptionInput.text.toString())
                val minor = ExpenseInput.amountMinor(amountInput.text.toString())
                descriptionInput.error = if (validDescription == null)
                    getString(R.string.invalid_description) else null
                amountInput.error = if (minor == null) getString(R.string.invalid_amount) else null
                if (validDescription != null && minor != null) {
                    save.isEnabled = false
                    saveSubmitted = true
                    storage({
                        try {
                            store.database.add(day, validDescription, minor)
                        } catch (error: SQLiteException) {
                            store.saveError = true
                            throw error
                        }
                    }, onError = {
                        saveSubmitted = false
                        store.saveError = false
                        save.isEnabled = true
                    }) {
                        dialog.dismiss()
                        refresh()
                    }
                }
            }
        }
        dialog.show()
    }

    private fun confirmDelete(expense: Expense) {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_expense)
            .setMessage(getString(R.string.delete_message, expense.description,
                money.format(BigDecimal.valueOf(expense.amountMinor, 2))))
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete) { _, _ ->
                storage({ store.database.delete(expense.id) }) { refresh() }
            }
            .show()
    }

    private fun <T> storage(operation: () -> T, onError: () -> Unit = {}, success: (T) -> Unit) {
        store.worker.execute {
            try {
                val result = operation()
                main.post { if (!isDestroyed && !isFinishing) success(result) }
            } catch (_: SQLiteException) {
                main.post {
                    if (!isDestroyed && !isFinishing) {
                        Toast.makeText(this, R.string.storage_error, Toast.LENGTH_LONG).show()
                        onError()
                    }
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putLong("selectedDate", selectedDate.timeInMillis)
        outState.putBoolean("entryOpen", entryDialog?.isShowing == true && !saveSubmitted)
        outState.putString("description", descriptionField?.text?.toString())
        outState.putString("amount", amountField?.text?.toString())
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        entryDialog?.dismiss()
        if (!isChangingConfigurations) {
            store.worker.execute { store.database.close() }
            store.worker.shutdown()
        }
        super.onDestroy()
    }

    @Deprecated("Uses the platform retention API to keep database work ordered across rotation.")
    override fun onRetainNonConfigurationInstance(): Any = store

    private fun column() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

    private fun label(text: String, size: Float) = TextView(this).apply {
        this.text = text
        textSize = size
        setPadding(0, dp(8), 0, dp(8))
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
