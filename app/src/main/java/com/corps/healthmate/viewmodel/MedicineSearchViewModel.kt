package com.corps.healthmate.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.corps.healthmate.models.Medicine
import com.corps.healthmate.repository.MedicineRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import timber.log.Timber

@HiltViewModel
class MedicineSearchViewModel @Inject constructor(
    private val repository: MedicineRepository
) : ViewModel() {

    private val _medicines = MutableStateFlow<List<Medicine>>(emptyList())
    val medicines: StateFlow<List<Medicine>> = _medicines

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    private var currentQuery = ""
    private val currentMedicines = mutableSetOf<Medicine>()

    fun loadInitialMedicines() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                currentMedicines.clear()
                repository.getInitialMedicines().collect { medicines ->
                    currentMedicines.addAll(medicines)
                    _medicines.value = currentMedicines.toList()
                }
            } catch (e: Exception) {
                _error.value = "Failed to load medicines"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun searchMedicines(query: String, isNewSearch: Boolean = true) {
        if (isNewSearch) {
            currentMedicines.clear()
            repository.resetPagination()
        }
        currentQuery = query

        viewModelScope.launch {
            if (_isLoading.value) return@launch
            _isLoading.value = true
            
            try {
                repository.searchMedicines(query, isNewSearch).collect { newMedicines ->
                    if (isNewSearch) {
                        currentMedicines.clear()
                    }
                    
                    // Add only unique medicines based on ID
                    val uniqueMedicines = newMedicines.filterNot { newMed -> 
                        currentMedicines.any { it.id == newMed.id }
                    }
                    currentMedicines.addAll(uniqueMedicines)
                    
                    _medicines.value = currentMedicines.toList().sortedBy { it.name }
                    Timber.d("Total unique medicines loaded: ${currentMedicines.size}")
                }
            } catch (e: Exception) {
                _error.value = "Search failed"
                Timber.e(e, "Search failed")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadMore() {
        if (!_isLoading.value && currentQuery.isNotEmpty()) {
            searchMedicines(currentQuery, false)
        }
    }


} 