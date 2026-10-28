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

    fun applyTheme(mode: GameMode, animate: Boolean = true) {
        val backgroundRes = when (mode) {
            GameMode.CHILDREN -> R.drawable.background_children
            GameMode.TEEN -> R.drawable.background_teen
            GameMode.ADULT -> R.drawable.background_adult
            GameMode.ADULT_PLUS -> R.drawable.background_adult_plus
            GameMode.SEX -> R.drawable.background_sex
        }

        val bottleRes = when (mode) {
            GameMode.CHILDREN -> R.drawable.ic_bottle_children
            GameMode.TEEN -> R.drawable.ic_bottle_teen
            GameMode.ADULT -> R.drawable.ic_bottle_adult
            GameMode.ADULT_PLUS -> R.drawable.ic_bottle_adult_plus
            GameMode.SEX -> R.drawable.ic_bottle_adult_plus
        }

        if (animate) {
            animateBackgroundChange(backgroundRes)
            animateBottleChange(bottleRes)
        } else {
            cancelAnimations()
            rootView.setBackgroundResource(backgroundRes)
            rootView.alpha = 1f
            bottleView.setImageResource(bottleRes)
            bottleView.scaleX = 1f
            bottleView.scaleY = 1f
        }
    }

    /** Отменяет незавершенные анимации (вызывается из Activity в onDestroy). */
    fun cancelAnimations() {
        rootView.animate().cancel()
        bottleView.animate().cancel()
    }

    private fun animateBackgroundChange(newBackgroundRes: Int) {
        // Отменяем предыдущую анимацию, иначе параллельные прогоны
        // оставляют view с alpha=0 или в промежуточном состоянии
        rootView.animate().cancel()
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
        bottleView.animate().cancel()
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