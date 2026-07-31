package ru.github.bottle.models

import com.google.gson.annotations.SerializedName

enum class GameMode(
    val displayName: String,
    val minAge: Int  // Минимальный возраст для доступа
) {
    CHILDREN("Детский", 0),
    TEEN("Подростковый", 10),
    ADULT("Взрослый", 16),
    ADULT_PLUS("18+", 18)
}

data class User(
    @SerializedName("id")
    val id: String = "",

    @SerializedName("email")
    val email: String = "",

    @SerializedName("username")
    val username: String = "",

    @SerializedName("age")
    val age: Int = 0,

    @SerializedName("isGuest")
    val isGuest: Boolean = false,

    @SerializedName("createdAt")
    val createdAt: Long = System.currentTimeMillis()
) {
    fun canAccessMode(mode: GameMode): Boolean {
        // Гости могут играть ТОЛЬКО в детский режим
        if (isGuest) {
            return mode == GameMode.CHILDREN
        }
        // Для зарегистрированных пользователей проверяем возраст
        return age >= mode.minAge
    }

    fun getUnlockMessage(mode: GameMode): String {
        return when {
            isGuest -> "Гостевой режим: доступен только Детский режим"
            age < mode.minAge -> "Режим \"${mode.displayName}\" будет доступен в ${
                mode.minAge
            } лет"
            else -> "Режим доступен"
        }
    }
}
