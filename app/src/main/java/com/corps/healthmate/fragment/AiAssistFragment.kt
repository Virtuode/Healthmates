package com.corps.healthmate.fragment


import android.animation.Animator
import android.animation.ObjectAnimator
import android.app.Activity
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewAnimationUtils
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.LinearInterpolator
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2

import com.airbnb.lottie.LottieAnimationView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import com.corps.healthmate.R
import com.corps.healthmate.activities.AiChatActivity
import com.corps.healthmate.activities.EmergencyHandlerActivity
import com.corps.healthmate.activities.MenuDialog
import com.corps.healthmate.activities.NotificationActivity
import com.corps.healthmate.activities.WelcomeScreenActivity
import com.corps.healthmate.adapters.ReminderAdapter
import com.corps.healthmate.adapters.SliderAdapter
import com.corps.healthmate.database.Reminder
import com.corps.healthmate.databinding.FragmentAiAssistBinding
import com.corps.healthmate.interfaces.OnReminderSavedListener
import com.corps.healthmate.interfaces.TimeDifferenceCallback
import com.corps.healthmate.notification.NotificationHelper
import com.corps.healthmate.repository.ReminderRepository
import com.corps.healthmate.utils.CloudinaryHelper
import com.corps.healthmate.utils.ReminderCreationHelper
import com.corps.healthmate.viewmodel.AiAssistViewModel
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.math.hypot
import androidx.navigation.fragment.findNavController
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import com.google.firebase.database.DatabaseException
import com.corps.healthmate.utils.FirebaseReferenceManager
import androidx.navigation.fragment.FragmentNavigator
import com.google.android.material.transition.MaterialContainerTransform
import com.google.android.material.transition.MaterialElevationScale
import android.graphics.Color
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.corps.healthmate.utils.ReminderManager

class AiAssistFragment : Fragment(), OnReminderSavedListener,
    ReminderAdapter.OnReminderClickListener, TimeDifferenceCallback {
    private var _binding: FragmentAiAssistBinding? = null
    private val binding get() = _binding!!
    private lateinit var remindersRecyclerViews: RecyclerView
    private lateinit var reminderAdapter: ReminderAdapter
    private lateinit var viewModel: AiAssistViewModel
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var pulseAnimation: Animation
    private var chatActionButton: ImageButton? = null
    private var animationView: LottieAnimationView? = null
    private var viewPager: ViewPager2? = null
    private var tabLayout: TabLayout? = null
    private val sliderAdapter: SliderAdapter by lazy { SliderAdapter(requireContext()) }
    private var currentSlideIndex = 0
    private val SLIDER = 3000
    private var sliderHandler: Handler? = null
    private var sliderRunnable: Runnable? = null
    private var usernameTextView: TextView? = null
    private var profileImageView: CircleImageView? = null
    private var loadingProgress: ProgressBar? = null
    private var imageMessage: ImageView? = null
    private var imageUri: Uri? = null
    private var repository: ReminderRepository? = null
    private var createReminderButton: LinearLayout? = null
    private val auth = FirebaseAuth.getInstance()
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
    private val menuDialog = MenuDialog()
    private var cardAiClick: MaterialCardView? = null
    private var reminders: List<Reminder> = emptyList()

    companion object {

        private const val TAG = "AiAssistFragment"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val factory =
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        viewModel = ViewModelProvider(this, factory)[AiAssistViewModel::class.java]
        checkAuthenticationState()


    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAiAssistBinding.inflate(inflater, container, false)
        val view = binding.root


        remindersRecyclerViews = binding.remindersRecyclerView
        repository = ReminderRepository(requireActivity().application)

        notificationHelper = NotificationHelper(requireContext())


        // Initialize RecyclerView if not already done
        if (remindersRecyclerViews.adapter == null) {
            remindersRecyclerViews.layoutManager = LinearLayoutManager(context)
            reminderAdapter = ReminderAdapter(
                listener = this,
                callback = this
            )
            remindersRecyclerViews.adapter = reminderAdapter
        }
        viewPager = binding.viewPager
        tabLayout = binding.tabLayout

        val sliderAdapter = SliderAdapter(requireContext())
        viewPager?.adapter = sliderAdapter

        // Configure TabLayout with ViewPager2
        TabLayoutMediator(
            tabLayout!!,
            viewPager!!
        ) { _, _ ->
            // Empty configuration as we're using custom dot indicators
        }.attach()


        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            // Observe reminders using LiveData from repository
            viewModel.allReminders.observe(viewLifecycleOwner) { reminders ->
                // Update UI on main thread
                lifecycleScope.launch(Dispatchers.Main) {
                    try {
                        // Filter active reminders if needed
                        val activeReminders = reminders.filter { it.isActive }

                        // Update adapter with new data
                        reminderAdapter.updateReminders(activeReminders)

                        // Update RecyclerView visibility
                        remindersRecyclerViews.visibility = if (activeReminders.isNotEmpty()) {
                            View.VISIBLE
                        } else {
                            View.GONE
                        }

                        // Log the update
                        Timber.tag(TAG).d("Updated reminders list. Count: %s", activeReminders.size)
                    } catch (e: Exception) {
                        Timber.tag(TAG).e(e, "Error updating reminders")
                    }
                }
            }
        } else {
            Timber.tag(TAG).e("User ID is null, cannot fetch reminders")
            remindersRecyclerViews.visibility = View.GONE
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        try {
            viewModel = ViewModelProvider(this)[AiAssistViewModel::class.java]
            pulseAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.pulse)


            setupViews()
            setupObservers()
            setupClickListeners()
            setupSlider()
            loadUserData()
            setupSearchFunctionality()


            // Initial load of reminders
            val userId = getCurrentUserId()
            if (userId.isNotEmpty()) {
                viewModel.loadReminders(userId)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error in onViewCreated")
        }
    }


    private fun setupViews() {

        remindersRecyclerViews = binding.remindersRecyclerView
        reminderAdapter = ReminderAdapter(this, this)
        remindersRecyclerViews.apply {
            // Set the layout manager with horizontal orientation
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = reminderAdapter
            setHasFixedSize(true)
            // Enable nested scrolling
            isNestedScrollingEnabled = true


        }



        loadingProgress = binding.loadingProgress
        usernameTextView = binding.usernameMain
        profileImageView = binding.profileImageAiAssist
        viewPager = binding.viewPager
        tabLayout = binding.tabLayout
        imageMessage = binding.messageCenterIcon
        createReminderButton = binding.createReminderButton
        cardAiClick = binding.quickChatCard

        // Setup initial animations
        animationView?.setAnimation(R.raw.metaanim)
        animationView?.playAnimation()
    }


    private fun checkAuthenticationState() {
        if (auth.currentUser == null) {
            navigateToWelcomeScreen()
        }
    }

    private fun navigateToWelcomeScreen() {
        val intent = Intent(requireActivity(), WelcomeScreenActivity::class.java)
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        requireActivity().finish()
    }

    private fun redirectMessageCenter() {
        val intent = Intent(requireActivity(), NotificationActivity::class.java)
        startActivity(intent)
    }

    private fun setupObservers() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            loadingProgress?.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            }
        }

        // Observe reminders
        viewModel.allReminders.observe(viewLifecycleOwner) { reminders ->
            lifecycleScope.launch(Dispatchers.Main) {
                try {
                    val activeReminders = reminders.filter { it.isActive }
                    reminderAdapter.updateReminders(activeReminders)
                    updateRemindersVisibility(activeReminders.isNotEmpty())
                    Timber.d("Updated reminders list. Count: %d", activeReminders.size)
                } catch (e: Exception) {
                    Timber.e(e, "Error updating reminders")
                }
            }
        }
    }

    private fun setupClickListeners() {
        // Profile image click
        profileImageView?.setOnClickListener {
            if (isAdded && !menuDialog.isAdded) {
                // Set up a callback for when the image is updated
                menuDialog.setOnImageUploadedListener { 
                    val userId = getCurrentUserId()
                    if (userId.isNotEmpty()) {
                        refreshProfileImage(userId)
                    }
                }
                menuDialog.show(childFragmentManager, "menuDialog")
            }
        }

        // Chat action button
        chatActionButton?.setOnClickListener {
            // Create a circular reveal animation
            val intent = Intent(requireContext(), AiChatActivity::class.java)

            // Create circular reveal animation from the button
            val options = ActivityOptions.makeSceneTransitionAnimation(
                requireActivity(),
                chatActionButton,  // The view to transition from
                "chat_transition" // The transition name
            )

            // Add subtle vibration feedback
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = requireContext().getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                requireContext().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            // Then use the vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(50)
            }

            // Start the activity with animation
            startActivity(intent, options.toBundle())
        }

        // FAB click
        createReminderButton?.setOnClickListener {
            val userId = getCurrentUserId()
            if (userId.isNotEmpty()) {
                ReminderCreationHelper.showAddPillDialog(requireContext(), userId) {
                    loadReminders()
                }
            } else {
                Toast.makeText(
                    requireContext(),
                    "Please sign in to create reminders",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        imageMessage?.setOnClickListener {
            redirectMessageCenter()
        }

        cardAiClick?.setOnClickListener {
            redirectToAiCenter()
        }
    }



    private fun setupSlider() {
        viewPager?.adapter = sliderAdapter
        TabLayoutMediator(tabLayout!!, viewPager!!) { _, _ -> }.attach()
        startAutoSlide()
    }

    private fun updateRemindersVisibility(hasReminders: Boolean) {
        remindersRecyclerViews.visibility = if (hasReminders) View.VISIBLE else View.GONE
    }

    private fun loadUserData() {
        val user = auth.currentUser
        if (user != null) {
            loadingProgress?.visibility = View.VISIBLE
            fetchUserSurveyData()
        }
    }

    override fun onDestroyView() {
        // Clean up resources
        sliderHandler?.removeCallbacks(sliderRunnable!!)
        remindersRecyclerViews.adapter = null
        notificationHelper.onDestroy()
        _binding = null
        super.onDestroyView()
    }

    override fun onReminderSaved() {
        // No need to call setupRecyclerView() as LiveData will automatically update
        Timber.tag(TAG).d("Reminder saved, LiveData will update automatically")
    }


    private fun startAutoSlide() {
        sliderHandler = Handler(Looper.getMainLooper())
        sliderRunnable = Runnable {
            viewPager?.let { pager ->
                if (currentSlideIndex == sliderAdapter.itemCount) {
                    currentSlideIndex = 0
                }
                pager.setCurrentItem(currentSlideIndex++, true)
            }
            sliderHandler?.postDelayed(sliderRunnable!!, SLIDER.toLong())
        }
        sliderHandler?.postDelayed(sliderRunnable!!, SLIDER.toLong())
    }

    override fun onDeleteClick(reminder: Reminder?) {
        reminder?.let {
            viewModel.deleteReminder(it)
        }
    }

    override fun onDeactivateClick(reminder: Reminder?) {
        reminder?.let {
            it.isActive = !it.isActive
            viewModel.updateReminder(it)
        }
    }

    // Implement TimeDifferenceCallback method
    override fun onCalculateTimeDifference(textView: TextView, reminderTime: String) {
        try {
            val currentTime = Calendar.getInstance()
            val reminderCalendar = Calendar.getInstance()

            // Split the time string and handle both 12-hour and 24-hour formats
            val (timeStr, period) = if (reminderTime.contains(" ")) {
                reminderTime.split(" ")
            } else {
                // If no AM/PM, assume 24-hour format
                listOf(reminderTime, "")
            }

            val (hours, minutes) = timeStr.split(":").map { it.trim().toInt() }

            // Set the reminder calendar time
            reminderCalendar.set(Calendar.HOUR, if (hours == 12) 0 else hours)
            reminderCalendar.set(Calendar.MINUTE, minutes)
            reminderCalendar.set(Calendar.SECOND, 0)
            reminderCalendar.set(Calendar.MILLISECOND, 0)

            // Handle AM/PM
            if (period.isNotEmpty()) {
                reminderCalendar.set(
                    Calendar.AM_PM,
                    if (period.uppercase().startsWith("P")) Calendar.PM else Calendar.AM
                )
            }

            // Set to today's date
            reminderCalendar.set(Calendar.YEAR, currentTime.get(Calendar.YEAR))
            reminderCalendar.set(Calendar.MONTH, currentTime.get(Calendar.MONTH))
            reminderCalendar.set(Calendar.DAY_OF_MONTH, currentTime.get(Calendar.DAY_OF_MONTH))

            // If the time has passed for today, set it to tomorrow
            if (reminderCalendar.before(currentTime)) {
                reminderCalendar.add(Calendar.DAY_OF_MONTH, 1)
            }

            // Calculate time difference
            val diffInMillis = reminderCalendar.timeInMillis - currentTime.timeInMillis
            val diffHours = TimeUnit.MILLISECONDS.toHours(diffInMillis)
            val diffMinutes = TimeUnit.MILLISECONDS.toMinutes(diffInMillis) % 60

            // Format the remaining time text
            val remainingText = when {
                diffHours > 0 -> "${diffHours}h ${if (diffMinutes > 0) "${diffMinutes}m" else ""} remaining"
                diffMinutes > 0 -> "${diffMinutes}m remaining"
                else -> "Time's up!"
            }

            // Update UI
            textView.text = remainingText

            // Set color based on urgency
            val colorRes = when {
                diffHours >= 2 -> R.color.green_201
                diffHours >= 1 || diffMinutes >= 30 -> R.color.yellow_700
                else -> R.color.red_500
            }
            textView.setTextColor(ContextCompat.getColor(requireContext(), colorRes))

            // Show warning for soon-to-come reminders
            if (diffHours == 0L && diffMinutes <= 5) {
                val itemView = textView.parent.parent as? View
                itemView?.let {
                    val pillNames = getPillNamesFromChipGroup(it)
                    if (pillNames.isNotEmpty()) {
                        notificationHelper.showWarningPopup("com.corps.healthmate.models.Medicine Reminder", pillNames)
                    }
                }
            }

        } catch (e: Exception) {
            Timber.e(e, "Error calculating time difference for time: $reminderTime")
            if (e is NumberFormatException || e is IndexOutOfBoundsException) {
                textView.text = "Invalid time format"
            } else {
                textView.text = "Unable to calculate time"
            }
            textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.secondary_text))
        }
    }

    private fun getPillNamesFromChipGroup(itemView: View): List<String> {
        val pillNames = mutableListOf<String>()
        val chipGroup = itemView.findViewById<ChipGroup>(R.id.pillNamesChipGroup)
        for (i in 0 until chipGroup.childCount) {
            val chip = chipGroup.getChildAt(i) as? Chip
            chip?.text?.toString()?.let { pillNames.add(it) }
        }
        return pillNames
    }


    private fun fetchUserSurveyData() {
        val user = auth.currentUser ?: run {
            Timber.tag(TAG).d("No user found")
            updateUIForNoUser()
            return
        }

        try {
            val userId = user.uid
            // Keep the user data synced
            FirebaseReferenceManager.keepSynced("patients/$userId")
            
            val userRef = FirebaseReferenceManager.getReference("patients/$userId")

            // Use a transaction to get all data at once with timeout
            userRef.get()
                .addOnCompleteListener { task ->
                    if (!isAdded) return@addOnCompleteListener

                    if (!task.isSuccessful) {
                        Timber.tag(TAG).e(task.exception, "Error fetching user data")
                        updateUIForError()
                        return@addOnCompleteListener
                    }

                    try {
                        val snapshot = task.result
                        if (!snapshot.exists()) {
                            Timber.tag(TAG).d("No user data found")
                            updateUIForNoData(user)
                            return@addOnCompleteListener
                        }

                        // Parse data efficiently using extension function
                        val userData = snapshot.toUserData()
                        updateUserInterface(userData)
                        loadProfileImage(userData.imageUrl)
                    } catch (e: Exception) {
                        Timber.tag(TAG).e(e, "Error parsing user data")
                        updateUIForError()
                    }
                }
                .addOnFailureListener { e ->
                    Timber.tag(TAG).e(e, "Failed to fetch user data")
                    updateUIForError()
                }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error in fetchUserSurveyData")
            updateUIForError()
        }
    }

    // Extension function for efficient data parsing
    private fun DataSnapshot.toUserData(): UserData {
        val basicInfo = child("survey").child("basicInfo")
        return UserData(
            firstName = basicInfo.child("firstName").getValue(String::class.java) ?: "",
            middleName = basicInfo.child("middleName").getValue(String::class.java) ?: "",
            lastName = basicInfo.child("lastName").getValue(String::class.java) ?: "",
            imageUrl = basicInfo.child("imageUrl").getValue(String::class.java)
                ?: child("imageUrl").getValue(String::class.java)
        )
    }

    private data class UserData(
        val firstName: String,
        val middleName: String,
        val lastName: String,
        val imageUrl: String?
    ) {
        val displayName: String
            get() = when {
                firstName.isNotEmpty() && lastName.isNotEmpty() && middleName.isNotEmpty() ->
                    "$firstName $middleName $lastName"
                firstName.isNotEmpty() && lastName.isNotEmpty() ->
                    "$firstName $lastName"
                firstName.isNotEmpty() -> firstName
                else -> "User"
            }

        val greetingName: String
            get() = when {
                firstName.isNotEmpty() -> firstName
                middleName.isNotEmpty() -> middleName
                else -> displayName
            }
    }

    private fun updateUserInterface(userData: UserData) {
        view?.post {
            // Create greeting based on time
            val greeting = createTimeBasedGreeting(userData.greetingName)
            usernameTextView?.text = greeting

            // Update feeling text
            view?.findViewById<TextView>(R.id.feeling_text)?.text = getFeelingText()
        }
    }

    private fun createTimeBasedGreeting(name: String): String {
        return when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 5..11 -> "Good morning, $name"
            in 12..16 -> "Good afternoon, $name"
            in 17..20 -> "Good evening, $name"
            else -> "Hi, $name"
        }
    }

    private fun getFeelingText(): String {
        return if (Calendar.getInstance().get(Calendar.HOUR_OF_DAY) in 5..11) {
            "How are you feeling today?"
        } else {
            "Hope you're having a great day!"
        }
    }

    private fun loadProfileImage(profileImageUrl: String?) {
        if (profileImageUrl.isNullOrEmpty()) {
            Timber.tag(TAG).d("No profile image URL found")
            profileImageView?.setImageResource(R.drawable.user)
            loadingProgress?.visibility = View.GONE
            return
        }

        val safeImageUrl = profileImageUrl.replace("http://", "https://")
        Timber.tag(TAG).d("Loading image from URL: $safeImageUrl")

        context?.let { ctx ->
            Glide.with(ctx)
                .load(safeImageUrl)
                .placeholder(R.drawable.baseline_person_24)
                .error(R.drawable.user)
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        Timber.tag(TAG).e(e, "Failed to load image from $safeImageUrl")
                        activity?.runOnUiThread {
                            loadingProgress?.visibility = View.GONE
                            profileImageView?.setImageResource(R.drawable.user)
                        }
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: Target<Drawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        activity?.runOnUiThread {
                            loadingProgress?.visibility = View.GONE
                        }
                        return false
                    }
                })
                .into(profileImageView ?: return)
        }
    }

    private fun refreshProfileImage(userId: String) {
        FirebaseDatabase.getInstance().reference
            .child("patients")
            .child(userId)
            .child("survey")
            .child("basicInfo")
            .child("imageUrl")
            .get()
            .addOnSuccessListener { snapshot ->
                val imageUrl = snapshot.getValue(String::class.java)
                loadProfileImage(imageUrl)
            }
            .addOnFailureListener { e ->
                Timber.tag(TAG).e(e, "Failed to fetch updated profile image URL")
            }
    }

    private fun updateDefaultProfileImage() {
        activity?.runOnUiThread {
            profileImageView?.setImageResource(R.drawable.user)
            loadingProgress?.visibility = View.GONE
        }
    }

    private fun updateUIForNoUser() {
        usernameTextView?.text = "User"
        profileImageView?.setImageResource(R.drawable.user)
        loadingProgress?.visibility = View.GONE
    }

    private fun updateUIForNoData(user: FirebaseUser) {
        usernameTextView?.text = user.displayName ?: "User"
        profileImageView?.setImageResource(R.drawable.user)
        loadingProgress?.visibility = View.GONE
    }

    private fun updateUIForError() {
        usernameTextView?.text = "User"
        profileImageView?.setImageResource(R.drawable.user)
        loadingProgress?.visibility = View.GONE
    }





    private fun getCurrentUserId(): String {
        return auth.currentUser?.uid ?: ""
    }

    private fun redirectToAiCenter() {
        val intent = Intent(requireActivity(), AiChatActivity::class.java)
        startActivity(intent)
    }

    private fun loadReminders() {
        val userId = getCurrentUserId()
        if (userId.isNotEmpty()) {
            viewModel.loadReminders(userId)
        } else {
            Timber.w("Cannot load reminders: userId is empty")
            updateRemindersVisibility(false)
        }
    }

    private fun setupSearchFunctionality() {
        // Handle search icon click
        binding.searchIcon.setOnClickListener {
            safeNavigateToMedicineSearch()
        }

        // Handle card click
        binding.searchCard.setOnClickListener {
            safeNavigateToMedicineSearch()
        }

        // Handle keyboard search action
        binding.searchAutoComplete.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                hideKeyboard(v)
                safeNavigateToMedicineSearch()
                true
            } else {
                false
            }
        }

        // Handle AutoCompleteTextView focus
        binding.searchAutoComplete.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                v.clearFocus()
                hideKeyboard(v)
                safeNavigateToMedicineSearch()
            }
        }
    }

    private fun safeNavigateToMedicineSearch() {
        try {
            if (!isAdded) {
                Timber.w("Fragment not attached to activity")
                return
            }
            
            val medicineSearchFragment = MedicineSearchFragment()
            
            // Set up the transition
            medicineSearchFragment.sharedElementEnterTransition = MaterialContainerTransform().apply {
                duration = 400L
                scrimColor = Color.TRANSPARENT
                setAllContainerColors(requireContext().getColor(android.R.color.white))
                fadeMode = MaterialContainerTransform.FADE_MODE_THROUGH
                interpolator = FastOutSlowInInterpolator()
            }
            
            // Set up exit transition for this fragment
            exitTransition = MaterialElevationScale(false).apply {
                duration = 400L
            }
            
            // Set up reenter transition for this fragment
            reenterTransition = MaterialElevationScale(true).apply {
                duration = 400L
            }
            
            // Perform the navigation with shared element transition
            requireActivity().supportFragmentManager
                .beginTransaction()
                .setReorderingAllowed(true)
                .addSharedElement(binding.searchCard, "search_container")
                .replace(R.id.fragment_container, medicineSearchFragment)
                .addToBackStack(null)
                .commit()
            
        } catch (e: Exception) {
            Timber.e(e, "Navigation failed")
            Toast.makeText(context, "Unable to open medicine search", Toast.LENGTH_SHORT).show()
        }
    }

    private fun hideKeyboard(view: View) {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    override fun onMedicineTaken(reminder: Reminder?) {
        reminder?.let {
            // Update the UI to show next day's time
            val reminderAdapter = binding.remindersRecyclerView.adapter as? ReminderAdapter
            reminderAdapter?.let { adapter ->
                val position = reminders.indexOf(reminder)
                if (position != -1) {
                    adapter.notifyItemChanged(position)
                }
            }
            
            // Schedule next reminder
            ReminderManager.getInstance(requireContext()).scheduleReminder(reminder)
        }
    }

    fun updateReminders(newReminders: List<Reminder>) {
        reminders = newReminders
        (binding.remindersRecyclerView.adapter as? ReminderAdapter)?.updateReminders(newReminders)
    }
}
