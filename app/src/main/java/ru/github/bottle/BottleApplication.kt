package ru.github.bottle

import android.app.Application
import android.content.Context
import ru.github.bottle.utils.LanguageManager
import ru.github.bottle.utils.Logger

class BottleApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Применяем сохраненный язык при запуске
        LanguageManager.applyLanguage(this)
        // Debug-логи только в отладочных сборках
        Logger.setEnabled(BuildConfig.DEBUG)
    }

    /**
     * Применяет выбранный язык ко всем Activity. Без этого Activity могут
     * пересоздать конфигурацию из системного языка и показать смешанный UI.
     */
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LanguageManager.wrap(base))
    }
}