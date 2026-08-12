package ru.github.bottle.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.first
import ru.github.bottle.models.GameMode
import ru.github.bottle.models.User
import ru.github.bottle.data.encryption.EncryptionManager
import java.security.MessageDigest
import java.security.SecureRandom
import android.util.Base64

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "user_prefs"
)

class UserRepository(private val context: Context) {

    companion object {
        private val GSON = Gson()
        private val USERS_LIST_KEY = stringPreferencesKey("users_list")
        private val CURRENT_USER_KEY = stringPreferencesKey("current_user")
        private val MODE_KEY = stringPreferencesKey("game_mode")
        private val LOGGED_IN_KEY = stringPreferencesKey("is_logged_in")
    }

    private val dataStore = context.dataStore

    suspend fun saveUser(user: User, password: String? = null) {
        val users = getUsersList().toMutableList()

        val existingIndex = users.indexOfFirst { it.username == user.username && !it.isGuest }
        if (existingIndex != -1) {
            users[existingIndex] = user
        } else {
            users.add(user)
        }

        val usersJson = GSON.toJson(users)
        val encrypted = try {
            EncryptionManager.getInstance(context).encrypt(usersJson)
        } catch (_: Exception) {
            usersJson
        }

        dataStore.edit { preferences ->
            preferences[USERS_LIST_KEY] = encrypted
            preferences[CURRENT_USER_KEY] = user.username
            preferences[LOGGED_IN_KEY] = "true"

            if (password != null && !user.isGuest) {
                val salt = generateSalt()
                val hash = hashPassword(password, salt)
                val hashKey = stringPreferencesKey("password_hash_${user.username}")
                val saltKey = stringPreferencesKey("password_salt_${user.username}")
                preferences[hashKey] = hash
                preferences[saltKey] = salt
            }
        }
    }

    private suspend fun getUsersList(): List<User> {
        val preferences = dataStore.data.first()
        val usersJson = preferences[USERS_LIST_KEY]

        return if (usersJson != null) {
            try {
                val decrypted = try {
                    EncryptionManager.getInstance(context).decrypt(usersJson)
                } catch (_: Exception) {
                    usersJson
                }
                val type = object : TypeToken<List<User>>() {}.type
                GSON.fromJson(decrypted, type)
            } catch (_: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    suspend fun getCurrentUser(): User? {
        val preferences = dataStore.data.first()
        val currentUsername = preferences[CURRENT_USER_KEY]

        if (currentUsername == null) return null

        val users = getUsersList()
        return users.find { it.username == currentUsername }
    }

    suspend fun getUser(): User? {
        return getCurrentUser()
    }

    suspend fun findUserByUsername(username: String): User? {
        val users = getUsersList()
        return users.find { it.username == username && !it.isGuest }
    }

    suspend fun checkPassword(username: String, password: String): Boolean {
        val preferences = dataStore.data.first()
        val hashKey = stringPreferencesKey("password_hash_$username")
        val saltKey = stringPreferencesKey("password_salt_$username")

        val storedHash = preferences[hashKey]
        val storedSalt = preferences[saltKey]

        if (storedHash == null || storedSalt == null) {
            return false
        }

        val hash = hashPassword(password, storedSalt)
        return hash == storedHash
    }

    private fun generateSalt(): String {
        val salt = ByteArray(32)
        SecureRandom().nextBytes(salt)
        return Base64.encodeToString(salt, Base64.NO_WRAP)
    }

    private fun hashPassword(password: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val saltedPassword = password + salt
        val hash = digest.digest(saltedPassword.toByteArray())
        return Base64.encodeToString(hash, Base64.NO_WRAP)
    }

    suspend fun isUsernameExists(username: String): Boolean {
        val users = getUsersList()
        return users.any { it.username == username && !it.isGuest }
    }

    suspend fun isLoggedIn(): Boolean {
        return dataStore.data.first()[LOGGED_IN_KEY]?.toBoolean() ?: false
    }

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
            username = "Гость",
            age = 0,
            birthDate = 0,
            isGuest = true
        )
        saveUser(guest)
    }
}