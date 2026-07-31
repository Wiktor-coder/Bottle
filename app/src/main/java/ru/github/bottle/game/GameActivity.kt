package ru.github.bottle.game

import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.Vibrator
import android.util.Log
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import ru.github.bottle.R
import ru.github.bottle.auth.LoginActivity
import ru.github.bottle.databinding.ActivityGameBinding
import ru.github.bottle.databinding.BottomSheetTaskBinding
import ru.github.bottle.databinding.DialogSettingsBinding
import ru.github.bottle.data.repository.UserRepository
import ru.github.bottle.models.GameMode
import ru.github.bottle.models.User
import ru.github.bottle.utils.TasksProvider
import kotlin.random.Random



    private const val TAG = "GameActivity"

class GameActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGameBinding
    private lateinit var userRepository: UserRepository
    private var currentMode: GameMode = GameMode.CHILDREN
    private var currentRotation = 0f
    private var isSpinning = false
    private var bottomSheetDialog: BottomSheetDialog? = null
    private var bottomSheetBinding: BottomSheetTaskBinding? = null
    private var tasksCompleted = 0
    private var settingsDialog: AlertDialog? = null
    private var currentUser: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Восстанавливаем состояние
        savedInstanceState?.let {
            currentMode = it.getSerializable(KEY_CURRENT_MODE) as? GameMode ?: GameMode.CHILDREN
            currentRotation = it.getFloat(KEY_CURRENT_ROTATION)
            tasksCompleted = it.getInt(KEY_TASKS_COMPLETED)
            binding.ivBottle.rotation = currentRotation
        }

        userRepository = UserRepository(this)

        lifecycleScope.launch {
            checkUserAndSetup()
        }

        setupListeners()

        // Показываем приветственное сообщение вместо задания
        showWelcomeMessage()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Сохраняем состояние
        outState.putSerializable(KEY_CURRENT_MODE, currentMode)
        outState.putFloat(KEY_CURRENT_ROTATION, currentRotation)
        outState.putInt(KEY_TASKS_COMPLETED, tasksCompleted)
        Log.d(TAG, "Состояние сохранено: режим=${currentMode.displayName}, вращение=$currentRotation, заданий=$tasksCompleted")
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        // Восстанавливаем состояние
        currentMode = savedInstanceState.getSerializable(KEY_CURRENT_MODE) as? GameMode ?: GameMode.CHILDREN
        currentRotation = savedInstanceState.getFloat(KEY_CURRENT_ROTATION)
        tasksCompleted = savedInstanceState.getInt(KEY_TASKS_COMPLETED)
        binding.ivBottle.rotation = currentRotation
        Log.d(TAG, "Состояние восстановлено: режим=${currentMode.displayName}, вращение=$currentRotation, заданий=$tasksCompleted")
    }

    // Обработка изменения конфигурации (поворот экрана)
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // При повороте экрана ничего не перезагружаем
        // Все данные уже сохранены в переменных
        Log.d(TAG, "Configuration changed: orientation=${newConfig.orientation}")
    }

    private suspend fun checkUserAndSetup() {
        val user = userRepository.getUser()

        if (user == null) {
            startActivity(LoginActivity::class.java)
            finish()
            return
        }

        currentUser = user

        Log.d(TAG, "Пользователь: ${user.username}, Возраст: ${user.age}, Гость: ${user.isGuest}")

        // Если режим не был восстановлен из savedInstanceState, берем из репозитория
        if (currentMode == GameMode.CHILDREN) {
            // Проверяем, есть ли сохраненный режим
            val savedMode = userRepository.getGameMode()
            if (user.canAccessMode(savedMode)) {
                currentMode = savedMode
            } else {
                currentMode = when {
                    user.age >= 18 -> GameMode.ADULT_PLUS
                    user.age >= 16 -> GameMode.ADULT
                    user.age >= 10 -> GameMode.TEEN
                    else -> GameMode.CHILDREN
                }
                userRepository.setGameMode(currentMode)
            }
        }

        // Устанавливаем режим по умолчанию
        if (user.isGuest) {
            currentMode = GameMode.CHILDREN
            userRepository.setGameMode(GameMode.CHILDREN)
            Toast.makeText(this, "Гостевой режим: доступен только детский режим", Toast.LENGTH_LONG).show()
        } else {
            // Проверяем доступные режимы в зависимости от возраста
            val savedMode = userRepository.getGameMode()

            Log.d(TAG, "Сохраненный режим: ${savedMode.displayName}")

            // Проверяем, доступен ли сохраненный режим
            if (user.canAccessMode(savedMode)) {
                currentMode = savedMode
            } else {
                // Если сохраненный режим недоступен, выбираем максимально доступный
                currentMode = when {
                    user.age >= 18 -> GameMode.ADULT_PLUS
                    user.age >= 16 -> GameMode.ADULT
                    user.age >= 10 -> GameMode.TEEN
                    else -> GameMode.CHILDREN
                }
                userRepository.setGameMode(currentMode)

                Log.d(TAG, "Режим изменен на: ${currentMode.displayName}")

                Toast.makeText(
                    this,
                    "Режим изменен на ${currentMode.displayName} (вам ${user.age} лет)",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        updateButtonsAvailability()
        updateModeUI()
        updateButtonsState()
        applyTheme(currentMode) // Применяем тему при загрузке
        loadStats()
    }

     // Применяет тему в зависимости от выбранного режима
    private fun applyTheme(mode: GameMode) {
        // 1. Меняем фоновое изображение
        val backgroundRes = try {
            when (mode) {
                GameMode.CHILDREN -> R.drawable.background_children
                GameMode.TEEN -> R.drawable.background_teen
                GameMode.ADULT -> R.drawable.background_adult
                GameMode.ADULT_PLUS -> R.drawable.background_adult_plus
            }
        } catch (e: Exception) {
            android.R.color.white
        }


        // Применяем фон с плавной анимацией
         try {
             animateBackgroundChange(backgroundRes)
         } catch (e: Exception) {
             // Если анимация не работает, просто ставим цвет
             binding.root.setBackgroundColor(ContextCompat.getColor(this, android.R.color.white))
         }

        // 2. Меняем иконку бутылки
        val bottleRes = try {
            when (mode) {
                GameMode.CHILDREN -> R.drawable.ic_bottle_children
                GameMode.TEEN -> R.drawable.ic_bottle_teen
                GameMode.ADULT -> R.drawable.ic_bottle_adult
                GameMode.ADULT_PLUS -> R.drawable.ic_bottle_adult_plus
            }
        } catch (e: Exception) {
            R.drawable.ic_bottle_teen
        }

        // Применяем иконку с анимацией
         try {
             animateBottleChange(bottleRes)
         } catch (e: Exception) {
             // Если иконки нет, оставляем текущую
         }
    }

    // Плавная смена фонового изображения
    private fun animateBackgroundChange(newBackgroundRes: Int) {
        // Затухание текущего фона
        binding.root.animate()
            .alpha(0f)
            .setDuration(300)
            .withEndAction {
                // Смена фона
                binding.root.setBackgroundResource(newBackgroundRes)
                // Появление нового фона
                binding.root.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .start()
            }
            .start()
    }

    // Плавная смена иконки бутылки
    private fun animateBottleChange(newBottleRes: Int) {
        // Уменьшение иконки
        binding.ivBottle.animate()
            .scaleX(0f)
            .scaleY(0f)
            .setDuration(300)
            .withEndAction {
                // Смена иконки
                binding.ivBottle.setImageResource(newBottleRes)
                // Увеличение иконки
                binding.ivBottle.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(300)
                    .start()
            }
            .start()
    }

    private fun updateButtonsAvailability() {
        val user = currentUser ?: return

        Log.d(TAG, "Обновление доступности кнопок. Возраст: ${user.age}, Гость: ${user.isGuest}")

        // Детский режим доступен всем
        binding.btnModeChildren.isEnabled = true

        // Подростковый режим доступен с 10 лет
        val teenAvailable = user.canAccessMode(GameMode.TEEN)
        binding.btnModeTeen.isEnabled = user.canAccessMode(GameMode.TEEN)
        Log.d(TAG, "Подростковый режим доступен: $teenAvailable")

        // Взрослый режим доступен с 16 лет
        val adultAvailable = user.canAccessMode(GameMode.ADULT)
        binding.btnModeAdult.isEnabled = user.canAccessMode(GameMode.ADULT)
        Log.d(TAG, "Взрослый режим доступен: $adultAvailable")

        // Режим 18+ доступен с 18 лет
        val adultPlusAvailable = user.canAccessMode(GameMode.ADULT_PLUS)
        binding.btnModeAdultPlus.isEnabled = user.canAccessMode(GameMode.ADULT_PLUS)
        Log.d(TAG, "Режим 18+ доступен: $adultPlusAvailable")

        // Для гостей блокируем все кроме детского
        if (user.isGuest) {
            binding.btnModeTeen.isEnabled = false
            binding.btnModeAdult.isEnabled = false
            binding.btnModeAdultPlus.isEnabled = false
            Log.d(TAG, "Гостевой режим: все кроме детского заблокированы")
        }
    }

    private fun setupListeners() {
        // Клик по бутылке
        binding.ivBottle.setOnClickListener {
            spinBottle()
        }

        // Обработка выбора режима
        binding.btnGroupMode.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val newMode = when (checkedId) {
                    R.id.btnModeChildren -> GameMode.CHILDREN
                    R.id.btnModeTeen -> GameMode.TEEN
                    R.id.btnModeAdult -> GameMode.ADULT
                    R.id.btnModeAdultPlus -> GameMode.ADULT_PLUS
                    else -> return@addOnButtonCheckedListener
                }

                lifecycleScope.launch {
                    val user = userRepository.getUser()

                    Log.d(TAG, "Попытка переключения на режим: ${newMode.displayName}")
                    if (user != null) {
                        Log.d(TAG, "Текущий пользователь: ${user.username}, Возраст: ${user.age}, Гость: ${user.isGuest}")
                        Log.d(TAG, "canAccessMode(${newMode.displayName}) = ${user.canAccessMode(newMode)}")
                    }

                    if (user != null && user.canAccessMode(newMode)) {
                        currentMode = newMode
                        userRepository.setGameMode(currentMode)
                        updateModeUI()
                        applyTheme(currentMode) // Применяем тему при смене режима
                        Toast.makeText(
                            this@GameActivity,
                            "Режим: ${newMode.displayName}",
                            Toast.LENGTH_SHORT
                        ).show()
                        Log.d(TAG, "Режим успешно переключен на: ${newMode.displayName}")
                    } else {
                        val message = if (user != null) {
                            user.getUnlockMessage(newMode)
                        } else {
                            "Режим недоступен"
                        }
                        Toast.makeText(
                            this@GameActivity,
                            message,
                            Toast.LENGTH_LONG
                        ).show()
                        Log.d(TAG, "Переключение отклонено: $message")
                        updateButtonsState()
                    }
                }
            }
        }

        // Настройки
        binding.btnSettings.setOnClickListener {
            showSettingsDialog()
        }
    }

    private fun showWelcomeMessage() {
        val user = currentUser
        val message = if (user == null) {
            "Нажмите на бутылку, чтобы начать!"
        } else if (user.isGuest) {
            "Гостевой режим\nНажмите на бутылку, чтобы получить задание!"
        } else {
            "Добро пожаловать, ${user.username}!\nВаш возраст: ${user.age} лет\nНажмите на бутылку, чтобы начать!"
        }
        showTaskInBottomSheet(message, true)
    }

    private fun showSettingsDialog() {
        settingsDialog?.dismiss()

        val dialogBinding = DialogSettingsBinding.inflate(layoutInflater)

        lifecycleScope.launch {
            val user = userRepository.getUser()
            val userInfo = if (user?.isGuest == true) {
                "Гость"
            } else {
                "${user?.username} (${user?.age} лет)"
            }
            dialogBinding.tvUserInfo.text = "Пользователь: $userInfo"
            dialogBinding.tvUserMode.text = "Режим: ${currentMode.displayName}"
            dialogBinding.tvTasksCount.text = tasksCompleted.toString()
        }

        settingsDialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setCancelable(true)
            .create()

        settingsDialog?.show()

        dialogBinding.btnClearStats.setOnClickListener {
            tasksCompleted = 0
            dialogBinding.tvTasksCount.text = "0"
            Toast.makeText(this@GameActivity, "Статистика сброшена", Toast.LENGTH_SHORT).show()
            saveStats()
        }

        dialogBinding.btnResetMode.setOnClickListener {
            lifecycleScope.launch {
                currentMode = GameMode.CHILDREN
                userRepository.setGameMode(GameMode.CHILDREN)
                updateModeUI()
                updateButtonsState()
                Toast.makeText(
                    this@GameActivity,
                    "Режим сброшен на Детский",
                    Toast.LENGTH_SHORT
                ).show()
                settingsDialog?.dismiss()
            }
        }

        dialogBinding.btnLogout.setOnClickListener {
            lifecycleScope.launch {
                userRepository.logout()
                startActivity(LoginActivity::class.java)
                finish()
            }
        }
    }

    private fun spinBottle() {
        if (isSpinning) return
        isSpinning = true

        val randomRotation = Random.nextFloat() * 720 + 360
        val finalRotation = currentRotation + randomRotation

        val animator = ObjectAnimator.ofFloat(binding.ivBottle, "rotation", currentRotation, finalRotation)
        animator.duration = 2000
        animator.interpolator = AccelerateDecelerateInterpolator()

        animator.addListener(object : android.animation.Animator.AnimatorListener {
            override fun onAnimationStart(animation: android.animation.Animator) {
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (vibrator.hasVibrator()) {
                    vibrator.vibrate(100)
                }
                bottomSheetDialog?.dismiss()
            }

            override fun onAnimationEnd(animation: android.animation.Animator) {
                currentRotation = finalRotation
                isSpinning = false
                showRandomTask()
                tasksCompleted++
                saveStats()
            }

            override fun onAnimationCancel(animation: android.animation.Animator) {
                isSpinning = false
            }

            override fun onAnimationRepeat(animation: android.animation.Animator) {}
        })

        animator.start()
    }

    private fun showRandomTask() {
        val task = TasksProvider.getRandomTask(currentMode)
        showTaskInBottomSheet(task, false)
    }

    private fun showTaskInBottomSheet(task: String, isWelcome: Boolean = false) {
        bottomSheetDialog?.dismiss()

        bottomSheetBinding = BottomSheetTaskBinding.inflate(layoutInflater)
        bottomSheetDialog = BottomSheetDialog(this, R.style.BottomSheetDialogTheme)
        bottomSheetDialog?.setContentView(bottomSheetBinding!!.root)

        bottomSheetBinding?.tvTask?.text = task

        // Если это приветственное сообщение, меняем заголовок
        if (isWelcome) {
            bottomSheetBinding?.btnClose?.text = "Начать игру!"
        } else {
            bottomSheetBinding?.btnClose?.text = "Понятно!"
        }

        val bottomSheet = bottomSheetDialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        bottomSheet?.let {
            val behavior = BottomSheetBehavior.from(it)
            behavior.peekHeight = 200
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.isHideable = false
        }

        bottomSheetBinding?.btnClose?.setOnClickListener {
            bottomSheetDialog?.dismiss()
        }

        bottomSheetDialog?.show()
    }

    private fun updateModeUI() {
        applyTheme(currentMode) // вызываем applyTheme при обновлении UI
        updateButtonsState()
    }

    private fun updateButtonsState() {
        binding.btnModeChildren.isChecked = currentMode == GameMode.CHILDREN
        binding.btnModeTeen.isChecked = currentMode == GameMode.TEEN
        binding.btnModeAdult.isChecked = currentMode == GameMode.ADULT
        binding.btnModeAdultPlus.isChecked = currentMode == GameMode.ADULT_PLUS

        updateButtonStyle(binding.btnModeChildren, currentMode == GameMode.CHILDREN)
        updateButtonStyle(binding.btnModeTeen, currentMode == GameMode.TEEN)
        updateButtonStyle(binding.btnModeAdult, currentMode == GameMode.ADULT)
        updateButtonStyle(binding.btnModeAdultPlus, currentMode == GameMode.ADULT_PLUS)
    }

    private fun updateButtonStyle(button: MaterialButton, isSelected: Boolean) {
        val primaryColor = ContextCompat.getColor(this, R.color.primary)
        val whiteColor = ContextCompat.getColor(this, android.R.color.white)
        val transparentColor = ContextCompat.getColor(this, android.R.color.transparent)

        if (isSelected) {
            button.setBackgroundColor(primaryColor)
            button.setTextColor(whiteColor)
            button.strokeWidth = 0
        } else {
            button.setBackgroundColor(transparentColor)
            button.setTextColor(primaryColor)
            button.strokeWidth = 2
            button.strokeColor = ContextCompat.getColorStateList(this, R.color.primary)
        }
    }

    private fun loadStats() {
        lifecycleScope.launch {
            //  можно загрузить сохраненную статистику
        }
    }

    private fun saveStats() {
        lifecycleScope.launch {
            //  можно сохранить статистику
        }
    }

    private fun startActivity(activityClass: Class<*>) {
        startActivity(Intent(this, activityClass))
    }

    override fun onDestroy() {
        super.onDestroy()
        bottomSheetDialog?.dismiss()
        settingsDialog?.dismiss()
    }

    companion object {
        private const val KEY_CURRENT_MODE = "current_mode"
        private const val KEY_CURRENT_ROTATION = "current_rotation"
        private const val KEY_TASKS_COMPLETED = "tasks_completed"
        fun getIntent(context: Context): Intent {
            return Intent(context, GameActivity::class.java)
        }
    }
}