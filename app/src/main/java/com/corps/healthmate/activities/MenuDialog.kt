package com.corps.healthmate.activities

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.corps.healthmate.R
import com.corps.healthmate.utils.CloudinaryHelper
import com.google.android.material.chip.Chip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import com.corps.healthmate.utils.ImagePickerHelper

import timber.log.Timber

class MenuDialog : DialogFragment() {

    private var profileImageView: CircleImageView? = null
    private var menuUsername: TextView? = null
    private var menuAge: TextView? = null
    private var menuBloodGroup: TextView? = null
    private var menuContact: TextView? = null
    private var menuEmail: TextView? = null
    private var mAuth: FirebaseAuth? = null
    private var imageUri: Uri? = null
    private var logoutClickListener: (() -> Unit)? = null
    private var uploadJob: Job? = null
    private var onImageUploadedListener: (() -> Unit)? = null
    private lateinit var imagePickerHelper: ImagePickerHelper

    override fun onAttach(context: Context) {
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize and register ImagePickerHelper during fragment creation
        imagePickerHelper = ImagePickerHelper { uri ->
            if (uri != null) {
                imageUri = uri
                uploadImage(uri)
            }
        }
        imagePickerHelper.register(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Make dialog background transparent
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // Set dialog width to 90% of screen width
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val view = inflater.inflate(R.layout.menudialog, container, false)

        // Initialize views
        profileImageView = view.findViewById(R.id.menu_profile_image)
        menuUsername = view.findViewById(R.id.menu_username)
        menuAge = view.findViewById(R.id.menu_age)
        menuBloodGroup = view.findViewById(R.id.menu_blood_group)
        menuContact = view.findViewById(R.id.menu_contact)
        menuEmail = view.findViewById(R.id.menu_email)

        mAuth = FirebaseAuth.getInstance()

        view.findViewById<LinearLayout>(R.id.settings_button).setOnClickListener {
            // Navigate to settings
            val intent = Intent(requireContext(), ProfileEditActivity::class.java)
            startActivity(intent)
            dismiss()
        }

        view.findViewById<LinearLayout>(R.id.help_button).setOnClickListener {
            // Navigate to help & feedback
            Toast.makeText(requireContext(), "Help & Feedback coming soon!", Toast.LENGTH_SHORT)
                .show()
        }

        // Set click listener for profile image
        profileImageView?.setOnClickListener {
            openGallery()
        }

        // Set click listener for logout button
        view.findViewById<LinearLayout>(R.id.logout_text).setOnClickListener {
            logoutClickListener?.invoke()
            // Show confirmation dialog
            AlertDialog.Builder(requireContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes") { _, _ ->
                    // Perform logout
                    FirebaseAuth.getInstance().signOut()

                    // Redirect to LoginActivity
                    val intent = Intent(requireContext(), LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)

                    // Close the dialog and finish the current activity
                    dismiss()
                }
                .setNegativeButton("No", null)
                .show()
        }

        loadUserData()
        return view
    }

    private fun openGallery() {
        imagePickerHelper.pickImage()
    }

    private fun uploadImage(imageUri: Uri) {
        val userId = mAuth?.currentUser?.uid
        if (userId == null) {
            showToast("User not authenticated")
            return
        }

        // Check if fragment is attached
        if (!isAdded) {
            return
        }

        // Show loading indicator
        showToast("Uploading image...")

        // Initialize Cloudinary
        CloudinaryHelper.init(requireContext())

        // Launch coroutine for image upload
        uploadJob = CoroutineScope(Dispatchers.Main).launch {
            try {
                val imageUrl = CloudinaryHelper.uploadProfileImage(imageUri, userId)
                
                // Check if fragment is still attached
                if (!isAdded) return@launch
                
                // Update image URL in patient's survey node
                val surveyReference = FirebaseDatabase.getInstance()
                    .getReference("patients")
                    .child(userId)
                    .child("survey")

                surveyReference.child("imageUrl").setValue(imageUrl)
                    .addOnSuccessListener {
                        if (!isAdded) return@addOnSuccessListener
                        
                        // Update profile image
                        profileImageView?.let { imageView ->
                            Glide.with(requireContext())
                                .load(imageUrl)
                                .placeholder(R.drawable.baseline_person_24)
                                .error(R.drawable.user)
                                .into(imageView)
                        }

                        showToast("Profile image updated successfully")
                        
                        // Notify the fragment that image was updated
                        onImageUploadedListener?.invoke()
                    }
                    .addOnFailureListener { e ->
                        // Check if fragment is still attached
                        if (!isAdded) return@addOnFailureListener
                        
                        Timber.e("Failed to update database", e)
                        showToast("Failed to update profile image in database")
                    }
            } catch (e: Exception) {
                // Check if fragment is still attached
                if (!isAdded) return@launch
                
                Timber.e("Failed to upload image", e)
                showToast("Failed to update profile image: ${e.message}")
            }
        }
    }

    // Add helper method for showing toasts
    private fun showToast(message: String) {
        if (isAdded) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadUserData() {
        val userId = mAuth?.currentUser?.uid ?: return

        // Get reference to user data
        val userRef = FirebaseDatabase.getInstance().reference
            .child("patients")
            .child(userId)

        // Enable disk persistence for this reference
        userRef.keepSynced(true)

        // Use a single query to fetch all data
        userRef.get().addOnCompleteListener { task ->
            if (!isAdded) return@addOnCompleteListener

            if (!task.isSuccessful) {
                Timber.tag("MenuDialog").e(task.exception, "Error fetching user data")
                updateUIForError()
                return@addOnCompleteListener
            }

            val snapshot = task.result
            if (!snapshot.exists()) {
                Timber.tag("MenuDialog").d("No user data found")
                updateUIForNoData()
                return@addOnCompleteListener
            }

            try {
                // Use extension function for efficient parsing
                val userData = snapshot.toUserData()
                updateUserInterface(userData)
                
                // Load image in parallel
                lifecycleScope.launch(Dispatchers.Main) {
                    loadProfileImage(userData.profileImageUrl)
                }
            } catch (e: Exception) {
                Timber.tag("MenuDialog").e(e, "Error parsing user data")
                updateUIForError()
            }
        }
    }

    // Extension function for efficient data parsing
    private fun DataSnapshot.toUserData(): UserData {
        val survey = child("survey")
        val basicInfo = survey.child("basicInfo")
        val bloodGroupInfo = survey.child("bloodGroup")

        return UserData(
            fullName = buildFullName(
                basicInfo.child("firstName").getValue(String::class.java) ?: "",
                basicInfo.child("middleName").getValue(String::class.java) ?: "",
                basicInfo.child("lastName").getValue(String::class.java) ?: ""
            ),
            ageGroup = basicInfo.child("age").getValue(Long::class.java)?.toString() ?: "Not specified",
            bloodGroup = bloodGroupInfo.child("bloodGroup").getValue(String::class.java) ?: "Not specified",
            contactNumber = basicInfo.child("contactNumber").getValue(String::class.java) ?: "Not specified",
            profileImageUrl = basicInfo.child("imageUrl").getValue(String::class.java) 
                ?: child("imageUrl").getValue(String::class.java),
        )
    }

    private data class UserData(
        val fullName: String,
        val ageGroup: String,
        val bloodGroup: String,
        val contactNumber: String,
        val profileImageUrl: String?

    )

    private fun buildFullName(firstName: String, middleName: String, lastName: String): String {
        return when {
            firstName.isNotEmpty() && middleName.isNotEmpty() && lastName.isNotEmpty() -> 
                "$firstName $middleName $lastName"
            firstName.isNotEmpty() && lastName.isNotEmpty() -> 
                "$firstName $lastName"
            firstName.isNotEmpty() -> firstName
            else -> "User"
        }
    }

    private fun updateUserInterface(userData: UserData) {
        view?.post {
            menuUsername?.text = userData.fullName
            context?.let { ctx ->
                menuAge?.text = userData.ageGroup
                menuBloodGroup?.text = userData.bloodGroup
                menuContact?.text = userData.contactNumber


            }
            menuEmail?.text = mAuth?.currentUser?.email ?: "No email"
        }
    }

    private fun loadProfileImage(profileImageUrl: String?) {
        if (profileImageUrl.isNullOrEmpty()) {
            Timber.tag("MenuDialog").d("No profile image URL found")
            profileImageView?.setImageResource(R.drawable.user)
            return
        }

        val safeImageUrl = profileImageUrl.replace("http://", "https://")
        Timber.tag("MenuDialog").d("Loading image from URL: $safeImageUrl")

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
                        Timber.tag("MenuDialog").e(e, "Failed to load image from $safeImageUrl")
                        profileImageView?.setImageResource(R.drawable.user)
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: Target<Drawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        Timber.tag("MenuDialog").d("Image loaded successfully from $safeImageUrl")
                        return false
                    }
                })
                .into(profileImageView ?: return)
        }
    }

    private fun updateUIForNoData() {
        menuUsername?.text = "User"
        menuAge?.text = getString(R.string.age_label, "Not specified")
        menuBloodGroup?.text = getString(R.string.blood_group_label, "Not specified")
        menuContact?.text = getString(R.string.lifestyle_label, "Not specified")
        menuEmail?.text = mAuth?.currentUser?.email ?: "No email"
        profileImageView?.setImageResource(R.drawable.user)
    }

    private fun updateUIForError() {
        Timber.tag("MenuDialog").e("Error updating UI")
        updateUIForNoData()
    }

    override fun onStart() {
        super.onStart()
        dialog?.let { dialog ->
            val screenWidth = resources.displayMetrics.widthPixels
            val margin = (screenWidth * 0.05).toInt() // 5% margin on each side
            val width = screenWidth - (margin * 2) // Adjust width to leave space on both sides

            dialog.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)

            // Remove default dialog background for a clean look
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
    }

    override fun onDestroyView() {
        uploadJob?.cancel() // Cancel any ongoing upload
        super.onDestroyView()
    }

    fun setOnImageUploadedListener(listener: () -> Unit) {
        onImageUploadedListener = listener
    }
}
