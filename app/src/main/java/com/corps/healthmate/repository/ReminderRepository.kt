package com.corps.healthmate.repository

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.corps.healthmate.database.AppDatabase
import com.corps.healthmate.database.Reminder
import com.corps.healthmate.database.ReminderDao
import com.corps.healthmate.utils.ReminderManager
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.*
import timber.log.Timber

class ReminderRepository(application: Application) {
    private val reminderDao: ReminderDao?
    private val _allReminders = MutableLiveData<List<Reminder>>(emptyList())
    val allReminders: LiveData<List<Reminder>> = _allReminders
    private val userId: String?
    private val reminderManager: ReminderManager
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        try {
            val db = AppDatabase.getDatabase(application)
            reminderDao = db.reminderDao()
            reminderManager = ReminderManager.getInstance(application)
            userId = FirebaseAuth.getInstance().currentUser?.uid
            
            // Initial load of reminders with error handling
            coroutineScope.launch {
                try {
                    refreshReminders()
                } catch (e: Exception) {
                    Timber.e(e, "Failed to load initial reminders")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error initializing ReminderRepository")
            throw e
        }
    }

    suspend fun refreshReminders() {
        try {
            withContext(Dispatchers.IO) {
                val reminders: List<Reminder> = reminderDao?.getAllReminders() ?: emptyList()
                _allReminders.postValue(reminders)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error refreshing reminders")
            _allReminders.postValue(emptyList())
        }
    }

    suspend fun insert(reminder: Reminder?) = withContext(Dispatchers.IO) {
        try {
            if (reminder != null && userId != null) {
                reminder.userId = userId
                reminderDao?.insert(reminder)
                Timber.d("Inserted reminder ${reminder.id}")
                reminderManager.scheduleReminder(reminder)
                refreshReminders()
            } else {
                Timber.w("Cannot insert reminder: reminder=$reminder, userId=$userId")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error inserting reminder")
            throw e
        }
    }

    suspend fun update(reminder: Reminder?) = withContext(Dispatchers.IO) {
        try {
            if (reminder != null) {
                reminderDao?.update(reminder)
                Timber.d("Updated reminder ${reminder.id}")
                if (reminder.isActive) {
                    reminderManager.scheduleReminder(reminder)
                } else {
                    reminderManager.cancelReminder(reminder)
                }
                refreshReminders()
            } else {
                Timber.w("Cannot update reminder: reminder is null")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error updating reminder")
            throw e
        }
    }

    suspend fun delete(reminder: Reminder?) = withContext(Dispatchers.IO) {
        try {
            if (reminder != null) {
                reminderDao?.delete(reminder)
                Timber.d("Deleted reminder ${reminder.id}")
                reminderManager.cancelReminder(reminder)
                refreshReminders()
            } else {
                Timber.w("Cannot delete reminder: reminder is null")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error deleting reminder")
            throw e
        }
    }

    suspend fun deactivateReminder(reminderId: Int) = withContext(Dispatchers.IO) {
        try {
            reminderDao?.deactivateReminder(reminderId)
            Timber.d("Deactivated reminder $reminderId")
            reminderDao?.getReminderById(reminderId)?.let { reminder ->
                reminderManager.cancelReminder(reminder)
            }
            refreshReminders()
        } catch (e: Exception) {
            Timber.e(e, "Error deactivating reminder")
            throw e
        }
    }

    suspend fun deleteAllRemindersForUser() = withContext(Dispatchers.IO) {
        try {
            if (userId != null) {
                val currentReminders = reminderDao?.getAllRemindersForUserSync(userId) ?: emptyList()
                currentReminders.forEach { reminder ->
                    reminderManager.cancelReminder(reminder)
                }
                reminderDao?.deleteAllRemindersForUser(userId)
                Timber.d("Deleted all reminders for user $userId")
                refreshReminders()
            } else {
                Timber.w("Cannot delete all reminders: userId is null")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error deleting all reminders")
            throw e
        }
    }

    companion object {
        private const val TAG = "ReminderRepository"
    }
}
