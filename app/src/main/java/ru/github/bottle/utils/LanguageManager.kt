package ru.github.bottle.utils

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.core.content.edit
import ru.github.bottle.R
import java.util.Locale

object LanguageManager {
    private const val PREFS_NAME = "app_prefs"
    private const val KEY_LANGUAGE = "selected_language"

    private val supportedLanguages = listOf(
        "ru",   // Русский
        "en",   // English
        "ja",   // 日本語
        "ko",   // 한국어
        "zh"    // 中文
    )

    fun getCurrentLanguage(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LANGUAGE, getDefaultLanguage()) ?: getDefaultLanguage()
    }

    /**
     * Сохраняет выбор языка. Применение локализации происходит при следующем
     * создании Activity через [wrap] (attachBaseContext), поэтому вызывающий код
     * должен пересоздать Activity (recreate).
     */
    fun setLanguage(context: Context, languageCode: String) {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putString(KEY_LANGUAGE, languageCode) }

        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        // Очищаем кэш заданий при смене языка
        TasksProvider.clearCache()
    }

    /**
     * Оборачивает контекст Activity с выбранной локалью. Вызывается из
     * Application.attachBaseContext, чтобы весь UI использовал выбранный язык,
     * а не системный.
     */
    fun wrap(context: Context): Context {
        val languageCode = getCurrentLanguage(context)
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLayoutDirection(locale)
        }
        return context.createConfigurationContext(config)
    }

    /** Применяет сохраненный язык к глобальной локали процесса. */
    fun applyLanguage(context: Context) {
        Locale.setDefault(Locale(getCurrentLanguage(context)))
    }

    private fun getDefaultLanguage(): String {
        return if (supportedLanguages.contains(Locale.getDefault().language)) {
            Locale.getDefault().language
        } else {
            "ru" // Русский по умолчанию
        }
    }

    fun getAvailableLanguages(context: Context): Map<String, String> {
        return mapOf(
            "ru" to context.getString(R.string.language_russian),
            "en" to context.getString(R.string.language_english),
            "ja" to context.getString(R.string.language_japanese),
            "ko" to context.getString(R.string.language_korean),
            "zh" to context.getString(R.string.language_chinese)
        )
    }
}
