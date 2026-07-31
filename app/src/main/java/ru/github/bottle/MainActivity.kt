package ru.github.bottle

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import ru.github.bottle.auth.LoginActivity
import ru.github.bottle.data.repository.UserRepository
import ru.github.bottle.game.GameActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Просто проверяем, залогинен ли пользователь, и перенаправляем
        lifecycleScope.launch {
            val userRepository = UserRepository(this@MainActivity)
            if (userRepository.isLoggedIn()) {
                startActivity(Intent(this@MainActivity, GameActivity::class.java))
            } else {
                startActivity(Intent(this@MainActivity, LoginActivity::class.java))
            }
            finish()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }
}