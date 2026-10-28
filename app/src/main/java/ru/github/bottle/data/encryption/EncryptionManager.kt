package ru.github.bottle.data.encryption

import android.content.Context
import android.util.Log
import com.google.crypto.tink.Aead
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.aead.AeadKeyTemplates
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.charset.StandardCharsets
import java.security.GeneralSecurityException

class EncryptionManager private constructor(private val context: Context) {

    companion object {
        private const val KEYSTORE_NAME = "bottle_keystore"
        private const val PREF_NAME = "bottle_keyset"
        private const val TAG = "EncryptionManager"
        private const val ASSOCIATED_DATA = "bottle_app_data"

        @Volatile
        private var instance: EncryptionManager? = null

        fun getInstance(context: Context): EncryptionManager {
            return instance ?: synchronized(this) {
                instance ?: EncryptionManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }

    private val aead: Aead by lazy {
        try {
            AeadConfig.register()

            val keysetManager = AndroidKeysetManager.Builder()
                .withKeyTemplate(AeadKeyTemplates.AES256_GCM)
                .withMasterKeyUri("android-keystore://$KEYSTORE_NAME")
                .withSharedPref(context, PREF_NAME, KEYSTORE_NAME)
                .build()

            keysetManager.keysetHandle.getPrimitive(Aead::class.java)
        } catch (e: GeneralSecurityException) {
            Log.e(TAG, "Failed to initialize encryption", e)
            throw RuntimeException("Failed to initialize encryption", e)
        }
    }

    /**
     * Шифрует данные. Бросает исключение при ошибке — вызывающий код
     * обязан обработать его и НЕ сохранять данные в открытом виде (fail-closed).
     */
    suspend fun encryptOrThrow(data: String): String = withContext(Dispatchers.IO) {
        try {
            val plaintext = data.toByteArray(StandardCharsets.UTF_8)
            val associatedData = ASSOCIATED_DATA.toByteArray(StandardCharsets.UTF_8)
            val ciphertext = aead.encrypt(plaintext, associatedData)
            android.util.Base64.encodeToString(ciphertext, android.util.Base64.NO_WRAP)
        } catch (e: GeneralSecurityException) {
            Log.e(TAG, "Encryption failed", e)
            throw EncryptionException("Encryption failed", e)
        }
    }

    /**
     * Расшифровывает данные. Если данные не являются корректным шифротекстом
     * (например, унаследованы от старых версий приложения), возвращает их как есть.
     * Ошибки инициализации ключей пробрасываются наверх.
     */
    suspend fun decrypt(encryptedData: String): String = withContext(Dispatchers.IO) {
        try {
            val ciphertext = android.util.Base64.decode(encryptedData, android.util.Base64.NO_WRAP)
            val associatedData = ASSOCIATED_DATA.toByteArray(StandardCharsets.UTF_8)
            val plaintext = aead.decrypt(ciphertext, associatedData)
            String(plaintext, StandardCharsets.UTF_8)
        } catch (e: GeneralSecurityException) {
            Log.w(TAG, "Decryption failed, treating data as legacy plaintext", e)
            encryptedData
        }
    }

    class EncryptionException(message: String, cause: Throwable?) : Exception(message, cause)
}