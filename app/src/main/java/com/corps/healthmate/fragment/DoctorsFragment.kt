package com.corps.healthmate.fragment

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.corps.healthmate.R
import com.corps.healthmate.activities.DoctorDetailActivity
import com.corps.healthmate.adapters.DoctorAdapter
import com.corps.healthmate.models.DoctorSummary
import com.corps.healthmate.models.TimeSlot
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.slider.RangeSlider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.GenericTypeIndicator
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Locale

class DoctorsFragment : Fragment(), DoctorAdapter.OnDoctorClickListener {
    private var searchBar: EditText? = null
    private var filterButton: ImageView? = null
    private var recyclerView: RecyclerView? = null
    private var chipGroup: ChipGroup? = null
    private var doctorAdapter: DoctorAdapter? = null
    private val doctorList: MutableList<DoctorSummary> = ArrayList()
    private val fullDoctorList: MutableList<DoctorSummary> = ArrayList()
    private val databaseReference = FirebaseDatabase.getInstance().getReference("doctors")
    private var valueEventListener: ValueEventListener? = null

    private lateinit var shimmerLayout: ShimmerFrameLayout
    private lateinit var mainContent: View

    // Filter criteria
    private var selectedSpecialization: String = "all"
    private var experienceRange: Pair<Float, Float> = Pair(0f, 50f)
    private var selectedDays: MutableSet<String> = mutableSetOf()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_doctor, container, false)

        shimmerLayout = view.findViewById(R.id.shimmer_layout)
        mainContent = view.findViewById(R.id.main_content)

        shimmerLayout.startShimmer()

        // Initialize UI components
        recyclerView = view.findViewById(R.id.doctors_recycler_view)
        searchBar = view.findViewById(R.id.search_bar)
        filterButton = view.findViewById(R.id.filter_button)
        chipGroup = view.findViewById(R.id.chip_group_specialization)

        recyclerView?.layoutManager = LinearLayoutManager(context)
        doctorAdapter = DoctorAdapter(requireContext(), doctorList, this)
        recyclerView?.adapter = doctorAdapter

        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Toast.makeText(requireContext(), "Please sign in to view doctors", Toast.LENGTH_SHORT).show()
            completeLoadingWithError()
        } else {
            fetchDoctors()
            loadData()
        }

        setupSearchBar()
        setupChipFilters()
        setupFilterButton()

        return view
    }

    private fun loadData() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {

            } catch (e: Exception) {
                Timber.e(e, "Error in loadData")
                completeLoadingWithError()
            }
        }
    }

    private fun fetchDoctors() {
        valueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val updatedDoctorList: MutableList<DoctorSummary> = ArrayList()
                if (snapshot.exists()) {
                    for (doctorSnapshot in snapshot.children) {
                        val id = doctorSnapshot.key
                        val name = doctorSnapshot.child("name").value?.toString()
                        val specialization = doctorSnapshot.child("specialization").value?.toString()
                        val experience = doctorSnapshot.child("experience").value?.toString() ?: "0"
                        val imageUrl = doctorSnapshot.child("profilePicture").value?.toString()
                        val education = doctorSnapshot.child("education").value?.toString()
                        val biography = doctorSnapshot.child("biography").value?.toString() ?: ""
                        val createdAt = doctorSnapshot.child("createdAt").value?.toString() ?: ""
                        val documentUrl = doctorSnapshot.child("documentUrl").value?.toString() ?: ""
                        val email = doctorSnapshot.child("email").value?.toString() ?: ""
                        val gender = doctorSnapshot.child("gender").value?.toString() ?: ""
                        val licenseNumber = doctorSnapshot.child("licenseNumber").value?.toString() ?: ""
                        val phone = doctorSnapshot.child("phone").value?.toString() ?: ""
                        val availableDays = doctorSnapshot.child("availableDays").getValue(object : GenericTypeIndicator<List<String>>() {}) ?: emptyList()
                        val languages = doctorSnapshot.child("languages").getValue(object : GenericTypeIndicator<List<String>>() {}) ?: emptyList()
                        val isVerified = doctorSnapshot.child("isVerified").getValue(Boolean::class.java) ?: false
                        val selectedTimeSlots = doctorSnapshot.child("selectedTimeSlots").children.mapNotNull { timeSlotSnapshot ->
                            TimeSlot(
                                day = timeSlotSnapshot.child("day").value?.toString() ?: "",
                                startTime = timeSlotSnapshot.child("startTime").value?.toString() ?: "",
                                endTime = timeSlotSnapshot.child("endTime").value?.toString() ?: "",
                                id = timeSlotSnapshot.child("id").value?.toString() ?: "",

                            )
                        }

                        val doctorSummary = DoctorSummary(
                            id = id, name = name, specialization = specialization, experience = experience,
                            imageUrl = imageUrl, education = education, availableDays = availableDays,
                            biography = biography, createdAt = createdAt, documentUrl = documentUrl,
                            email = email, gender = gender, languages = languages, licenseNumber = licenseNumber,
                            phone = phone, selectedTimeSlots = selectedTimeSlots, isVerified = isVerified
                        )
                        updatedDoctorList.add(doctorSummary)
                    }
                    fullDoctorList.clear()
                    fullDoctorList.addAll(updatedDoctorList)
                    filterDoctors(searchBar?.text.toString(), selectedSpecialization, experienceRange, selectedDays)
                    completeLoading() // Data is loaded, hide shimmer
                } else {
                    Toast.makeText(requireContext(), "No doctors available", Toast.LENGTH_SHORT).show()
                    completeLoadingWithError()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Timber.e(error.toException(), "Failed to load doctors")
                Toast.makeText(requireContext(), "Failed to load doctors", Toast.LENGTH_SHORT).show()
                completeLoadingWithError()
            }
        }
        valueEventListener?.let { databaseReference.addValueEventListener(it) }
    }

    private fun completeLoading() {
        shimmerLayout.stopShimmer()
        shimmerLayout.visibility = View.GONE
        mainContent.visibility = View.VISIBLE
        mainContent.alpha = 0f
        mainContent.animate()
            .alpha(1f)
            .setDuration(300)
            .start()
    }

    private fun completeLoadingWithError() {
        shimmerLayout.stopShimmer()
        shimmerLayout.visibility = View.GONE
        mainContent.visibility = View.VISIBLE
        mainContent.alpha = 0f
        mainContent.animate()
            .alpha(1f)
            .setDuration(300)
            .start()
    }

    private fun setupSearchBar() {
        searchBar?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                filterDoctors(s.toString(), selectedSpecialization, experienceRange, selectedDays)
            }
            override fun afterTextChanged(s: Editable) {}
        })
    }

    private fun setupChipFilters() {
        chipGroup?.setOnCheckedStateChangeListener { group, checkedIds ->
            val chipText = checkedIds.firstOrNull()?.let { id ->
                group.findViewById<Chip>(id)?.text?.toString()?.lowercase(Locale.getDefault())
            }
            selectedSpecialization = chipText ?: "all"
            filterDoctors(searchBar?.text.toString(), selectedSpecialization, experienceRange, selectedDays)
        }
    }

    private fun setupFilterButton() {
        filterButton?.setOnClickListener {
            showFilterDialog()
        }
    }

    private fun showFilterDialog() {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_filter_doctors)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        val specializationChipGroup = dialog.findViewById<ChipGroup>(R.id.specialization_chip_group)
        val specializations = listOf("All", "Cardiologist", "Dermatologist", "Neurologist") // Add more as needed
        specializations.forEach { spec ->
            val chip = Chip(requireContext()).apply {
                text = spec
                isCheckable = true
                setChipBackgroundColorResource(R.color.chip_background_selector)
                setTextColor(resources.getColorStateList(R.color.chip_text_selector, null))
                isChecked = spec.lowercase(Locale.getDefault()) == selectedSpecialization
            }
            specializationChipGroup.addView(chip)
        }

        val experienceSlider = dialog.findViewById<RangeSlider>(R.id.experience_slider)
        experienceSlider.values = listOf(experienceRange.first, experienceRange.second)

        val availabilityContainer = dialog.findViewById<LinearLayout>(R.id.availability_container)
        val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
        days.forEach { day ->
            val checkBox = CheckBox(requireContext()).apply {
                text = day
                isChecked = selectedDays.contains(day.lowercase(Locale.getDefault()))
            }
            availabilityContainer.addView(checkBox)
        }

        dialog.findViewById<MaterialButton>(R.id.cancel_button).setOnClickListener {
            dialog.dismiss()
        }

        dialog.findViewById<MaterialButton>(R.id.apply_button).setOnClickListener {
            selectedSpecialization = specializationChipGroup.findViewById<Chip>(specializationChipGroup.checkedChipId)?.text?.toString()?.lowercase(Locale.getDefault()) ?: "all"
            experienceRange = Pair(experienceSlider.values[0], experienceSlider.values[1])
            selectedDays.clear()
            availabilityContainer.children.filterIsInstance<CheckBox>().forEach { checkBox ->
                if (checkBox.isChecked) selectedDays.add(checkBox.text.toString().lowercase(Locale.getDefault()))
            }
            filterDoctors(searchBar?.text.toString(), selectedSpecialization, experienceRange, selectedDays)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun filterDoctors(query: String?, specialization: String, experienceRange: Pair<Float, Float>, selectedDays: Set<String>) {
        val filteredList = fullDoctorList.filter { doctor ->
            val matchesQuery = doctor.name?.lowercase(Locale.getDefault())?.contains(query?.lowercase(Locale.getDefault()) ?: "") == true ||
                    doctor.specialization?.lowercase(Locale.getDefault())?.contains(query?.lowercase(Locale.getDefault()) ?: "") == true
            val matchesSpecialization = specialization == "all" || doctor.specialization?.lowercase(Locale.getDefault()) == specialization
            val experience = doctor.experience?.toFloatOrNull() ?: 0f
            val matchesExperience = experience in experienceRange.first..experienceRange.second
            val matchesDays = selectedDays.isEmpty() || doctor.availableDays.any { it.lowercase(Locale.getDefault()) in selectedDays }
            matchesQuery && matchesSpecialization && matchesExperience && matchesDays
        }
        doctorList.clear()
        doctorList.addAll(filteredList)
        doctorAdapter?.updateDoctorList(filteredList)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        valueEventListener?.let { databaseReference.removeEventListener(it) }
        searchBar = null
        filterButton = null
        recyclerView = null
        chipGroup = null
        doctorAdapter = null
        shimmerLayout.stopShimmer()
    }

    override fun onDoctorClick(doctor: DoctorSummary?) {
        if (doctor == null) {
            Timber.tag("DoctorsFragment").e("Doctor is null")
            return
        }
        val intent = Intent(context, DoctorDetailActivity::class.java)
        intent.putExtra("doctorId", doctor.id)
        startActivity(intent)
    }
}