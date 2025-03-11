package com.corps.healthmate.fragment

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.corps.healthmate.R
import com.corps.healthmate.activities.DoctorDetailActivity
import com.corps.healthmate.adapters.DoctorAdapter
import com.corps.healthmate.models.DoctorSummary
import com.corps.healthmate.models.TimeSlot
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.GenericTypeIndicator
import com.google.firebase.database.ValueEventListener
import java.util.Locale

class DoctorsFragment : Fragment(), DoctorAdapter.OnDoctorClickListener {
    private var searchBar: EditText? = null
    private var filterButton: ImageView? = null
    private var recyclerView: RecyclerView? = null
    private var chipGroup: ChipGroup? = null
    private var doctorAdapter: DoctorAdapter? = null
    private val doctorList: MutableList<DoctorSummary> = ArrayList()
    private val fullDoctorList: MutableList<DoctorSummary> = ArrayList() // Store full list for filtering
    private val databaseReference = FirebaseDatabase.getInstance().getReference("doctors")
    private var valueEventListener: ValueEventListener? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_doctor, container, false)

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
        } else {
            fetchDoctors()
        }
        setupSearchBar()
        setupChipFilters()

        return view
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
                                isAvailable = timeSlotSnapshot.child("isAvailable").getValue(Boolean::class.java) ?: true
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
                } else {
                    Toast.makeText(requireContext(), "No doctors available", Toast.LENGTH_SHORT).show()
                }
                fullDoctorList.clear()
                fullDoctorList.addAll(updatedDoctorList)
                doctorList.clear()
                doctorList.addAll(updatedDoctorList)
                doctorAdapter?.updateDoctorList(updatedDoctorList)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("DoctorsFragment", "Database error: ${error.message}")
                Toast.makeText(requireContext(), "Failed to load doctors", Toast.LENGTH_SHORT).show()
            }
        }
        valueEventListener?.let { databaseReference.addValueEventListener(it) }
    }

    private fun setupSearchBar() {
        searchBar?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                filterDoctors(s.toString(), getSelectedSpecialization())
            }
            override fun afterTextChanged(s: Editable) {}
        })
    }

    private fun setupChipFilters() {
        chipGroup?.setOnCheckedChangeListener { group, checkedId ->
            val chip = group.findViewById<Chip>(checkedId)
            val specialization = chip?.text?.toString()?.lowercase(Locale.getDefault()) ?: "all"
            filterDoctors(searchBar?.text.toString() ?: "", specialization)
        }
    }

    private fun getSelectedSpecialization(): String {
        val checkedChipId = chipGroup?.checkedChipId
        val chip = checkedChipId?.let { chipGroup?.findViewById<Chip>(it) }
        return chip?.text?.toString()?.lowercase(Locale.getDefault()) ?: "all"
    }

    private fun filterDoctors(query: String, specialization: String) {
        val filteredList = fullDoctorList.filter { doctor ->
            val matchesQuery = doctor.name?.lowercase(Locale.getDefault())?.contains(query.lowercase(Locale.getDefault())) == true ||
                    doctor.specialization?.lowercase(Locale.getDefault())?.contains(query.lowercase(Locale.getDefault())) == true
            val matchesSpecialization = specialization == "all" || doctor.specialization?.lowercase(Locale.getDefault()) == specialization
            matchesQuery && matchesSpecialization
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
    }

    override fun onDoctorClick(doctor: DoctorSummary?) {
        if (doctor == null) {
            Log.e("DoctorsFragment", "Doctor is null")
            return
        }
        val intent = Intent(context, DoctorDetailActivity::class.java)
        intent.putExtra("doctor", doctor)
        startActivity(intent)
    }
}