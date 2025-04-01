package com.corps.healthmate.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.corps.healthmate.database.Reminder
import com.corps.healthmate.repository.ReminderRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class AiAssistViewModel(application: Application) : AndroidViewModel(application) {
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error


    // Initialize repository and LiveData
    private val repository: ReminderRepository = ReminderRepository(application)



    // Expose repository's LiveData directly
    val allReminders: LiveData<List<Reminder>> = repository.allReminders

    init {

        // Initial load of reminders
        viewModelScope.launch {
            loadReminders(getCurrentUserId())
        }
    }

    private fun getCurrentUserId(): String {
        return getApplication<Application>().let {
            com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
        }
    }

    fun loadReminders(userId: String) {
        if (userId.isBlank()) {
            Timber.w("Cannot load reminders: userId is blank")
            return
        }

        viewModelScope.launch {
            try {
                _isLoading.value = true
                withContext(Dispatchers.IO) {
                    repository.refreshReminders()
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading reminders")
                _error.value = "Failed to load reminders: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                withContext(Dispatchers.IO) {
                    repository.delete(reminder)
                    Timber.d("Reminder deleted successfully")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error deleting reminder")
                _error.value = "Failed to delete reminder: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateReminder(reminder: Reminder) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                withContext(Dispatchers.IO) {
                    repository.update(reminder)
                    Timber.d("Reminder updated successfully")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error updating reminder")
                _error.value = "Failed to update reminder: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }




}