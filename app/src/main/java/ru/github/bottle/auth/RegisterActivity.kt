package ru.github.bottle.auth

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import ru.github.bottle.databinding.ActivityRegisterBinding
import ru.github.bottle.data.repository.UserRepository
import ru.github.bottle.models.User

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private lateinit var userRepository: UserRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userRepository = UserRepository(this)
        setupClickListeners()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }

    private fun setupClickListeners() {
        binding.btnRegister.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val age = binding.etAge.text.toString().trim()

            if (validateInput(email, username, password, age)) {
                registerUser(email, username, password, age.toInt())
            }
        }

        binding.tvLogin.setOnClickListener {
            finish()
        }
    }

    private fun validateInput(email: String, username: String, password: String, age: String): Boolean {
        if (email.isEmpty()) {
            binding.etEmail.error = "Введите email"
            return false
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.error = "Введите корректный email"
            return false
        }
        if (username.isEmpty()) {
            binding.etUsername.error = "Введите имя"
            return false
        }
        if (username.length < 3) {
            binding.etUsername.error = "Имя должно быть не менее 3 символов"
            return false
        }
        if (password.isEmpty()) {
            binding.etPassword.error = "Введите пароль"
            return false
        }
        if (password.length < 6) {
            binding.etPassword.error = "Пароль должен быть не менее 6 символов"
            return false
        }
        if (age.isEmpty()) {
            binding.etAge.error = "Введите возраст"
            return false
        }
        val ageInt = age.toIntOrNull()
        if (ageInt == null) {
            binding.etAge.error = "Введите корректный возраст"
            return false
        }
        if (ageInt < 5) {
            binding.etAge.error = "Возраст должен быть от 5 лет"
            return false
        }
        if (ageInt > 120) {
            binding.etAge.error = "Введите корректный возраст"
            return false
        }
        return true
    }

    private fun registerUser(email: String, username: String, password: String, age: Int) {
        lifecycleScope.launch {
            try {
                // Проверяем, не зарегистрирован ли уже такой пользователь
                val existingUser = userRepository.getUser()
                if (existingUser != null && existingUser.email == email) {
                    Toast.makeText(
                        this@RegisterActivity,
                        "Пользователь с таким email уже существует",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }

                val user = User(
                    id = System.currentTimeMillis().toString(),
                    email = email,
                    username = username,
                    age = age,
                    isGuest = false
                )

                userRepository.saveUser(user)

                Toast.makeText(
                    this@RegisterActivity,
                    "Регистрация успешна!",
                    Toast.LENGTH_LONG).show()

                // Закрываем регистрацию и возвращаемся к логину
                finish()
            } catch (e: Exception) {
                Toast.makeText(
                    this@RegisterActivity,
                    "Ошибка регистрации: ${e.message}",
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