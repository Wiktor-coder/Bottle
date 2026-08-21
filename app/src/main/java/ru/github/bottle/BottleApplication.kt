package ru.github.bottle

import android.app.Application
import ru.github.bottle.utils.LanguageManager

class BottleApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Применяем сохраненный язык при запуске
        LanguageManager.applyLanguage(this)
    }
}