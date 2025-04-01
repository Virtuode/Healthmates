package com.corps.healthmate.activities

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.method.PasswordTransformationMethod
import android.util.Patterns
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AnimationUtils
import android.view.animation.CycleInterpolator
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.airbnb.lottie.LottieAnimationView
import com.corps.healthmate.R
import com.corps.healthmate.navigation.NavigationManager
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import timber.log.Timber
import com.google.firebase.auth.ActionCodeSettings
import com.corps.healthmate.utils.SystemBarUtils
import com.corps.healthmate.utils.ActivityTransitionUtil
import de.hdodenhof.circleimageview.CircleImageView

class RegistrationActivity : AppCompatActivity() {
    private var regEmail: EditText? = null
    private var passwordEditText: EditText? = null
    private var confirmPasswordEditText: EditText? = null
    private var passwordToggleView: ImageView? = null
    private var confirmPasswordToggleView: ImageView? = null
    private var passwordStrengthProgress: ProgressBar? = null
    private var passwordStrengthText: TextView? = null
    private var passwordMatchText: TextView? = null
    private var checkAgreements: CheckBox? = null
    private var btnSignUp: Button? = null
    private var mAuth: FirebaseAuth? = null
    private var redirectLogin: TextView? = null
    private lateinit var loadingProgressBar: ProgressBar
    private lateinit var registrationContainer: View
    private lateinit var logoImageView: CircleImageView

    private lateinit var waterAnimation: LottieAnimationView

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration)

        // Use registration_container instead of main
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.registration_container)) { v: View, insets: WindowInsetsCompat ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setupStatusBar()
        initViews()

        setupAnimations()

        // Initialize UI components
        regEmail = findViewById(R.id.reg_email_EditText)
        passwordEditText = findViewById(R.id.password_reg)
        confirmPasswordEditText = findViewById(R.id.passwordEditTextConf)
        passwordToggleView = findViewById(R.id.password_toggle_view)
        confirmPasswordToggleView = findViewById(R.id.confirm_password_toggle_view)
        passwordStrengthProgress = findViewById(R.id.password_strength_progress)
        passwordStrengthText = findViewById(R.id.password_strength_text)
        passwordMatchText = findViewById(R.id.password_match_text)
        checkAgreements = findViewById(R.id.agreeCheckBox)
        btnSignUp = findViewById(R.id.button_SignUp)
        redirectLogin = findViewById(R.id.LoginTextView_redirect)


        setupPasswordVisibilityToggles()
        setupPasswordStrengthMeter()
        setupPasswordMatchValidation()

        mAuth = FirebaseAuth.getInstance()
        btnSignUp?.setOnClickListener {
            animateButton()
            if (validateEmail() && validatePassword() && validateAgreements()) {
                createAccount()
            }
        }

        redirectLogin?.setOnClickListener {
            startLoginActivityWithAnimation()
        }
    }

    private fun initViews() {
        loadingProgressBar = findViewById(R.id.loading_progress_bar)
        registrationContainer = findViewById(R.id.registration_container)
    }

    private fun setupAnimations() {

        waterAnimation = findViewById(R.id.animation_of_water)
        logoImageView = findViewById(R.id.logo_image)



        // Setup water animation to loop
        waterAnimation.apply {
            speed = 1.0f
            repeatCount = -1 // Infinite loop
            playAnimation()
        }

        // Setup logo hover animation
        logoImageView.post {
            val hoverAnimation = ObjectAnimator.ofFloat(logoImageView, "translationY", 0f, -20f, 0f).apply {
                duration = 1500  // Duration of one cycle
                repeatCount = ObjectAnimator.INFINITE  // Infinite loop
                repeatMode = ObjectAnimator.RESTART
            }
            hoverAnimation.start()
        }
    }

    private fun setupPasswordVisibilityToggles() {
        passwordToggleView?.setOnClickListener {
            togglePasswordVisibility(passwordEditText, passwordToggleView)
        }

        confirmPasswordToggleView?.setOnClickListener {
            togglePasswordVisibility(confirmPasswordEditText, confirmPasswordToggleView)
        }
    }

    private fun togglePasswordVisibility(editText: EditText?, toggleView: ImageView?) {
        if (editText == null || toggleView == null) return

        val isPasswordVisible = editText.transformationMethod == null

        // Create fade out animation
        val fadeOut = ObjectAnimator.ofFloat(toggleView, "alpha", 1f, 0f)
        fadeOut.duration = 150

        // Create fade in animation
        val fadeIn = ObjectAnimator.ofFloat(toggleView, "alpha", 0f, 1f)
        fadeIn.duration = 150

        fadeOut.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                // Change the icon
                if (isPasswordVisible) {
                    editText.transformationMethod = PasswordTransformationMethod.getInstance()
                    toggleView.setImageResource(R.drawable.ic_visibility_off)
                } else {
                    editText.transformationMethod = null
                    toggleView.setImageResource(R.drawable.ic_visibility)
                }
                // Start fade in animation
                fadeIn.start()
            }
        })

        // Start fade out animation
        fadeOut.start()

        // Maintain cursor position with animation
        editText.setSelection(editText.text.length)
    }

    private fun setupPasswordMatchValidation() {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validatePasswordMatch()
            }
        }

        passwordEditText?.addTextChangedListener(textWatcher)
        confirmPasswordEditText?.addTextChangedListener(textWatcher)
    }

    private fun setupPasswordStrengthMeter() {
        // Initially hide progress and text
        passwordStrengthProgress?.visibility = View.GONE
        passwordStrengthText?.visibility = View.GONE

        passwordEditText?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val password = s.toString()
                if (password.isEmpty()) {
                    // Hide progress and text when password is empty
                    passwordStrengthProgress?.animate()
                        ?.alpha(0f)
                        ?.setDuration(200)
                        ?.withEndAction {
                            passwordStrengthProgress?.visibility = View.GONE
                            passwordStrengthText?.visibility = View.GONE
                        }?.start()
                } else {
                    // Show progress and text when typing password
                    if (passwordStrengthProgress?.visibility == View.GONE) {
                        passwordStrengthProgress?.apply {
                            visibility = View.VISIBLE
                            alpha = 0f
                            animate().alpha(1f).setDuration(200).start()
                        }
                        passwordStrengthText?.apply {
                            visibility = View.VISIBLE
                            alpha = 0f
                            animate().alpha(1f).setDuration(200).start()
                        }
                    }
                    val strength = calculatePasswordStrength(password)
                    updatePasswordStrengthIndicator(strength)
                }
            }
        })
    }

    private fun validatePasswordMatch() {
        val password = passwordEditText?.text.toString()
        val confirmPassword = confirmPasswordEditText?.text.toString()

        if (password.isEmpty()) {
            passwordMatchText?.apply {
                animate()
                    .alpha(0f)
                    .setDuration(200)
                    .withEndAction { visibility = View.GONE }
                    .start()
            }
            return
        }

        if (confirmPassword.isNotEmpty()) {
            if (password == confirmPassword) {
                // If passwords match, fade out the match text
                passwordMatchText?.apply {
                    animate()
                        .alpha(0f)
                        .setDuration(300)
                        .withEndAction { visibility = View.GONE }
                        .start()
                }
                confirmPasswordEditText?.background = ContextCompat.getDrawable(this, R.drawable.rounded_bg)
            } else {
                // Show "Passwords do not match" only when they don't match
                passwordMatchText?.apply {
                    text = "Passwords do not match"
                    setTextColor(Color.RED)
                    if (visibility != View.VISIBLE) {
                        visibility = View.VISIBLE
                        alpha = 0f
                    }
                    animate()
                        .alpha(1f)
                        .setDuration(300)
                        .setInterpolator(AccelerateDecelerateInterpolator())
                        .start()
                }

                // Safe shake animation
                confirmPasswordEditText?.let { editText ->
                    val shake = ObjectAnimator.ofFloat(editText, View.TRANSLATION_X,
                        0f, 10f, -10f, 7f, -7f, 3f, -3f, 0f)
                    shake.duration = 300
                    shake.interpolator = CycleInterpolator(2f)
                    shake.start()
                }
            }
        } else {
            // Hide match text when confirm password is empty
            passwordMatchText?.apply {
                animate()
                    .alpha(0f)
                    .setDuration(200)
                    .withEndAction { visibility = View.GONE }
                    .start()
            }
        }
    }

    private fun updatePasswordStrengthIndicator(strength: Int) {
        passwordStrengthProgress?.let { progress ->
            // Animate progress change
            val animator = ObjectAnimator.ofInt(progress, "progress",
                progress.progress, strength)
            animator.duration = 300
            animator.interpolator = AccelerateDecelerateInterpolator()

            val color = when {
                strength <= 40 -> Color.RED
                strength <= 60 -> Color.YELLOW
                else -> Color.GREEN
            }

            // Animate color change
            val colorAnim = ValueAnimator.ofObject(
                ArgbEvaluator(),
                (progress.progressTintList?.defaultColor ?: color),
                color
            )
            colorAnim.duration = 300
            colorAnim.addUpdateListener { animator ->
                progress.progressTintList = ColorStateList.valueOf(animator.animatedValue as Int)
            }

            val strengthText = when {
                strength <= 40 -> "Weak"
                strength <= 60 -> "Medium"
                else -> "Strong"
            }

            // Animate text change
            passwordStrengthText?.apply {
                alpha = 1f
                visibility = View.VISIBLE
                animate()
                    .alpha(0f)
                    .setDuration(150)
                    .withEndAction {
                        text = strengthText
                        setTextColor(color)
                        animate()
                            .alpha(1f)
                            .setDuration(150)
                            .start()
                    }.start()
            }

            animator.start()
            colorAnim.start()
        }
    }

    private fun animateButton() {
        btnSignUp?.let { button ->
            button.isEnabled = false

            val animatorSet = AnimatorSet().apply {
                playTogether(
                    ObjectAnimator.ofFloat(button, View.SCALE_X, 1f, 0.9f, 1f),
                    ObjectAnimator.ofFloat(button, View.SCALE_Y, 1f, 0.9f, 1f)
                )
                duration = 300
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        button.isEnabled = true
                    }
                })
            }
            animatorSet.start()
        }
    }

    private fun startLoginActivityWithAnimation() {
        val intent = Intent(this, LoginActivity::class.java)
        ActivityTransitionUtil.startActivityWithAnimation(this, intent)
        finish()
    }

    // Email validation method
    private fun validateEmail(): Boolean {
        val email = regEmail!!.text.toString().trim { it <= ' ' }
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            regEmail!!.error = "Invalid email address"
            return false
        }
        return true
    }

    // Password validation method
    private fun validatePassword(): Boolean {
        val password = passwordEditText!!.text.toString().trim { it <= ' ' }
        val confirmPassword = confirmPasswordEditText!!.text.toString().trim { it <= ' ' }
        if (password.isEmpty() || password.length < 8) {
            Toast.makeText(this, "Password must be at least 8 characters long", Toast.LENGTH_SHORT).show()
            return false
        }
        if (password != confirmPassword) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    // Agreement checkbox validation method
    private fun validateAgreements(): Boolean {
        if (!checkAgreements !!.isChecked) {
            Toast.makeText(this, "Please agree to the terms and conditions", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun createAccount() {
        val email = regEmail?.text.toString().trim()
        val password = passwordEditText?.text.toString().trim()

        showLoading()

        mAuth?.createUserWithEmailAndPassword(email, password)
            ?.addOnCompleteListener(this) { task ->
                hideLoading()
                if (task.isSuccessful) {
                    NavigationManager.handleRegistrationSuccess(this@RegistrationActivity)
                    
                    // Clear sensitive data
                    regEmail?.text?.clear()
                    passwordEditText?.text?.clear()
                    confirmPasswordEditText?.text?.clear()
                } else {
                    handleRegistrationError(task.exception!!)
                }
            }
    }

    private fun calculatePasswordStrength(password: String): Int {
        var score = 0
        if (password.length >= 8) score += 20
        if (password.matches(Regex(".*[A-Z].*"))) score += 20
        if (password.matches(Regex(".*[a-z].*"))) score += 20
        if (password.matches(Regex(".*[0-9].*"))) score += 20
        if (password.matches(Regex(".*[!@#\$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*"))) score += 20
        return score
    }

    private fun handleRegistrationError(exception: Exception) {
        val errorMessage = when (exception) {
            is FirebaseAuthWeakPasswordException -> getString(R.string.error_weak_password)
            is FirebaseAuthInvalidCredentialsException -> getString(R.string.error_invalid_email)
            is FirebaseAuthUserCollisionException -> getString(R.string.error_email_exists)
            else -> getString(R.string.error_registration_failed)
        }
        showError(errorMessage)
    }

    private fun showLoading() {
        loadingProgressBar.visibility = View.VISIBLE
        registrationContainer.alpha = 0.5f
        registrationContainer.isEnabled = false
    }

    private fun hideLoading() {
        loadingProgressBar.visibility = View.GONE
        registrationContainer.alpha = 1.0f
        registrationContainer.isEnabled = true
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun setupStatusBar() {
        SystemBarUtils.setupSystemBars(this)
    }

    override fun onPause() {
        super.onPause()

        waterAnimation.pauseAnimation()
    }

    override fun onResume() {
        super.onResume()

        waterAnimation.resumeAnimation()
    }

    override fun onDestroy() {
        super.onDestroy()
        waterAnimation.cancelAnimation()
    }
}