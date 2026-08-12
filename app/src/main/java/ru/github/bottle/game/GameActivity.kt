package ru.github.bottle.game

import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.Vibrator
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
import ru.github.bottle.utils.Task
import ru.github.bottle.utils.TasksProvider
import ru.github.bottle.BuildConfig
import kotlin.random.Random

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

    companion object {
        private const val KEY_CURRENT_MODE = "current_mode"
        private const val KEY_CURRENT_ROTATION = "current_rotation"
        private const val KEY_TASKS_COMPLETED = "tasks_completed"
        private const val PREFS_NAME = "game_stats"
        private const val KEY_TASKS_STATS = "tasks_completed"

        fun getIntent(context: Context): Intent {
            return Intent(context, GameActivity::class.java)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
        showWelcomeMessage()
        loadStats()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable(KEY_CURRENT_MODE, currentMode)
        outState.putFloat(KEY_CURRENT_ROTATION, currentRotation)
        outState.putInt(KEY_TASKS_COMPLETED, tasksCompleted)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        currentMode = savedInstanceState.getSerializable(KEY_CURRENT_MODE) as? GameMode ?: GameMode.CHILDREN
        currentRotation = savedInstanceState.getFloat(KEY_CURRENT_ROTATION)
        tasksCompleted = savedInstanceState.getInt(KEY_TASKS_COMPLETED)
        binding.ivBottle.rotation = currentRotation
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }

    private suspend fun checkUserAndSetup() {
        val user = userRepository.getUser()

        if (user == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        currentUser = user

        if (currentMode == GameMode.CHILDREN) {
            val savedMode = userRepository.getGameMode()
            if (user.canAccessMode(savedMode)) {
                currentMode = savedMode
            } else {
                currentMode = when {
                    user.age >= 18 -> GameMode.SEX
                    user.age >= 16 -> GameMode.ADULT
                    user.age >= 10 -> GameMode.TEEN
                    else -> GameMode.CHILDREN
                }
                userRepository.setGameMode(currentMode)
            }
        }

        if (user.isGuest) {
            currentMode = GameMode.CHILDREN
            userRepository.setGameMode(GameMode.CHILDREN)
            Toast.makeText(this, R.string.game_guest_mode, Toast.LENGTH_LONG).show()
        } else {
            val savedMode = userRepository.getGameMode()

            if (user.canAccessMode(savedMode)) {
                currentMode = savedMode
            } else {
                currentMode = when {
                    user.age >= 18 -> GameMode.SEX
                    user.age >= 16 -> GameMode.ADULT
                    user.age >= 10 -> GameMode.TEEN
                    else -> GameMode.CHILDREN
                }
                userRepository.setGameMode(currentMode)

                Toast.makeText(
                    this,
                    getString(R.string.game_mode_auto_changed, currentMode.displayName, user.age),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        updateButtonsAvailability()
        updateModeUI()
        updateButtonsState()
        applyTheme(currentMode)
    }

    private fun applyTheme(mode: GameMode) {
        val backgroundRes = try {
            when (mode) {
                GameMode.CHILDREN -> R.drawable.background_children
                GameMode.TEEN -> R.drawable.background_teen
                GameMode.ADULT -> R.drawable.background_adult
                GameMode.ADULT_PLUS -> R.drawable.background_adult_plus
                GameMode.SEX -> R.drawable.background_adult_plus
            }
        } catch (_: Exception) {
            android.R.color.white
        }

        try {
            animateBackgroundChange(backgroundRes)
        } catch (_: Exception) {
            binding.root.setBackgroundColor(ContextCompat.getColor(this, android.R.color.white))
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
    }

    private fun animateBackgroundChange(newBackgroundRes: Int) {
        binding.root.animate()
            .alpha(0f)
            .setDuration(300)
            .withEndAction {
                binding.root.setBackgroundResource(newBackgroundRes)
                binding.root.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .start()
            }
            .start()
    }

    private fun animateBottleChange(newBottleRes: Int) {
        binding.ivBottle.animate()
            .scaleX(0f)
            .scaleY(0f)
            .setDuration(300)
            .withEndAction {
                binding.ivBottle.setImageResource(newBottleRes)
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

        binding.btnModeChildren.visibility = View.VISIBLE
        binding.btnModeChildren.isEnabled = true

        setModeButtonVisibility(binding.btnModeTeen, user.canAccessMode(GameMode.TEEN))
        setModeButtonVisibility(binding.btnModeAdult, user.canAccessMode(GameMode.ADULT))
        setModeButtonVisibility(binding.btnModeAdultPlus, user.canAccessMode(GameMode.ADULT_PLUS))
        setModeButtonVisibility(binding.btnModeSex, user.canAccessMode(GameMode.SEX))

        if (user.isGuest) {
            binding.btnModeTeen.visibility = View.GONE
            binding.btnModeAdult.visibility = View.GONE
            binding.btnModeAdultPlus.visibility = View.GONE
            binding.btnModeSex.visibility = View.GONE
        }
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

                lifecycleScope.launch {
                    val user = userRepository.getUser()

                    if (user != null && user.canAccessMode(newMode)) {
                        currentMode = newMode
                        userRepository.setGameMode(currentMode)
                        updateModeUI()
                        applyTheme(currentMode)
                        Toast.makeText(
                            this@GameActivity,
                            getString(R.string.game_mode_changed, newMode.displayName),
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        val message = if (user != null) {
                            user.getUnlockMessage(newMode)
                        } else {
                            getString(R.string.game_mode_unavailable)
                        }
                        Toast.makeText(
                            this@GameActivity,
                            message,
                            Toast.LENGTH_LONG
                        ).show()
                        updateButtonsState()
                    }
                }
            }
        }

        binding.btnSettings.setOnClickListener {
            showSettingsDialog()
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
                getString(R.string.settings_user_info, user?.username ?: "Гость")
            }
            dialogBinding.btnUserInfo.text = userInfoText

            if (user != null) {
                dialogBinding.tvUserInfoDetail.text = getString(R.string.settings_user_name, user.username)
                dialogBinding.tvUserAge.text = getString(R.string.settings_user_age, user.age)
                dialogBinding.tvUserModeDetail.text = getString(R.string.settings_user_mode, currentMode.displayName)
            } else {
                dialogBinding.tvUserInfoDetail.text = getString(R.string.settings_user_guest_name)
                dialogBinding.tvUserAge.text = getString(R.string.settings_user_guest_age)
                dialogBinding.tvUserModeDetail.text = getString(R.string.settings_user_mode, currentMode.displayName)
            }

            dialogBinding.tvTasksCount.text = tasksCompleted.toString()
        }

        settingsDialog = AlertDialog.Builder(this, R.style.SettingsDialogTheme)
            .setView(dialogBinding.root)
            .setCancelable(true)
            .create()

        settingsDialog?.show()

        dialogBinding.btnUserInfo.setOnClickListener {
            val isVisible = dialogBinding.layoutUserDetails.visibility == View.VISIBLE
            dialogBinding.layoutUserDetails.visibility = if (isVisible) View.GONE else View.VISIBLE
            dialogBinding.btnUserInfo.icon = if (isVisible) {
                ContextCompat.getDrawable(this, R.drawable.ic_expand_more)
            } else {
                ContextCompat.getDrawable(this, R.drawable.ic_expand_less)
            }
        }

        dialogBinding.btnStats.setOnClickListener {
            val isVisible = dialogBinding.layoutStatsDetails.visibility == View.VISIBLE
            dialogBinding.layoutStatsDetails.visibility = if (isVisible) View.GONE else View.VISIBLE
            dialogBinding.btnStats.icon = if (isVisible) {
                ContextCompat.getDrawable(this, R.drawable.ic_expand_more)
            } else {
                ContextCompat.getDrawable(this, R.drawable.ic_expand_less)
            }
        }

        dialogBinding.btnClearStats.setOnClickListener {
            tasksCompleted = 0
            dialogBinding.tvTasksCount.text = "0"
            Toast.makeText(
                this@GameActivity,
                R.string.game_stats_reset,
                Toast.LENGTH_SHORT
            ).show()
            saveStats()
        }

        dialogBinding.btnAbout.setOnClickListener {
            showAboutDialog()
        }

        dialogBinding.btnLogout.setOnClickListener {
            showExitDialog()
        }
    }

    private fun getAppVersion(): String {
       return BuildConfig.VERSION_NAME
    }

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

        val builder = AlertDialog.Builder(this, android.R.style.Theme_Material_Light_Dialog_Alert)
            .setTitle(R.string.about_title)
            .setMessage(message)

        builder.setPositiveButton(R.string.about_donate) { _, _ ->
            openDonateLink()
        }

        builder.setNegativeButton(R.string.about_close, null)
        builder.show()
    }

    private fun openDonateLink() {
        try {
            val donateUrl = getString(R.string.about_donatUrl)
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = android.net.Uri.parse(donateUrl)

            // Проверяем, есть ли приложение для открытия ссылки
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                // Если браузер не найден, показываем сообщение
                Toast.makeText(
                    this,
                    getString(R.string.about_error_browser),
                    Toast.LENGTH_SHORT
                ).show()
            }
        } catch (e: Exception) {
            Toast.makeText(
                this,
                getString(R.string.about_error) + "${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun showExitDialog() {
        AlertDialog.Builder(this, android.R.style.Theme_Material_Light_Dialog_Alert)
            .setTitle(R.string.exit_title)
            .setMessage(R.string.exit_message)
            .setPositiveButton(R.string.exit_logout) { _, _ ->
                lifecycleScope.launch {
                    userRepository.logout()
                    TasksProvider.resetAllTasks()
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
        applyTheme(currentMode)
        updateButtonsState()
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

    private fun loadStats() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        tasksCompleted = prefs.getInt(KEY_TASKS_STATS, 0)
    }

    private fun saveStats() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_TASKS_STATS, tasksCompleted).apply()
    }

    override fun onDestroy() {
        super.onDestroy()
        bottomSheetDialog?.dismiss()
        settingsDialog?.dismiss()
    }
}