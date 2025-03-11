package com.corps.healthmate.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.corps.healthmate.R
import com.corps.healthmate.adapters.MedicineSearchAdapter
import com.corps.healthmate.databinding.FragmentMedicineSearchBinding
import com.corps.healthmate.models.Medicine
import com.corps.healthmate.viewmodel.MedicineSearchViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import com.corps.healthmate.utils.MedicineDataLoader

@AndroidEntryPoint
class MedicineSearchFragment : Fragment() {
    private var _binding: FragmentMedicineSearchBinding? = null
    private val binding get() = _binding!!
    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var searchAdapter: MedicineSearchAdapter
    private var isLoading = false
    private val viewModel: MedicineSearchViewModel by viewModels()
    private lateinit var medicineDataLoader: MedicineDataLoader

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMedicineSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        medicineDataLoader = MedicineDataLoader(requireContext())
        
        setupRecyclerView()
        setupSearch()
        setupToolbar()
        setupObservers()
        
        // Check and load medicines data
        checkAndLoadMedicinesData()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationIcon(R.drawable.baseline_arrow_back_24)
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }
    }

    private fun setupRecyclerView() {
        searchAdapter = MedicineSearchAdapter(
            onMedicineClick = { medicine ->
                showMedicineDetails(medicine)
            },
            onLoadMore = {
                viewModel.loadMore()
            }
        )

        binding.medicinesRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = searchAdapter
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.medicines.collect { medicines ->
                searchAdapter.submitList(medicines)
                searchAdapter.setLoading(false)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                binding.progressBar.isVisible = isLoading
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.error.collect { error ->
                error?.let { showError(it) }
            }
        }
    }

    private fun setupSearch() {
        binding.searchEditText.setOnEditorActionListener { _, _, _ ->
            viewModel.searchMedicines(binding.searchEditText.text.toString())
            true
        }
    }

    private fun showMedicineDetails(medicine: Medicine) {
        val bottomSheet = BottomSheetDialog(requireContext(), R.style.CustomBottomSheetDialog)
        val bottomSheetView = layoutInflater.inflate(R.layout.bottom_sheet_medicine_details, null)

        bottomSheetView.apply {
            findViewById<TextView>(R.id.medicineName).text = medicine.name
            findViewById<TextView>(R.id.medicineDescription).text = medicine.description
            findViewById<TextView>(R.id.medicineDosage).text = medicine.dosage
            findViewById<TextView>(R.id.medicineSideEffects).text = medicine.sideEffects
        }

        bottomSheet.setContentView(bottomSheetView)
        bottomSheet.show()
    }

    private fun showError(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    private fun checkAndLoadMedicinesData() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                Timber.d("Starting medicine data check and load")
                withContext(Dispatchers.IO) {
                    medicineDataLoader.loadMedicinesIntoFirestore()
                }
                Timber.d("Successfully loaded medicines data")
                // After loading data, load initial medicines for display
                viewModel.loadInitialMedicines()
            } catch (e: Exception) {
                Timber.e(e, "Failed to load medicines data")
                withContext(Dispatchers.Main) {
                    showError("Failed to load medicines data: ${e.message}")
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}