package ru.github.bottle.game

import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import ru.github.bottle.R
import ru.github.bottle.models.GameMode

class ThemeManager(
    private val rootView: ConstraintLayout,
    private val bottleView: ImageView
) {

    fun applyTheme(mode: GameMode, onThemeChanged: () -> Unit = {}) {
        val backgroundRes = try {
            when (mode) {
                GameMode.CHILDREN -> R.drawable.background_children
                GameMode.TEEN -> R.drawable.background_teen
                GameMode.ADULT -> R.drawable.background_adult
                GameMode.ADULT_PLUS -> R.drawable.background_adult_plus
                GameMode.SEX -> R.drawable.background_sex
            }
        } catch (_: Exception) {
            android.R.color.white
        }

        try {
            animateBackgroundChange(backgroundRes)
        } catch (_: Exception) {
            rootView.setBackgroundColor(ContextCompat.getColor(rootView.context, android.R.color.white))
        }

        val bottleRes = try {
            when (mode) {
                GameMode.CHILDREN -> R.drawable.ic_bottle_children
                GameMode.TEEN -> R.drawable.ic_bottle_teen
                GameMode.ADULT -> R.drawable.ic_bottle_adult
                GameMode.ADULT_PLUS -> R.drawable.ic_bottle_adult_plus
                GameMode.SEX -> R.drawable.ic_bottle_adult_plus
            }
        } catch (_: Exception) {
            R.drawable.ic_bottle_teen
        }

        try {
            animateBottleChange(bottleRes)
        } catch (_: Exception) {
            // Игнорируем
        }

        onThemeChanged()
    }

    private fun animateBackgroundChange(newBackgroundRes: Int) {
        rootView.animate()
            .alpha(0f)
            .setDuration(300)
            .withEndAction {
                rootView.setBackgroundResource(newBackgroundRes)
                rootView.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .start()
            }
            .start()
    }

    private fun animateBottleChange(newBottleRes: Int) {
        bottleView.animate()
            .scaleX(0f)
            .scaleY(0f)
            .setDuration(300)
            .withEndAction {
                bottleView.setImageResource(newBottleRes)
                bottleView.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(300)
                    .start()
            }
            .start()
    }
}