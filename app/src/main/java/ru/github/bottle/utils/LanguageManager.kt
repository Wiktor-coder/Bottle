package ru.github.bottle.utils

import android.content.Context
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

    fun setLanguage(context: Context, languageCode: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putString(KEY_LANGUAGE, languageCode) }
        updateResources(context, languageCode)

        // Очищаем кэш заданий при смене языка
        TasksProvider.clearCache()
    }

    fun updateResources(context: Context, languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val resources = context.resources
        val config = resources.configuration

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale)
            config.setLayoutDirection(locale)
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
        }

        resources.updateConfiguration(config, resources.displayMetrics)
    }

    fun applyLanguage(context: Context) {
        val languageCode = getCurrentLanguage(context)
        updateResources(context, languageCode)
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