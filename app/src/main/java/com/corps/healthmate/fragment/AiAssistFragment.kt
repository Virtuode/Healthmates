package com.corps.healthmate.fragment


import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
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
import com.bumptech.glide.request.target.Target
import com.corps.healthmate.R
import com.corps.healthmate.activities.AiChatActivity
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
import com.corps.healthmate.utils.ReminderCreationHelper
import com.corps.healthmate.viewmodel.AiAssistViewModel
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Calendar
import java.util.concurrent.TimeUnit
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import com.corps.healthmate.utils.FirebaseReferenceManager
import com.google.android.material.transition.MaterialContainerTransform
import com.google.android.material.transition.MaterialElevationScale
import android.graphics.Color
import android.graphics.Rect
import android.widget.ScrollView
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.recyclerview.widget.PagerSnapHelper
import com.corps.healthmate.activities.AppointmentHistoryActivity
import com.corps.healthmate.activities.DoctorDetailActivity
import com.corps.healthmate.adapters.AppointmentAdapter
import com.corps.healthmate.models.Appointment
import com.corps.healthmate.models.DisplayAppointment
import com.corps.healthmate.models.Doctor
import com.corps.healthmate.utils.ReminderManager
import com.facebook.shimmer.ShimmerFrameLayout

import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

class AiAssistFragment : Fragment(), OnReminderSavedListener,
    ReminderAdapter.OnReminderClickListener, TimeDifferenceCallback {
    private var _binding: FragmentAiAssistBinding? = null
    private val binding get() = _binding!!
    private lateinit var remindersRecyclerViews: RecyclerView
    private lateinit var reminderAdapter: ReminderAdapter
    private lateinit var appointmentsRecyclerView: RecyclerView
    private lateinit var appointmentAdapter: AppointmentAdapter
    private lateinit var viewModel: AiAssistViewModel
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var pulseAnimation: Animation
    private var chatActionButton: ImageButton? = null
    private var animationView: LottieAnimationView? = null
    private var viewPager: ViewPager2? = null
    private var tabLayout: TabLayout? = null
    private val sliderAdapter: SliderAdapter by lazy { SliderAdapter(requireContext()) }
    private var currentSlideIndex = 0
    private val slider = 3000
    private var sliderHandler: Handler? = null
    private var sliderRunnable: Runnable? = null
    private var usernameTextView: TextView? = null
    private var profileImageView: CircleImageView? = null
    private var loadingProgress: ProgressBar? = null
    private var imageMessage: ImageView? = null
    private var repository: ReminderRepository? = null
    private var createReminderButton: TextView? = null
    private val auth = FirebaseAuth.getInstance()
    private val menuDialog = MenuDialog()
    private var cardAiClick: MaterialCardView? = null
    private var reminders: List<Reminder> = emptyList()
    private var displayAppointments: List<DisplayAppointment> = emptyList()
    private lateinit var patientId: String
    private var historyIcon: TextView? = null


    companion object {

        private const val TAG = "AiAssistFragment"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val factory =
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        viewModel = ViewModelProvider(this, factory)[AiAssistViewModel::class.java]
        checkAuthenticationState()
        patientId = FirebaseAuth.getInstance().currentUser?.uid ?: ""


    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAiAssistBinding.inflate(inflater, container, false)



        remindersRecyclerViews = binding.remindersRecyclerView
        repository = ReminderRepository(requireActivity().application)
        notificationHelper = NotificationHelper(requireContext())


        if (remindersRecyclerViews.adapter == null) {
            remindersRecyclerViews.layoutManager = LinearLayoutManager(context)
            reminderAdapter = ReminderAdapter(listener = this, callback = this)
            remindersRecyclerViews.adapter = reminderAdapter
        }

        appointmentsRecyclerView = binding.appointmentsRecyclerView
        appointmentsRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        appointmentAdapter = AppointmentAdapter(emptyList(), Date()) { appointment ->
            if (appointment.status == Appointment.STATUS_MISSED) redirectToDoctorDetail(appointment)
        }
        appointmentsRecyclerView.adapter = appointmentAdapter
        fetchAppointments(FirebaseAuth.getInstance().currentUser?.uid ?: "")

        val snapHelper = PagerSnapHelper()
        snapHelper.attachToRecyclerView(appointmentsRecyclerView)
        appointmentsRecyclerView.addItemDecoration(CardScaleItemDecoration())

        viewPager = binding.viewPager
        tabLayout = binding.tabLayout
        viewPager?.adapter = sliderAdapter
        TabLayoutMediator(tabLayout!!, viewPager!!) { _, _ -> }.attach()


        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            viewModel.allReminders.observe(viewLifecycleOwner) { reminders ->
                lifecycleScope.launch(Dispatchers.Main) {
                    val activeReminders = reminders.filter { it.isActive }
                    reminderAdapter.updateReminders(activeReminders)
                    remindersRecyclerViews.visibility = if (activeReminders.isNotEmpty()) View.VISIBLE else View.GONE
                }
            }
            fetchAppointments(userId)
        } else {
            Timber.tag(TAG).e("User ID is null, cannot fetch reminders or appointments")
            remindersRecyclerViews.visibility = View.GONE
            appointmentsRecyclerView.visibility = View.GONE
        }



        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        try {
            viewModel = ViewModelProvider(this)[AiAssistViewModel::class.java]
            pulseAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.pulse)

            remindersRecyclerViews = binding.remindersRecyclerView
            remindersRecyclerViews.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            reminderAdapter = ReminderAdapter(listener = this, callback = this)
            remindersRecyclerViews.adapter = reminderAdapter


            setupViews()
            setupObservers()
            setupClickListeners()
            setupSlider()
            loadUserData()
            setupSearchFunctionality()


            val userId = getCurrentUserId()
            if (userId.isNotEmpty()) {
                viewModel.loadReminders(userId)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error in onViewCreated")
        }


    }

    private fun fetchAppointments(patientId: String) {
        val currentDate = Date()
        val currentDateFormatted = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(currentDate)
        val appointmentsRef = FirebaseDatabase.getInstance().reference.child("patients").child(patientId).child("appointments").orderByChild("date").startAt(currentDateFormatted)

        appointmentsRef.addValueEventListener(object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                lifecycleScope.launch {
                    val appointmentsList = mutableListOf<DisplayAppointment>()
                    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

                    for (child in snapshot.children) {
                        val appointment = child.getValue(Appointment::class.java)?.apply { javaClass.getDeclaredField("id").apply { isAccessible = true }.set(this, child.key) } ?: continue
                        if (appointment.patientId != patientId) continue

                        val doctorSnapshot = FirebaseDatabase.getInstance().reference.child("doctors").child(appointment.doctorId).get().await()
                        val doctorImageUrl = doctorSnapshot.child("profilePicture").getValue(String::class.java) ?: ""
                        val doctorName = doctorSnapshot.child("name").getValue(String::class.java)

                        val chatSnapshot = FirebaseDatabase.getInstance().reference.child("chats").orderByChild("appointmentId").equalTo(appointment.id).get().await()
                        val chatId = chatSnapshot.children.firstOrNull()?.key ?: createChat(appointment)

                        val startDateTime = sdf.parse("${appointment.date} ${appointment.startTime}")
                        val endDateTime = sdf.parse("${appointment.date} ${appointment.endTime}")
                        val now = currentDate.time
                        val missedThreshold = now - (24 * 60 * 60 * 1000)

                        val updatedAppointment = when {
                            appointment.status == Appointment.STATUS_CONFIRMED && startDateTime?.before(currentDate) == true && endDateTime?.after(currentDate) == true -> appointment.copy(status = Appointment.STATUS_ONGOING)
                            appointment.status == Appointment.STATUS_CONFIRMED && endDateTime?.time ?: 0 > missedThreshold && endDateTime?.before(currentDate) == true -> appointment.copy(status = Appointment.STATUS_MISSED)
                            else -> appointment
                        }

                        if (startDateTime?.after(currentDate) == true || updatedAppointment.status in listOf(Appointment.STATUS_ONGOING, Appointment.STATUS_MISSED, Appointment.STATUS_PENDING)) {
                            appointmentsList.add(DisplayAppointment(updatedAppointment, doctorImageUrl, doctorName, chatId))
                        }
                    }

                    displayAppointments = appointmentsList.sortedWith(compareBy({ it.appointment.date }, { it.appointment.startTime }))
                    appointmentAdapter.updateAppointments(displayAppointments, currentDate)
                    appointmentsRecyclerView.visibility = if (displayAppointments.isNotEmpty()) View.VISIBLE else View.GONE
                }
            }

            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                Timber.e(error.toException(), "Failed to fetch appointments")
                appointmentsRecyclerView.visibility = View.GONE
            }
        })

        listenForChatUpdates(patientId)
    }

    private fun createChat(appointment: Appointment): String {
        val db = FirebaseDatabase.getInstance().reference
        val chatRef = db.child("chats").push()
        val chatId = chatRef.key ?: return ""
        chatRef.setValue(mapOf("appointmentId" to appointment.id, "doctorId" to appointment.doctorId, "patientId" to appointment.patientId, "active" to true, "videoCallInitiated" to false))
        return chatId
    }

    private fun listenForChatUpdates(patientId: String) {
        FirebaseDatabase.getInstance().reference.child("chats").addValueEventListener(object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                lifecycleScope.launch {
                    val updatedAppointments = displayAppointments.map { displayAppt ->
                        val chatSnapshot = snapshot.children.firstOrNull { it.child("appointmentId").getValue(String::class.java) == displayAppt.appointment.id }
                        val chatId = chatSnapshot?.key ?: displayAppt.chatId
                        displayAppt.copy(chatId = chatId)
                    }
                    displayAppointments = updatedAppointments
                    appointmentAdapter.updateAppointments(displayAppointments, Date())
                }
            }

            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                Timber.e(error.toException(), "Failed to listen for chat updates")
            }
        })
    }



    private fun checkDataLoadingComplete() {
        // Check if all required data is loaded
        if (viewModel.allReminders.value != null && displayAppointments.isNotEmpty()) {
        }
    }



    private fun redirectToDoctorDetail(appointment: Appointment) {
        lifecycleScope.launch {
            try {
                val doctorSnapshot = FirebaseDatabase.getInstance().reference
                    .child("doctors")
                    .child(appointment.doctorId)
                    .get()
                    .await()
                val doctor = doctorSnapshot.getValue(Doctor::class.java) // Assuming a Doctor model
                if (doctor != null) {
                    val intent = Intent(requireActivity(), DoctorDetailActivity::class.java).apply {
                        putExtra("doctorId", doctor.id)
                        putExtra("originalAppointmentId", appointment.id)
                    }
                    startActivity(intent)
                    Timber.d("Redirected to DoctorDetailActivity for doctor: ${doctor.name}")
                } else {
                    Toast.makeText(context, "Doctor details not found", Toast.LENGTH_SHORT).show()
                    Timber.e("Doctor not found for ID: ${appointment.doctorId}")
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error loading doctor details", Toast.LENGTH_SHORT).show()
                Timber.e(e, "Failed to redirect to DoctorDetailActivity")
            }
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
        historyIcon = binding.historyIcon

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

        historyIcon?.setOnClickListener {
            startActivity(Intent(requireContext(), AppointmentHistoryActivity::class.java))
        }

        // Chat action button
        chatActionButton?.setOnClickListener {
            val intent = Intent(requireContext(), AiChatActivity::class.java)
            val options = ActivityOptions.makeSceneTransitionAnimation(
                requireActivity(),
                chatActionButton,
                "chat_transition"
            )
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = requireContext().getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                requireContext().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))


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
        appointmentsRecyclerView.adapter = null
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
            sliderHandler?.postDelayed(sliderRunnable!!, slider.toLong())
        }
        sliderHandler?.postDelayed(sliderRunnable!!, slider.toLong())
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
                        notificationHelper.showWarningPopup(pillNames)
                    }
                }
            }

        } catch (e: Exception) {
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
                        checkDataLoadingComplete()
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
            val greeting = createTimeBasedGreeting(userData.greetingName)
            usernameTextView?.text = greeting
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

    private fun updateUIForNoUser() {
        usernameTextView?.text = context?.getString(R.string.user_label)

        profileImageView?.setImageResource(R.drawable.user)
        loadingProgress?.visibility = View.GONE
    }

    private fun updateUIForNoData(user: FirebaseUser) {
        usernameTextView?.text = user.displayName ?: "User"
        profileImageView?.setImageResource(R.drawable.user)
        loadingProgress?.visibility = View.GONE
    }

    private fun updateUIForError() {
        usernameTextView?.text = context?.getString(R.string.user_label)

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

    inner class CardScaleItemDecoration : RecyclerView.ItemDecoration() {
        private val scaleFactor = 0.1f // Scale factor for non-focused cards
        private val padding = 16 // Padding between cards in dp

        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State
        ) {
            val position = parent.getChildAdapterPosition(view)
            val itemCount = parent.adapter?.itemCount ?: 0

            outRect.left = padding
            outRect.right = padding

            if (position == 0) {
                outRect.left = 2 * padding // Extra padding on the first item
            }
            if (position == itemCount - 1) {
                outRect.right = 2 * padding // Extra padding on the last item
            }
        }

        override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
            super.onDrawOver(c, parent, state)

            val centerX = parent.width / 2f
            val childCount = parent.childCount

            for (i in 0 until childCount) {
                val child = parent.getChildAt(i)
                val childCenterX = (child.left + child.right) / 2f
                val distanceFromCenter = abs(centerX - childCenterX)
                val maxDistance = parent.width / 2f + padding
                val scale = 1f - (scaleFactor * (distanceFromCenter / maxDistance)).coerceAtMost(1f)

                child.scaleX = scale
                child.scaleY = scale

                // Adjust translation to keep cards centered
                val translationX = (1 - scale) * child.width / 2
                if (childCenterX < centerX) {
                    child.translationX = translationX
                } else {
                    child.translationX = -translationX
                }
            }
        }
    }


}
