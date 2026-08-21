package ru.github.bottle.auth

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import ru.github.bottle.R
import ru.github.bottle.data.repository.UserRepository
import ru.github.bottle.databinding.ActivityLoginBinding
import ru.github.bottle.game.GameActivity
import androidx.core.content.edit

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
                val user = userRepository.getUser()
                if (user != null) {
                    navigateToGame()
                    return@launch
                } else {
                    userRepository.logout()
                }
            }

            val lastUsername = getLastUsername()
            if (lastUsername.isNotEmpty()) {
                binding.etUsername.setText(lastUsername)
            }
        }

        setupClickListeners()
    }

    private fun saveLastUsername(username: String) {
        val prefs = getSharedPreferences("login_prefs", MODE_PRIVATE)
        prefs.edit { putString("last_username", username) }
    }

    private fun getLastUsername(): String {
        val prefs = getSharedPreferences("login_prefs", MODE_PRIVATE)
        return prefs.getString("last_username", "") ?: ""
    }

    private fun loginUser(username: String, password: String) {
        // Блокируем кнопку и показываем индикатор загрузки
        binding.btnLogin.isEnabled = false
        binding.btnLogin.text = getString(R.string.login_loading)

        lifecycleScope.launch {
            try {
                val user = userRepository.findUserByUsername(username)

                if (user != null) {
                    val isPasswordCorrect = userRepository.checkPassword(username, password)

                    if (isPasswordCorrect) {
                        saveLastUsername(username)
                        userRepository.loginUser(user)

                        val message = if (user.age >= 18) {
                            getString(R.string.login_welcome_user, user.username)
                        } else {
                            getString(R.string.login_welcome_user_children, user.username)
                        }

                        Toast.makeText(
                            this@LoginActivity,
                            message,
                            Toast.LENGTH_LONG
                        ).show()
                        navigateToGame()
                    } else {
                        binding.etPassword.error = getString(R.string.login_wrong_password)
                        Toast.makeText(
                            this@LoginActivity,
                            R.string.login_wrong_password,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    binding.etUsername.error = getString(R.string.login_user_not_found)
                    Toast.makeText(
                        this@LoginActivity,
                        R.string.login_user_not_found,
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@LoginActivity,
                    getString(R.string.login_error) + ": ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                // Разблокируем кнопку в любом случае
                binding.btnLogin.isEnabled = true
                binding.btnLogin.text = getString(R.string.login_button)
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }

    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (validateInput(username, password)) {
                loginUser(username, password)
            }
        }

        binding.btnGuest.setOnClickListener {
            loginAsGuest()
        }

        binding.tvRegister.setOnClickListener {
            startActivity(RegisterActivity.getIntent(this))
        }
    }

    private fun validateInput(username: String, password: String): Boolean {
        if (username.isEmpty()) {
            binding.etUsername.error = getString(R.string.login_invalid_username)
            return false
        }
        if (password.isEmpty()) {
            binding.etPassword.error = getString(R.string.login_invalid_password)
            return false
        }
        return true
    }

    private fun loginAsGuest() {
        lifecycleScope.launch {
            try {
                val existingUser = userRepository.getUser()
                if (existingUser?.isGuest == true) {
                    Toast.makeText(
                        this@LoginActivity,
                        R.string.login_welcome_guest,
                        Toast.LENGTH_LONG
                    ).show()
                    navigateToGame()
                    return@launch
                }

                userRepository.saveGuestUser()
                Toast.makeText(
                    this@LoginActivity,
                    R.string.login_welcome_guest,
                    Toast.LENGTH_LONG
                ).show()
                navigateToGame()
            } catch (e: Exception) {
                Toast.makeText(
                    this@LoginActivity,
                    getString(R.string.login_error) + ": ${e.message}",
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