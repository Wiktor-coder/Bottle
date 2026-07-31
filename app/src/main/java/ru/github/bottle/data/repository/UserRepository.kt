package ru.github.bottle.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import ru.github.bottle.models.GameMode
import ru.github.bottle.models.User
import ru.github.bottle.data.encryption.EncryptionManager

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "user_prefs"
)

class UserRepository(private val context: Context) {

    companion object {
        private val GSON = Gson()
        private val USER_KEY = stringPreferencesKey("user_data")
        private val MODE_KEY = stringPreferencesKey("game_mode")
        private val LOGGED_IN_KEY = stringPreferencesKey("is_logged_in")
    }

    private val dataStore = context.dataStore

    suspend fun saveUser(user: User) {
        val userJson = GSON.toJson(user)
        // Шифруем данные пользователя
        val encrypted = try {
            EncryptionManager.getInstance(context).encrypt(userJson)
        } catch (e: Exception) {
            userJson // Если шифрование не удалось, сохраняем как есть
        }

        dataStore.edit { preferences ->
            preferences[USER_KEY] = encrypted
            preferences[LOGGED_IN_KEY] = "true"
        }
    }

    suspend fun getUser(): User? {
        val preferences = dataStore.data.first()
        val userJson = preferences[USER_KEY]

        return if (userJson != null) {
            try {
                // Пробуем расшифровать
                val decrypted = try {
                    EncryptionManager.getInstance(context).decrypt(userJson)
                } catch (e: Exception) {
                    userJson // Если расшифровка не удалась, используем как есть
                }
                GSON.fromJson(decrypted, User::class.java)
            } catch (e: Exception) {
                null
            }
        } else null
    }

    suspend fun isLoggedIn(): Boolean {
        return dataStore.data.first()[LOGGED_IN_KEY]?.toBoolean() ?: false
    }

    suspend fun loginUser(user: User) {
        saveUser(user)
    }

    suspend fun logout() {
        dataStore.edit { preferences ->
            preferences.remove(USER_KEY)
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
        } catch (e: IllegalArgumentException) {
            GameMode.CHILDREN
        }
    }

    suspend fun saveGuestUser() {
        val guest = User(
            id = System.currentTimeMillis().toString(),
            email = "guest_${System.currentTimeMillis()}@temp.com",
            username = "Гость",
            age = 0,
            isGuest = true
        )
        saveUser(guest)
    }
}