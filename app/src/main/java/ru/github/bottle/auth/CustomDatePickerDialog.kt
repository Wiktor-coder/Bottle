package ru.github.bottle.auth

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.NumberPicker
import ru.github.bottle.R
import ru.github.bottle.databinding.DialogDatePickerBinding
import java.util.Calendar

class CustomDatePickerDialog(
    context: Context,
    private val onDateSelected: (year: Int, month: Int, day: Int) -> Unit
) : Dialog(context) {

    companion object {
        private const val DEFAULT_AGE = 18
        private const val MAX_YEARS_BACK = 100
        private const val MIN_MONTH = 1
        private const val MAX_MONTH = 12
        private const val MIN_DAY = 1
        private const val DEFAULT_DAY = 1
        private const val DEFAULT_MONTH = 1
    }

    private lateinit var binding: DialogDatePickerBinding

//    private lateinit var yearPicker: NumberPicker
//    private lateinit var monthPicker: NumberPicker
//    private lateinit var dayPicker: NumberPicker
//    private lateinit var btnConfirm: Button
//    private lateinit var btnCancel: Button

    private var selectedYear: Int = 2000
    private var selectedMonth: Int = 0
    private var selectedDay: Int = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogDatePickerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupPickers()
        setupListeners()
    }

    private fun setupPickers() {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)

        binding.npYear.minValue = currentYear - MAX_YEARS_BACK
        binding.npYear.maxValue = currentYear
        binding.npYear.value = currentYear - DEFAULT_AGE
        binding.npYear.wrapSelectorWheel = false

        binding.npMonth.minValue = MIN_MONTH
        binding.npMonth.maxValue = MAX_MONTH
        binding.npMonth.value = DEFAULT_MONTH
        binding.npMonth.wrapSelectorWheel = false

        binding.npDay.minValue = MIN_DAY
        binding.npDay.maxValue = 31
        binding.npDay.value = DEFAULT_DAY
        binding.npDay.wrapSelectorWheel = false

        selectedYear = binding.npYear.value
        selectedMonth = binding.npMonth.value - 1
        selectedDay = binding.npDay.value

        binding.npYear.setOnValueChangedListener { _, _, newVal ->
            selectedYear = newVal
            updateDayPicker()
        }

        binding.npMonth.setOnValueChangedListener { _, _, newVal ->
            selectedMonth = newVal - 1
            updateDayPicker()
        }
    }

    private fun updateDayPicker() {
        val daysInMonth = getDaysInMonth(selectedYear, selectedMonth)
        binding.npDay.maxValue = daysInMonth
        if (binding.npDay.value > daysInMonth) {
            binding.npDay.value = daysInMonth
        }
        selectedDay = binding.npDay.value
    }

    private fun getDaysInMonth(year: Int, month: Int): Int {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, 1)
        return calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    private fun setupListeners() {
        binding.btnConfirm.setOnClickListener {
            onDateSelected(selectedYear, selectedMonth, selectedDay)
            dismiss()
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.npDay.setOnValueChangedListener { _, _, newVal ->
            selectedDay = newVal
        }
    }
}