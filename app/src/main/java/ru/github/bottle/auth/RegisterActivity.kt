package ru.github.bottle.auth

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import ru.github.bottle.R
import ru.github.bottle.databinding.ActivityRegisterBinding
import ru.github.bottle.data.repository.UserRepository
import ru.github.bottle.game.GameActivity
import ru.github.bottle.models.User
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private lateinit var userRepository: UserRepository

    private var selectedDate: Date? = null
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userRepository = UserRepository(this)
        setupClickListeners()
        setupDatePicker()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }

    private fun setupClickListeners() {
        binding.btnRegister.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (validateInput(username, password)) {
                registerUser(username, password)
            }
        }

        binding.tvLogin.setOnClickListener {
            finish()
        }
    }

    private fun setupDatePicker() {
        binding.etBirthDate.setOnClickListener {
            showCustomDatePicker()
        }

        binding.etBirthDate.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.hideSoftInputFromWindow(binding.etBirthDate.windowToken, 0)
                showCustomDatePicker()
            }
        }
    }

    private fun showCustomDatePicker() {
        val dialog = CustomDatePickerDialog(this) { year, month, day ->
            val dateCalendar = Calendar.getInstance()
            dateCalendar.set(year, month, day)
            selectedDate = dateCalendar.time

            binding.etBirthDate.setText(dateFormat.format(selectedDate))
            updateAgeHint()
        }
        dialog.show()
    }

    private fun updateAgeHint() {
        if (selectedDate != null) {
            val age = calculateAge(selectedDate!!)
            val ageText = when {
                age >= 18 -> getString(R.string.register_age_hint_18, age)
                age >= 16 -> getString(R.string.register_age_hint_16, age)
                age >= 10 -> getString(R.string.register_age_hint_10, age)
                age >= 5 -> getString(R.string.register_age_hint_5, age)
                else -> getString(R.string.register_age_hint_min)
            }
            binding.tvAgeHint.text = ageText

            val color = if (age >= 5) {
                if (age >= 18) {
                    ContextCompat.getColor(this, android.R.color.holo_green_dark)
                } else {
                    ContextCompat.getColor(this, android.R.color.holo_orange_dark)
                }
            } else {
                ContextCompat.getColor(this, android.R.color.holo_red_dark)
            }
            binding.tvAgeHint.setTextColor(color)
        }
    }

    private fun calculateAge(birthDate: Date): Int {
        val birthCalendar = Calendar.getInstance()
        birthCalendar.time = birthDate

        val currentCalendar = Calendar.getInstance()

        var age = currentCalendar.get(Calendar.YEAR) - birthCalendar.get(Calendar.YEAR)

        if (currentCalendar.get(Calendar.DAY_OF_YEAR) < birthCalendar.get(Calendar.DAY_OF_YEAR)) {
            age--
        }

        return age
    }

    private fun validateDate(birthDate: Date): Boolean {
        val now = Calendar.getInstance()
        val birth = Calendar.getInstance().apply { time = birthDate }

        // Нельзя выбрать дату в будущем
        if (birth.after(now)) {
            binding.etBirthDate.error = "Дата не может быть в будущем"
            return false
        }
        return true
    }

    private fun validateInput(username: String, password: String): Boolean {
        if (username.isEmpty()) {
            binding.etUsername.error = getString(R.string.register_username_hint)
            return false
        }
        if (username.length < 3) {
            binding.etUsername.error = getString(R.string.register_username_min_length)
            return false
        }
        if (password.isEmpty()) {
            binding.etPassword.error = getString(R.string.register_password_hint)
            return false
        }
        if (password.length < 6) {
            binding.etPassword.error = getString(R.string.register_password_min_length)
            return false
        }

        if (selectedDate == null) {
            binding.etBirthDate.error = getString(R.string.register_select_birthdate)
            return false
        }

        // Добавляем проверку даты
        if (!validateDate(selectedDate!!)) {
            return false
        }

        val age = calculateAge(selectedDate!!)
        if (age < 5) {
            binding.etBirthDate.error = getString(R.string.register_age_min)
            Toast.makeText(
                this,
                R.string.register_age_min_message,
                Toast.LENGTH_LONG
            ).show()
            return false
        }

        return true
    }

    private fun registerUser(username: String, password: String) {
        lifecycleScope.launch {
            try {
                if (userRepository.isUsernameExists(username)) {
                    Toast.makeText(
                        this@RegisterActivity,
                        R.string.register_user_exists,
                        Toast.LENGTH_LONG
                    ).show()
                    binding.etUsername.error = getString(R.string.register_user_exists)
                    binding.etUsername.setText("")
                    return@launch
                }

                val age = calculateAge(selectedDate!!)

                val user = User(
                    id = System.currentTimeMillis().toString(),
                    username = username,
                    age = age,
                    isGuest = false,
                    birthDate = selectedDate!!.time
                )

                userRepository.saveUser(user, password)

                Toast.makeText(
                    this@RegisterActivity,
                    R.string.register_success,
                    Toast.LENGTH_LONG
                ).show()

                startActivity(GameActivity.getIntent(this@RegisterActivity))
                finish()
            } catch (e: Exception) {
                Toast.makeText(
                    this@RegisterActivity,
                    getString(R.string.register_error) + ": ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    companion object {
        fun getIntent(context: Context): Intent {
            return Intent(context, RegisterActivity::class.java)
        }
    }
}