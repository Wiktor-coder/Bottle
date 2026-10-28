package ru.github.bottle.models

import android.content.Context
import com.google.gson.annotations.SerializedName
import ru.github.bottle.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class GameMode(
    val minAge: Int,
    val displayNameRes: Int
) {
    CHILDREN(0, R.string.game_mode_children),
    TEEN(10, R.string.game_mode_teen),
    ADULT(16, R.string.game_mode_adult),
    ADULT_PLUS(18, R.string.game_mode_adult_plus),
    SEX(18, R.string.game_mode_sex);

    fun displayName(context: Context): String = context.getString(displayNameRes)
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

    /**
     * Пересчитывает возраст от даты рождения. Без этого возраст «застывает»
     * на моменте регистрации и режимы разблокируются несвоевременно.
     */
    fun calculateAgeFromBirthDate(): User {
        if (birthDate == 0L || isGuest) {
            return this
        }

        val birth = Instant.ofEpochMilli(birthDate)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        val today = LocalDate.now()

        // Корректный расчет: учитываем месяц и день, а не DAY_OF_YEAR
        var newAge = today.year - birth.year
        if (today.monthValue < birth.monthValue ||
            (today.monthValue == birth.monthValue && today.dayOfMonth < birth.dayOfMonth)
        ) {
            newAge--
        }

        return this.copy(age = newAge.coerceAtLeast(0))
    }

    fun canAccessMode(mode: GameMode): Boolean {
        if (isGuest) {
            return mode == GameMode.CHILDREN
        }
        return age >= mode.minAge
    }

    /** Локализованное сообщение о недоступности режима. */
    fun getUnlockMessage(context: Context, mode: GameMode): String {
        return when {
            isGuest -> context.getString(R.string.game_guest_mode_locked)
            age < mode.minAge -> context.getString(
                R.string.game_mode_unlock_message,
                mode.displayName(context),
                mode.minAge
            )
            else -> context.getString(R.string.game_mode_available)
        }
    }
}
