package ru.github.bottle.utils

import android.util.Log
import java.util.Locale

object Logger {
    private const val TAG_PREFIX = "BottleApp"
    private const val LOG_ENABLED = true

    fun d(tag: String, message: String) {
        if (isLoggable()) {
            Log.d("$TAG_PREFIX/$tag", message)
        }
    }

    fun i(tag: String, message: String) {
        if (isLoggable()) {
            Log.i("$TAG_PREFIX/$tag", message)
        }
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        if (isLoggable()) {
            if (throwable != null) {
                Log.w("$TAG_PREFIX/$tag", message, throwable)
            } else {
                Log.w("$TAG_PREFIX/$tag", message)
            }
        }
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (isLoggable()) {
            if (throwable != null) {
                Log.e("$TAG_PREFIX/$tag", message, throwable)
            } else {
                Log.e("$TAG_PREFIX/$tag", message)
            }
        }
    }

    private fun isLoggable(): Boolean {
        // Отключаем логи в тестовом окружении
        try {
            // Проверяем, запущены ли тесты
            val isTest = System.getProperty("java.runtime.name")?.contains("Android") == false
                    || System.getProperty("org.gradle.testing") != null
                    || Thread.currentThread().stackTrace.any {
                it.className.contains("Test") || it.className.contains("JUnit")
            }
            if (isTest) {
                return false
            }
        } catch (_: Exception) {
            return false
        }
        return LOG_ENABLED
    }
}