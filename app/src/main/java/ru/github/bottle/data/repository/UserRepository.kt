package ru.github.bottle.data.repository

import android.content.Context
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.first
import ru.github.bottle.data.encryption.EncryptionManager
import ru.github.bottle.models.GameMode
import ru.github.bottle.models.User
import ru.github.bottle.utils.Logger
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "user_prefs"
)

class UserRepository(private val context: Context) {

    companion object {
        private const val TAG = "UserRepository"
        private val GSON = Gson()
        private val USERS_LIST_KEY = stringPreferencesKey("users_list")
        private val CURRENT_USER_KEY = stringPreferencesKey("current_user")
        private val MODE_KEY = stringPreferencesKey("game_mode")
        private val LOGGED_IN_KEY = stringPreferencesKey("is_logged_in")

        private const val PBKDF2_ITERATIONS = 120_000
        private const val PBKDF2_KEY_LENGTH = 256

        private fun passwordHashKey(username: String) =
            stringPreferencesKey("password_hash_$username")

        private fun passwordSaltKey(username: String) =
            stringPreferencesKey("password_salt_$username")

        private val USER_LIST_TYPE = object : TypeToken<List<User>>() {}.type
    }

    private val dataStore = context.dataStore

    /**
     * Атомарное сохранение пользователя: чтение-модификация-запись выполняются
     * внутри одного блока [edit], что исключает потерю данных при конкурентных вызовах.
     */
    suspend fun saveUser(user: User, password: String? = null) {
        dataStore.edit { preferences ->
            val users = deserializeUsers(preferences[USERS_LIST_KEY]).toMutableList()

            val existingIndex =
                users.indexOfFirst { it.username == user.username && !it.isGuest }
            if (existingIndex != -1) {
                users[existingIndex] = user
            } else {
                users.add(user)
            }

            // Шифрование обязательно: при ошибке не сохраняем данные в открытом виде
            val encrypted = EncryptionManager.getInstance(context)
                .encryptOrThrow(GSON.toJson(users))

            preferences[USERS_LIST_KEY] = encrypted
            preferences[CURRENT_USER_KEY] = user.username
            preferences[LOGGED_IN_KEY] = "true"

            if (password != null && !user.isGuest) {
                val salt = generateSalt()
                preferences[passwordHashKey(user.username)] = hashPassword(password, salt)
                preferences[passwordSaltKey(user.username)] = salt
            }
        }
    }

    private fun deserializeUsers(stored: String?): List<User> {
        if (stored == null) return emptyList()
        return try {
            val decrypted = EncryptionManager.getInstance(context).decrypt(stored)
            GSON.fromJson<List<User>>(decrypted, USER_LIST_TYPE) ?: emptyList()
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to read users list", e)
            emptyList()
        }
    }

    private suspend fun getUsersList(): List<User> =
        deserializeUsers(dataStore.data.first()[USERS_LIST_KEY])

    suspend fun getCurrentUser(): User? {
        val currentUsername = dataStore.data.first()[CURRENT_USER_KEY] ?: return null
        return getUsersList().find { it.username == currentUsername }
            // Возраст пересчитываем от даты рождения, а не берем застывшее значение
            ?.calculateAgeFromBirthDate()
    }

    suspend fun getUser(): User? = getCurrentUser()

    suspend fun findUserByUsername(username: String): User? =
        getUsersList().find { it.username == username && !it.isGuest }
            ?.calculateAgeFromBirthDate()

    suspend fun checkPassword(username: String, password: String): Boolean {
        val preferences = dataStore.data.first()
        val storedHash = preferences[passwordHashKey(username)]
        val storedSalt = preferences[passwordSaltKey(username)]

        if (storedHash == null || storedSalt == null) return false

        val computed = hashPassword(password, storedSalt)
        return constantTimeEquals(computed, storedHash)
    }

    private fun generateSalt(): String {
        val salt = ByteArray(32)
        SecureRandom().nextBytes(salt)
        return Base64.encodeToString(salt, Base64.NO_WRAP)
    }

    /** PBKDF2-HMAC-SHA256 с солью и большим числом итераций. */
    private fun hashPassword(password: String, salt: String): String {
        val spec = PBEKeySpec(
            password.toCharArray(),
            Base64.decode(salt, Base64.NO_WRAP),
            PBKDF2_ITERATIONS,
            PBKDF2_KEY_LENGTH
        )
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hash = factory.generateSecret(spec).encoded
        spec.clearPassword()
        return Base64.encodeToString(hash, Base64.NO_WRAP)
    }

    /** Сравнение без утечки времени. */
    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var diff = 0
        for (i in a.indices) {
            diff = diff or (a[i].code xor b[i].code)
        }
        return diff == 0
    }

    suspend fun isUsernameExists(username: String): Boolean =
        getUsersList().any { it.username == username && !it.isGuest }

    suspend fun isLoggedIn(): Boolean =
        dataStore.data.first()[LOGGED_IN_KEY]?.toBooleanStrictOrNull() ?: false

    suspend fun loginUser(user: User) {
        dataStore.edit { preferences ->
            preferences[CURRENT_USER_KEY] = user.username
            preferences[LOGGED_IN_KEY] = "true"
        }
    }

    suspend fun logout() {
        dataStore.edit { preferences ->
            preferences.remove(CURRENT_USER_KEY)
            preferences[LOGGED_IN_KEY] = "false"
        }
    }

    suspend fun setGameMode(mode: GameMode) {
        dataStore.edit { preferences ->
            preferences[MODE_KEY] = mode.name
        }
    }

    suspend fun getGameMode(): GameMode {
        val modeName = dataStore.data.first()[MODE_KEY] ?: GameMode.CHILDREN.name
        return try {
            GameMode.valueOf(modeName)
        } catch (_: IllegalArgumentException) {
            GameMode.CHILDREN
        }
    }

    suspend fun saveGuestUser() {
        val guest = User(
            id = System.currentTimeMillis().toString(),
            username = context.getString(ru.github.bottle.R.string.guest_username),
            age = 0,
            birthDate = 0,
            isGuest = true
        )
        saveUser(guest)
    }
}
