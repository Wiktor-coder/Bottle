package ru.github.bottle.game

import android.animation.ObjectAnimator
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.ImageView
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.content.getSystemService
import kotlin.random.Random

class BottleAnimator(
    private val bottleView: ImageView,
    private val context: Context
) {
    private var currentRotation = 0f
    private var isSpinning = false
    private var onSpinEnd: (() -> Unit)? = null
    private var currentAnimator: ObjectAnimator? = null

    fun setOnSpinEndListener(listener: () -> Unit) {
        onSpinEnd = listener
    }

    fun spin() {
        if (isSpinning) return
        isSpinning = true

        val randomRotation = Random.nextFloat() * 720 + 360
        val finalRotation = currentRotation + randomRotation

        // Отменяем предыдущий аниматор, если он вдруг еще активен
        currentAnimator?.cancel()

        val animator = ObjectAnimator.ofFloat(bottleView, "rotation", currentRotation, finalRotation)
        animator.duration = 2000
        animator.interpolator = AccelerateDecelerateInterpolator()
        currentAnimator = animator

        animator.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: android.animation.Animator) {
                vibrate(100)
            }

            override fun onAnimationEnd(animation: android.animation.Animator) {
                if (animation !== currentAnimator) return
                currentRotation = bottleView.rotation
                isSpinning = false
                currentAnimator = null
                onSpinEnd?.invoke()
            }

            override fun onAnimationCancel(animation: android.animation.Animator) {
                if (animation !== currentAnimator) return
                currentRotation = bottleView.rotation
                isSpinning = false
                currentAnimator = null
            }
        })

        animator.start()
    }

    /** Освобождение ресурсов (вызывать из Activity.onDestroy). */
    fun release() {
        currentAnimator?.cancel()
        currentAnimator = null
        onSpinEnd = null
        isSpinning = false
    }

    private fun vibrate(duration: Long) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService<VibratorManager>()
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService<Vibrator>()
        }

        vibrator?.let {
            if (it.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    it.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    it.vibrate(duration)
                }
            }
        }
    }

    fun setCurrentRotation(rotation: Float) {
        currentRotation = rotation
        bottleView.rotation = rotation
    }

}