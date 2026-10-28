package ru.github.bottle.game

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.core.os.BundleCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.gson.Gson
import kotlinx.coroutines.launch
import ru.github.bottle.BuildConfig
import ru.github.bottle.R
import ru.github.bottle.auth.LoginActivity
import ru.github.bottle.data.repository.UserRepository
import ru.github.bottle.databinding.ActivityGameBinding
import ru.github.bottle.databinding.BottomSheetTaskBinding
import ru.github.bottle.databinding.DialogSettingsBinding
import ru.github.bottle.models.GameMode
import ru.github.bottle.models.User
import ru.github.bottle.utils.LanguageManager
import ru.github.bottle.utils.Logger
import ru.github.bottle.utils.Task
import ru.github.bottle.utils.TasksProvider

class GameActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGameBinding
    private lateinit var userRepository: UserRepository
    private lateinit var themeManager: ThemeManager
    private lateinit var bottleAnimator: BottleAnimator

    private var currentMode: GameMode = GameMode.CHILDREN
    private var currentRotation = 0f
    private var bottomSheetDialog: BottomSheetDialog? = null
    private var bottomSheetBinding: BottomSheetTaskBinding? = null
    private var tasksCompleted = 0
    private var settingsDialog: AlertDialog? = null
    private var currentUser: User? = null

    // Флаг для OnGlobalLayoutListener: пересчитываем паддинги только при реальном
    // изменении, иначе setPadding -> layout -> listener зацикливается
    private var lastCenteredKey: String? = null

    companion object {
        private const val KEY_CURRENT_MODE = "current_mode"
        private const val KEY_CURRENT_ROTATION = "current_rotation"
        private const val KEY_TASKS_COMPLETED = "tasks_completed"
        private const val PREFS_NAME = "game_stats"
        private const val TAG = "GameActivity"
        private const val GSON_PREFS = "task_progress"

        fun getIntent(context: Context): Intent {
            return Intent(context, GameActivity::class.java)
        }

        private fun statsKey(mode: GameMode) = "tasks_completed_${mode.name}"
        private fun usedTasksKey(mode: GameMode) = "used_tasks_${mode.name}"
    }

    override fun attachBaseContext(newBase: Context) {
        // Язык приложения применяется ко всем Activity (иначе recreate()
        // показывает смешанную локализацию)
        super.attachBaseContext(LanguageManager.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Инициализация менеджеров
        themeManager = ThemeManager(binding.root, binding.ivBottle)
        bottleAnimator = BottleAnimator(binding.ivBottle, this)

        restoreState(savedInstanceState)

        userRepository = UserRepository(this)

        setupListeners()

        lifecycleScope.launch {
            checkUserAndSetup()
        }

        // Центрируем кнопки после загрузки
        binding.horizontalScrollView.post {
            centerModeButtons()
        }
    }

    private fun restoreState(savedInstanceState: Bundle?) {
        savedInstanceState?.let { bundle ->
            BundleCompat.getSerializable(bundle, KEY_CURRENT_MODE, GameMode::class.java)
                ?.let { currentMode = it }
            currentRotation = bundle.getFloat(KEY_CURRENT_ROTATION, 0f)
            binding.ivBottle.rotation = currentRotation
            bottleAnimator.setCurrentRotation(currentRotation)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        lastCenteredKey = null
        binding.horizontalScrollView.post {
            centerModeButtons()
        }
    }

    private fun centerModeButtons() {
        val scrollView = binding.horizontalScrollView
        val buttonsContainer = scrollView.getChildAt(0) as? LinearLayout ?: return

        val scrollViewWidth = scrollView.width
        val contentWidth = buttonsContainer.width
        if (scrollViewWidth <= 0 || contentWidth <= 0) return

        val padding = if (contentWidth < scrollViewWidth) {
            (scrollViewWidth - contentWidth) / 2
        } else {
            16
        }

        // Повторная установка тех же паддингов триггерит layout-цикл; пропускаем,
        // если значение не изменилось
        val key = "$scrollViewWidth:$contentWidth:$padding"
        if (key == lastCenteredKey) return
        lastCenteredKey = key

        scrollView.setPadding(padding, 0, padding, 0)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable(KEY_CURRENT_MODE, currentMode)
        // Сохраняем фактический поворот бутылки, а не устаревшее поле
        outState.putFloat(KEY_CURRENT_ROTATION, binding.ivBottle.rotation)
    }

    private suspend fun checkUserAndSetup() {
        val user = userRepository.getUser()

        if (user == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        currentUser = user

        if (user.isGuest) {
            currentMode = GameMode.CHILDREN
            userRepository.setGameMode(GameMode.CHILDREN)
            Toast.makeText(this, R.string.game_guest_mode, Toast.LENGTH_LONG).show()
        } else {
            val savedMode = userRepository.getGameMode()
            currentMode = if (user.canAccessMode(savedMode)) {
                savedMode
            } else {
                getDefaultModeForAge(user.age).also {
                    userRepository.setGameMode(it)
                }
            }

            if (currentMode != savedMode) {
                Toast.makeText(
                    this,
                    getString(
                        R.string.game_mode_auto_changed,
                        currentMode.displayName(this),
                        user.age
                    ),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        loadStats()
        restoreUsedTasks()
        updateButtonsAvailability()
        updateButtonsState()
        // Без анимации при первом применении темы, иначе фон мигает при старте
        themeManager.applyTheme(currentMode, animate = false)

        // Приветствие показываем ПОСЛЕ загрузки пользователя, иначе всегда
        // отображается дефолтное сообщение
        showWelcomeMessage()
    }

    private fun updateButtonsAvailability() {
        val user = currentUser ?: return

        binding.btnModeChildren.visibility = View.VISIBLE
        binding.btnModeChildren.isEnabled = true

        setModeButtonVisibility(binding.btnModeTeen, user.canAccessMode(GameMode.TEEN))
        setModeButtonVisibility(binding.btnModeAdult, user.canAccessMode(GameMode.ADULT))
        setModeButtonVisibility(binding.btnModeAdultPlus, user.canAccessMode(GameMode.ADULT_PLUS))
        setModeButtonVisibility(binding.btnModeSex, user.canAccessMode(GameMode.SEX))
    }

    private fun setModeButtonVisibility(button: MaterialButton, visible: Boolean) {
        if (visible) {
            button.visibility = View.VISIBLE
            button.isEnabled = true
        } else {
            button.visibility = View.GONE
        }
    }

    private fun setupListeners() {
        binding.ivBottle.setOnClickListener {
            spinBottle()
        }

        binding.btnGroupMode.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val newMode = when (checkedId) {
                    R.id.btnModeChildren -> GameMode.CHILDREN
                    R.id.btnModeTeen -> GameMode.TEEN
                    R.id.btnModeAdult -> GameMode.ADULT
                    R.id.btnModeAdultPlus -> GameMode.ADULT_PLUS
                    R.id.btnModeSex -> GameMode.SEX
                    else -> return@addOnButtonCheckedListener
                }
                changeMode(newMode)
            }
        }

        binding.btnSettings.setOnClickListener {
            showSettingsDialog()
        }
    }

    private fun changeMode(newMode: GameMode) {
        lifecycleScope.launch {
            val user = userRepository.getUser()
            if (user != null && user.canAccessMode(newMode)) {
                userRepository.setGameMode(newMode)
                // Тема и состояние кнопок применяются к НОВОМУ режиму
                currentMode = newMode
                updateButtonsState()
                themeManager.applyTheme(newMode)
                Toast.makeText(
                    this@GameActivity,
                    getString(R.string.game_mode_changed, newMode.displayName(this@GameActivity)),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                val message = if (user != null) {
                    user.getUnlockMessage(this@GameActivity, newMode)
                } else {
                    getString(R.string.game_mode_unavailable)
                }
                Toast.makeText(this@GameActivity, message, Toast.LENGTH_LONG).show()
                // Возвращаем выделение текущего режима (кнопка уже «нажата» группой)
                updateButtonsState()
            }
        }
    }

    private fun showWelcomeMessage() {
        val user = currentUser
        val message = if (user == null) {
            getString(R.string.game_welcome_default)
        } else if (user.isGuest) {
            getString(R.string.game_welcome_guest)
        } else {
            getString(R.string.game_welcome_user, user.username, user.age)
        }
        showTaskInBottomSheet(Task(message), true)
    }

    private fun showSettingsDialog() {
        settingsDialog?.dismiss()

        val dialogBinding = DialogSettingsBinding.inflate(layoutInflater)

        lifecycleScope.launch {
            val user = userRepository.getUser()

            val userInfoText = if (user?.isGuest == true) {
                getString(R.string.settings_user_guest)
            } else {
                getString(R.string.settings_user_info, user?.username.orEmpty())
            }
            dialogBinding.btnUserInfo.text = userInfoText

            if (user != null) {
                dialogBinding.tvUserInfoDetail.text =
                    getString(R.string.settings_user_name, user.username)
                dialogBinding.tvUserAge.text = getString(R.string.settings_user_age, user.age)
            } else {
                dialogBinding.tvUserInfoDetail.text = getString(R.string.settings_user_guest_name)
                dialogBinding.tvUserAge.text = getString(R.string.settings_user_guest_age)
            }
            dialogBinding.tvUserModeDetail.text =
                getString(R.string.settings_user_mode, currentMode.displayName(this@GameActivity))

            dialogBinding.tvTasksCount.text = tasksCompleted.toString()

            // Показываем прогресс заданий (локализованная строка из ресурсов)
            val remaining = TasksProvider.getRemainingTasksCount(this@GameActivity, currentMode)
            val total = TasksProvider.getTasks(this@GameActivity, currentMode).size
            dialogBinding.tvTasksProgress.text =
                getString(R.string.settings_stats_progress, remaining, total)
        }

        settingsDialog = AlertDialog.Builder(this, R.style.SettingsDialogTheme)
            .setView(dialogBinding.root)
            .setCancelable(true)
            .create()

        settingsDialog?.show()

        setupSettingsDialogListeners(dialogBinding)
    }

    private fun setupSettingsDialogListeners(dialogBinding: DialogSettingsBinding) {
        dialogBinding.btnUserInfo.setOnClickListener {
            toggleVisibility(dialogBinding.layoutUserDetails, dialogBinding.btnUserInfo)
        }

        dialogBinding.btnStats.setOnClickListener {
            toggleVisibility(dialogBinding.layoutStatsDetails, dialogBinding.btnStats)
        }

        dialogBinding.btnClearStats.setOnClickListener {
            tasksCompleted = 0
            dialogBinding.tvTasksCount.text = "0"
            dialogBinding.tvTasksProgress.text = getString(
                R.string.settings_stats_progress,
                TasksProvider.getTasks(this, currentMode).size,
                TasksProvider.getTasks(this, currentMode).size
            )
            Toast.makeText(this@GameActivity, R.string.game_stats_reset, Toast.LENGTH_SHORT).show()
            saveStats()
            TasksProvider.resetAllTasks()
            saveUsedTasks()
        }

        dialogBinding.btnAbout.setOnClickListener {
            showAboutDialog()
        }

        dialogBinding.btnDonat.setOnClickListener {
            openDonateLink()
        }

        dialogBinding.btnLogout.setOnClickListener {
            showExitDialog()
        }

        dialogBinding.btnLanguage.setOnClickListener {
            showLanguageDialog()
        }
    }

    private fun showLanguageDialog() {
        val languages = LanguageManager.getAvailableLanguages(this)
        val currentLanguage = LanguageManager.getCurrentLanguage(this)

        val items = languages.values.toTypedArray()
        val checkedItem = languages.keys.indexOf(currentLanguage)

        AlertDialog.Builder(this, R.style.SettingsDialogTheme)
            .setTitle(R.string.settings_language)
            .setSingleChoiceItems(items, checkedItem) { dialog, which ->
                val selectedLanguage = languages.keys.elementAt(which)
                val languageName = languages[selectedLanguage] ?: selectedLanguage

                // Меняем язык и очищаем кэш заданий
                LanguageManager.setLanguage(this, selectedLanguage)
                TasksProvider.clearCache()
                dialog.dismiss()

                Toast.makeText(
                    this,
                    getString(R.string.language_changed, languageName),
                    Toast.LENGTH_LONG
                ).show()

                // Пересоздаем Activity — язык применится через attachBaseContext
                recreate()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun toggleVisibility(layout: View, button: MaterialButton) {
        layout.isVisible = !layout.isVisible
        button.icon = if (layout.isVisible) {
            ContextCompat.getDrawable(this, R.drawable.ic_expand_less)
        } else {
            ContextCompat.getDrawable(this, R.drawable.ic_expand_more)
        }
    }

    private fun getAppVersion(): String = BuildConfig.VERSION_NAME

    private fun showAboutDialog() {
        val appVersion = getAppVersion()

        val message = buildString {
            appendLine("🎯 ${getString(R.string.about_description)}")
            appendLine()
            appendLine("📌 ${getString(R.string.about_modes_title)}")
            appendLine("   ${getString(R.string.about_mode_children)}")
            appendLine("   ${getString(R.string.about_mode_teen)}")
            appendLine("   ${getString(R.string.about_mode_adult)}")
            appendLine("   ${getString(R.string.about_mode_adult_plus)}")
            appendLine("   ${getString(R.string.about_mode_sex)}")
            appendLine()
            appendLine("📦 ${getString(R.string.about_version)}: $appVersion")
            appendLine()
            appendLine(getString(R.string.about_developer))
        }

        AlertDialog.Builder(this, R.style.SettingsDialogTheme)
            .setTitle(R.string.about_title)
            .setMessage(message)
            .setPositiveButton(R.string.about_close, null)
            .create()
            .show()
    }

    private fun openDonateLink() {
        try {
            val donateUrl = getString(R.string.about_donatUrl)
            val intent = Intent(Intent.ACTION_VIEW, donateUrl.toUri())
            // На Android 11+ resolveActivity без <queries> в манифесте всегда вернет null,
            // поэтому просто пробуем запустить и ловим ActivityNotFoundException
            startActivity(intent)
        } catch (e: android.content.ActivityNotFoundException) {
            Logger.e(TAG, "No browser found for donate link", e)
            Toast.makeText(this, R.string.about_error_browser, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to open donate link", e)
            Toast.makeText(
                this,
                getString(R.string.about_error) + e.message,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun showExitDialog() {
        val user = currentUser
        val message = if (user?.isGuest == true) {
            getString(R.string.game_exit_guest_message)
        } else {
            getString(R.string.exit_message)
        }

        AlertDialog.Builder(this, R.style.SettingsDialogTheme)
            .setTitle(R.string.exit_title)
            .setMessage(message)
            .setPositiveButton(R.string.exit_logout) { _, _ ->
                lifecycleScope.launch {
                    userRepository.logout()
                    TasksProvider.resetAllTasks()
                    clearAllSavedProgress()
                    startActivity(Intent(this@GameActivity, LoginActivity::class.java))
                    finish()
                }
            }
            .setNegativeButton(R.string.exit_app) { _, _ ->
                finishAffinity()
            }
            .setNeutralButton(R.string.exit_cancel, null)
            .show()
    }

    private fun spinBottle() {
        // Слушатель навешивается один раз в setupListeners, а не при каждом спинне
        bottleAnimator.setOnSpinEndListener {
            showRandomTask()
            tasksCompleted++
            saveStats()
            saveUsedTasks()
        }
        bottleAnimator.spin()
        bottomSheetDialog?.dismiss()
    }

    private fun showRandomTask() {
        val task = TasksProvider.getRandomTask(this, currentMode)
        showTaskInBottomSheet(task, false)
    }

    private fun showTaskInBottomSheet(task: Task, isWelcome: Boolean = false) {
        bottomSheetDialog?.dismiss()

        bottomSheetBinding = BottomSheetTaskBinding.inflate(layoutInflater)
        bottomSheetDialog = BottomSheetDialog(this, R.style.BottomSheetDialogTheme)
        bottomSheetDialog?.setContentView(bottomSheetBinding!!.root)

        bottomSheetBinding?.tvTask?.text = task.text

        if (task.imageRes != null && task.imageRes > 0) {
            bottomSheetBinding?.ivTaskImage?.apply {
                visibility = View.VISIBLE
                setImageResource(task.imageRes)
            }
        } else {
            bottomSheetBinding?.ivTaskImage?.visibility = View.GONE
        }

        bottomSheetBinding?.btnClose?.text = if (isWelcome) {
            getString(R.string.game_task_start)
        } else {
            getString(R.string.game_task_close)
        }

        val bottomSheet =
            bottomSheetDialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
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

    private fun updateButtonsState() {
        binding.btnModeChildren.isChecked = currentMode == GameMode.CHILDREN
        binding.btnModeTeen.isChecked = currentMode == GameMode.TEEN
        binding.btnModeAdult.isChecked = currentMode == GameMode.ADULT
        binding.btnModeAdultPlus.isChecked = currentMode == GameMode.ADULT_PLUS
        binding.btnModeSex.isChecked = currentMode == GameMode.SEX

        updateButtonStyle(binding.btnModeChildren, currentMode == GameMode.CHILDREN)
        updateButtonStyle(binding.btnModeTeen, currentMode == GameMode.TEEN)
        updateButtonStyle(binding.btnModeAdult, currentMode == GameMode.ADULT)
        updateButtonStyle(binding.btnModeAdultPlus, currentMode == GameMode.ADULT_PLUS)
        updateButtonStyle(binding.btnModeSex, currentMode == GameMode.SEX)
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

    private fun getDefaultModeForAge(age: Int): GameMode {
        return when {
            age >= 18 -> GameMode.SEX
            age >= 16 -> GameMode.ADULT
            age >= 10 -> GameMode.TEEN
            else -> GameMode.CHILDREN
        }
    }

    /** Статистика хранится отдельно для каждого режима. */
    private fun loadStats() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        tasksCompleted = prefs.getInt(statsKey(currentMode), 0)
    }

    private fun saveStats() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putInt(statsKey(currentMode), tasksCompleted) }
    }

    /** Прогресс «использованных» заданий переживает перезапуск процесса. */
    private fun restoreUsedTasks() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        TasksProvider.restoreUsedTasks(currentMode, prefs.getString(usedTasksKey(currentMode), null))
    }

    private fun saveUsedTasks() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = try {
            Gson().toJson(TasksProvider.getUsedTasks(currentMode))
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to serialize used tasks", e)
            null
        }
        prefs.edit { putString(usedTasksKey(currentMode), json) }
    }

    private fun clearAllSavedProgress() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            GameMode.entries.forEach { mode ->
                remove(statsKey(mode))
                remove(usedTasksKey(mode))
            }
        }
    }

    override fun onDestroy() {
        // Отвязываем слушатели и останавливаем анимации, чтобы view
        // не удерживался после уничтожения Activity
        bottleAnimator.release()
        themeManager.cancelAnimations()
        bottomSheetDialog?.dismiss()
        settingsDialog?.dismiss()
        bottomSheetDialog = null
        bottomSheetBinding = null
        settingsDialog = null
        super.onDestroy()
    }
}
