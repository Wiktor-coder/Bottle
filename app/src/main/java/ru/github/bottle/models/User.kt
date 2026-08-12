package ru.github.bottle.models

import com.google.gson.annotations.SerializedName
import java.util.Calendar

enum class GameMode(
    val displayName: String,
    val minAge: Int
) {
    CHILDREN("Детский", 0),
    TEEN("Подростковый", 10),
    ADULT("Взрослый", 16),
    ADULT_PLUS("18+", 18),
    SEX("Секс", 18)
}

data class User(
    @SerializedName("id")
    val id: String = "",

    @SerializedName("username")
    val username: String = "",

    @SerializedName("age")
    val age: Int = 0,

    @SerializedName("birthDate")
    val birthDate: Long = 0,

    @SerializedName("isGuest")
    val isGuest: Boolean = false,

    @SerializedName("createdAt")
    val createdAt: Long = System.currentTimeMillis()
) {

    fun calculateAge(): User {
        if (birthDate == 0L || isGuest) {
            return this
        }

        val birthCalendar = Calendar.getInstance().apply {
            timeInMillis = birthDate
        }
        val currentCalendar = Calendar.getInstance()

        var newAge = currentCalendar.get(Calendar.YEAR) - birthCalendar.get(Calendar.YEAR)

        if (currentCalendar.get(Calendar.DAY_OF_YEAR) < birthCalendar.get(Calendar.DAY_OF_YEAR)) {
            newAge--
        }

        return this.copy(age = newAge)
    }

    fun canAccessMode(mode: GameMode): Boolean {
        if (isGuest) {
            return mode == GameMode.CHILDREN
        }
        return age >= mode.minAge
    }

    fun getUnlockMessage(mode: GameMode): String {
        return when {
            isGuest -> "Гостевой режим: доступен только Детский режим"
            age < mode.minAge -> "Режим \"${mode.displayName}\" будет доступен в ${mode.minAge} лет"
            else -> "Режим доступен"
        }
    }
}
