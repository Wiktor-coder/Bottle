package ru.github.bottle.auth

import android.content.res.Configuration
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import ru.github.bottle.data.repository.UserRepository
import ru.github.bottle.databinding.ActivityLoginBinding
import ru.github.bottle.game.GameActivity

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var userRepository: UserRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userRepository = UserRepository(this)

        lifecycleScope.launch {
            if (userRepository.isLoggedIn()) {
                navigateToGame()
            }
        }

        setupClickListeners()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }

    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (validateInput(email, password)) {
                loginUser(email, password)
            }
        }

        binding.btnGuest.setOnClickListener {
            loginAsGuest()
        }

        binding.tvRegister.setOnClickListener {
            startActivity(RegisterActivity.getIntent(this))
        }

        // Тестовый вход
//        binding.btnTestLogin.setOnClickListener {
//            lifecycleScope.launch {
//                val testUser = User(
//                    id = System.currentTimeMillis().toString(),
//                    email = "test@test.com",
//                    username = "Тестовый пользователь",
//                    age = 25,
//                    isGuest = false
//                )
//                userRepository.saveUser(testUser)
//                Toast.makeText(
//                    this@LoginActivity,
//                    "Тестовый вход (18+) выполнен",
//                    Toast.LENGTH_LONG
//                ).show()
//                navigateToGame()
//            }
//        }
    }

    private fun validateInput(email: String, password: String): Boolean {
        if (email.isEmpty()) {
            binding.etEmail.error = "Введите email"
            return false
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.error = "Введите корректный email"
            return false
        }
        if (password.isEmpty()) {
            binding.etPassword.error = "Введите пароль"
            return false
        }
        return true
    }

    private fun loginUser(email: String, password: String) {
        lifecycleScope.launch {
            try {
                // Проверяем, есть ли пользователь с таким email
                val user = userRepository.getUser()

                if (user != null && user.email == email) {
                    // Пользователь найден - входим
                    userRepository.loginUser(user)

                    val message = if (user.age >= 18) {
                        "Добро пожаловать, ${user.username}! Доступны все режимы"
                    } else {
                        "Добро пожаловать, ${user.username}! Доступен только детский режим"
                    }

                    Toast.makeText(
                        this@LoginActivity,
                        message,
                        Toast.LENGTH_LONG
                    ).show()
                    navigateToGame()
                } else {
                    Toast.makeText(
                        this@LoginActivity,
                        "Пользователь не найден. Проверьте email или зарегистрируйтесь.",
                        Toast.LENGTH_LONG
                    ).show()
                    navigateToGame()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@LoginActivity,
                    "Ошибка входа: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun loginAsGuest() {
        lifecycleScope.launch {
            try {
                userRepository.saveGuestUser()
                Toast.makeText(
                    this@LoginActivity,
                    "Добро пожаловать, Гость! Доступен только детский режим",
                    Toast.LENGTH_LONG
                ).show()
                navigateToGame()
            } catch (e: Exception) {
                Toast.makeText(
                    this@LoginActivity,
                    "Ошибка: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun navigateToGame() {
        startActivity(GameActivity.getIntent(this))
        finish()
    }
}