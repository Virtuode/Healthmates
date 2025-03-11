package com.corps.healthmate.fragment

import com.corps.healthmate.interfaces.SurveyDataProvider
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import com.corps.healthmate.R
import com.corps.healthmate.databinding.FragmentBasicInfoBinding

import com.corps.healthmate.utils.CloudinaryHelper
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.corps.healthmate.utils.ImagePickerHelper

@AndroidEntryPoint
class BasicInfoFragment : Fragment(), SurveyDataProvider {
    private var _binding: FragmentBasicInfoBinding? = null
    private val binding get() = _binding ?: throw IllegalStateException("Binding is null. Is the view visible?")

    private var imageUri: Uri? = null
    private var cloudinaryImageUrl: String? = null

    // Cached values
    private var cachedFirstName: String = ""
    private var cachedMiddleName: String = ""
    private var cachedLastName: String = ""
    private var cachedAge: String = ""
    private var cachedGender: String = ""
    private var cachedContact: String = ""
    private var cachedHeight: String = ""
    private var cachedWeight: String = ""

    private lateinit var imagePickerHelper: ImagePickerHelper

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBasicInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupGenderDropdown()
        setupTextWatchers()
        setupImagePicker()
        setupInputListeners()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        imagePickerHelper = ImagePickerHelper { uri ->
            if (uri != null) {
                imageUri = uri
                binding.profileImage.setImageURI(uri)
                uploadImageToCloudinary()
            }
        }
        imagePickerHelper.register(this)
    }

    private fun setupGenderDropdown() {
        val genders = arrayOf("Male", "Female", "Other")
        val adapter = ArrayAdapter(requireContext(), R.layout.dropdown_item, genders)
        binding.genderInput.setAdapter(adapter)
    }

    private fun setupTextWatchers() {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {}
        }

        with(binding) {
            FirstName.addTextChangedListener(textWatcher)
            MiddleName.addTextChangedListener(textWatcher)
            LastName.addTextChangedListener(textWatcher)
            ageInput.addTextChangedListener(textWatcher)
            genderInput.addTextChangedListener(textWatcher)
            contactInput.addTextChangedListener(textWatcher)
            heightInput.addTextChangedListener(textWatcher)
            weightInput.addTextChangedListener(textWatcher)
        }
    }

    private fun setupInputListeners() {
        // Name fields
        binding.FirstName.addTextChangedListener { 
            cachedFirstName = it?.toString()?.trim() ?: ""
        }
        binding.MiddleName.addTextChangedListener {
            cachedMiddleName = it?.toString()?.trim() ?: ""
        }
        binding.LastName.addTextChangedListener {
            cachedLastName = it?.toString()?.trim() ?: ""
        }

        // Age and gender
        binding.ageInput.addTextChangedListener {
            cachedAge = it?.toString()?.trim() ?: ""
        }
        binding.genderInput.addTextChangedListener {
            cachedGender = it?.toString()?.trim() ?: ""
        }

        // Contact
        binding.contactInput.addTextChangedListener {
            cachedContact = it?.toString()?.trim() ?: ""
        }

        // Height and weight
        binding.heightInput.addTextChangedListener {
            cachedHeight = it?.toString()?.trim() ?: ""
        }
        binding.weightInput.addTextChangedListener {
            cachedWeight = it?.toString()?.trim() ?: ""
        }
    }

    private fun setupImagePicker() {
        binding.fabCamera.setOnClickListener { openGallery() }
    }

    override fun getSurveyData(): Map<String, Any> {
        return mapOf(
            "basicInfo" to mapOf(
                "firstName" to cachedFirstName,
                "middleName" to cachedMiddleName,
                "lastName" to cachedLastName,
                "age" to (cachedAge.toIntOrNull() ?: 0),
                "gender" to cachedGender,
                "contactNumber" to cachedContact,
                "height" to (cachedHeight.toFloatOrNull() ?: 0f),
                "weight" to (cachedWeight.toFloatOrNull() ?: 0f),
                "imageUrl" to cloudinaryImageUrl
            )
        )
    }

    override fun isDataValid(): Boolean {
        return cachedFirstName.isNotEmpty() &&
               cachedLastName.isNotEmpty() &&
               cachedAge.isNotEmpty() &&
               cachedGender.isNotEmpty() &&
               cachedContact.isNotEmpty() &&
               cachedHeight.isNotEmpty() &&
               cachedWeight.isNotEmpty() &&
               validateNumericFields()
    }

    private fun validateNumericFields(): Boolean {
        return try {
            val age = cachedAge.toInt()
            val height = cachedHeight.toFloat()
            val weight = cachedWeight.toFloat()
            age > 0 && height > 0f && weight > 0f
        } catch (e: NumberFormatException) {
            false
        }
    }

    override fun loadExistingData(data: Map<String, Any?>) {
        val basicInfo = data["basicInfo"] as? Map<*, *> ?: return
        
        // Update cached values
        cachedFirstName = (basicInfo["firstName"] as? String) ?: ""
        cachedMiddleName = (basicInfo["middleName"] as? String) ?: ""
        cachedLastName = (basicInfo["lastName"] as? String) ?: ""
        cachedAge = (basicInfo["age"] as? Int)?.toString() ?: ""
        cachedGender = (basicInfo["gender"] as? String) ?: ""
        cachedContact = (basicInfo["contactNumber"] as? String) ?: ""
        cachedHeight = (basicInfo["height"] as? Float)?.toString() ?: ""
        cachedWeight = (basicInfo["weight"] as? Float)?.toString() ?: ""
        cloudinaryImageUrl = basicInfo["imageUrl"] as? String

        // Update UI if binding is available
        _binding?.let { binding ->
            binding.FirstName.setText(cachedFirstName)
            binding.MiddleName.setText(cachedMiddleName)
            binding.LastName.setText(cachedLastName)
            binding.ageInput.setText(cachedAge)
            binding.genderInput.setText(cachedGender)
            binding.contactInput.setText(cachedContact)
            binding.heightInput.setText(cachedHeight)
            binding.weightInput.setText(cachedWeight)
            
            // Load image if URL exists
            cloudinaryImageUrl?.let { url ->
                // Implement image loading logic here
            }
        }
    }

    // Image handling methods
    private fun openGallery() {
        imagePickerHelper.pickImage()
    }

    private fun uploadImageToCloudinary() {
        val user = FirebaseAuth.getInstance().currentUser ?: run {
            Toast.makeText(context, "User not authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        imageUri?.let { uri ->
            context?.let { ctx ->
                CloudinaryHelper.init(ctx)
                binding.fabCamera.isEnabled = false
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        cloudinaryImageUrl = CloudinaryHelper.uploadProfileImage(uri, user.uid)
                        Toast.makeText(context, "Profile image uploaded successfully", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Log.e("BasicInfoFragment", "Failed to upload image: ${e.message}")
                        Toast.makeText(context, "Failed to upload image", Toast.LENGTH_SHORT).show()
                    } finally {
                        binding.fabCamera.isEnabled = true
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = BasicInfoFragment()
        private const val PICK_IMAGE_REQUEST = 1
    }
}
