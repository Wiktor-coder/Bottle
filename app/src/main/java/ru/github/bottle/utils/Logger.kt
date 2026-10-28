package ru.github.bottle.utils

import android.util.Log

object Logger {
    private const val TAG_PREFIX = "BottleApp"

    // В release-сборке логи вырезаются ProGuard/R8 (см. proguard-rules.pro),
    // здесь дополнительно используем флаг BuildConfig.DEBUG.
    private var enabled: Boolean = true

    fun setEnabled(enable: Boolean) {
        enabled = enable
    }

    fun d(tag: String, message: String) {
        if (enabled) {
            Log.d("$TAG_PREFIX/$tag", message)
        }
    }

    fun i(tag: String, message: String) {
        if (enabled) {
            Log.i("$TAG_PREFIX/$tag", message)
        }
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        if (enabled) {
            if (throwable != null) {
                Log.w("$TAG_PREFIX/$tag", message, throwable)
            } else {
                Log.w("$TAG_PREFIX/$tag", message)
            }
        }
    }

    /** Ошибки логируем всегда, даже если debug-логи отключены. */
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e("$TAG_PREFIX/$tag", message, throwable)
        } else {
            Log.e("$TAG_PREFIX/$tag", message)
        }
    }
}
