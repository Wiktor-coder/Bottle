package ru.github.bottle.auth

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.NumberPicker
import ru.github.bottle.R
import java.util.Calendar

class CustomDatePickerDialog(
    context: Context,
    private val onDateSelected: (year: Int, month: Int, day: Int) -> Unit
) : Dialog(context) {

    private lateinit var yearPicker: NumberPicker
    private lateinit var monthPicker: NumberPicker
    private lateinit var dayPicker: NumberPicker
    private lateinit var btnConfirm: Button
    private lateinit var btnCancel: Button

    private var selectedYear: Int = 2000
    private var selectedMonth: Int = 0
    private var selectedDay: Int = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_date_picker)

        yearPicker = findViewById(R.id.npYear)
        monthPicker = findViewById(R.id.npMonth)
        dayPicker = findViewById(R.id.npDay)
        btnConfirm = findViewById(R.id.btnConfirm)
        btnCancel = findViewById(R.id.btnCancel)

        setupPickers()
        setupListeners()
    }

    private fun setupPickers() {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)

        yearPicker.minValue = currentYear - 100
        yearPicker.maxValue = currentYear
        yearPicker.value = currentYear - 18
        yearPicker.wrapSelectorWheel = false

        monthPicker.minValue = 1
        monthPicker.maxValue = 12
        monthPicker.value = 1
        monthPicker.wrapSelectorWheel = false

        dayPicker.minValue = 1
        dayPicker.maxValue = 31
        dayPicker.value = 1
        dayPicker.wrapSelectorWheel = false

        selectedYear = yearPicker.value
        selectedMonth = monthPicker.value - 1
        selectedDay = dayPicker.value

        yearPicker.setOnValueChangedListener { _, _, newVal ->
            selectedYear = newVal
            updateDayPicker()
        }

        monthPicker.setOnValueChangedListener { _, _, newVal ->
            selectedMonth = newVal - 1
            updateDayPicker()
        }
    }

    private fun updateDayPicker() {
        val daysInMonth = getDaysInMonth(selectedYear, selectedMonth)
        dayPicker.maxValue = daysInMonth
        if (dayPicker.value > daysInMonth) {
            dayPicker.value = daysInMonth
        }
        selectedDay = dayPicker.value
    }

    private fun getDaysInMonth(year: Int, month: Int): Int {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, 1)
        return calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    private fun setupListeners() {
        btnConfirm.setOnClickListener {
            onDateSelected(selectedYear, selectedMonth, selectedDay)
            dismiss()
        }

        btnCancel.setOnClickListener {
            dismiss()
        }

        dayPicker.setOnValueChangedListener { _, _, newVal ->
            selectedDay = newVal
        }
    }
}